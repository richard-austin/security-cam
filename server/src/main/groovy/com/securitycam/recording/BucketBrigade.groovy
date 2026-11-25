package com.securitycam.recording

import com.securitycam.services.LogService
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avformat.AVFormatContext
import org.bytedeco.ffmpeg.avformat.AVStream
import org.bytedeco.ffmpeg.global.avutil

import static com.securitycam.recording.deepCopy.deepCopyPacket
import static com.securitycam.recording.deepCopy.freePacket
import static org.bytedeco.ffmpeg.global.avcodec.AV_PKT_FLAG_KEY

class BucketBrigade {
    enum brigadePhase {
        eGetSettleStartTime, eInitialSettleTime, eCalculateBrigadeSize, eRunning
    }

    LogService logService
    final AVPacket[] brigade
    int inputIndex
    int outputIndex
    int indexLimit
    final int length
    long startTime
    long preambleTime
    final AVFormatContext formatContext
    GOPCache gopCache
    final long initialSettleTime
    brigadePhase phase

    BucketBrigade(int length, long preambleTime, AVFormatContext formatContext, LogService logService) {
        initialSettleTime = 7
        phase = brigadePhase.eGetSettleStartTime
        this.logService = logService
        this.length = length
        brigade = new AVPacket[length]
        inputIndex = outputIndex = 0
        startTime = 0
        this.preambleTime = preambleTime
        this.formatContext = formatContext
        indexLimit = length
        gopCache = new GOPCache(800, formatContext, logService)
    }

    AVPacket writeAndGetDelayed(AVPacket pkt, String cameraName) {
        if (pkt == null || pkt.isNull()) return null
        AVPacket delayedPacket = null

        synchronized (brigade) {
            switch (phase) {
                case brigadePhase.eGetSettleStartTime:
                    startTime = System.currentTimeSeconds()
                    phase = brigadePhase.eInitialSettleTime
                    freePacket(pkt)
                    break

                case brigadePhase.eInitialSettleTime:
                    if (System.currentTimeSeconds() - startTime >= initialSettleTime) {
                        phase = brigadePhase.eCalculateBrigadeSize
                        startTime = System.currentTimeSeconds()
                    }
                    freePacket(pkt)
                    break

                case brigadePhase.eCalculateBrigadeSize:
                    if (System.currentTimeSeconds() - startTime > preambleTime || inputIndex >= length - 1) {
                        phase = brigadePhase.eRunning
                        indexLimit = Math.max(1, inputIndex)
                        inputIndex = 0
                        outputIndex = 0
                        logService.cam.info("Bucket brigade " + cameraName + " " + length + " = " + indexLimit)
                        freePacket(pkt)
                    } else {
                        brigade[inputIndex] = pkt
                        ++inputIndex
                    }
                    break

                case brigadePhase.eRunning:
                    delayedPacket = brigade[outputIndex]
                    brigade[inputIndex] = pkt
                    inputIndex = (inputIndex + 1) % indexLimit
                    outputIndex = (inputIndex + 1) % indexLimit

                    if (delayedPacket != null && !delayedPacket.isNull()) {
                        final AVPacket outputPacket = deepCopyPacket(delayedPacket)
                        if (outputPacket == null || outputPacket.isNull()) {
                            logService.cam.error("Output packet was null in the bucket brigade for $cameraName")
                            break
                        }
                        if (formatContext == null || formatContext.isNull()) {
                            freePacket(outputPacket)
                            throw new Exception("formatContext was null in the bucket brigade for $cameraName")
                        }
                        AVStream avStream = formatContext.streams(outputPacket.stream_index())
                        if (avStream == null) {
                            logService.cam.error("avStream is null")
                        }
                        int codecType = avStream?.codecpar()?.codec_type()
                        final boolean keyFrame = (codecType == avutil.AVMEDIA_TYPE_VIDEO &&
                                (outputPacket.flags() & AV_PKT_FLAG_KEY) != 0)
                        if (keyFrame) {
                            gopCache.writeKeyFrame(outputPacket)
                        } else {
                            gopCache.write(outputPacket)
                        }
                    }
                    break
            }
        }
        return delayedPacket
    }

    void cleanup() {
        synchronized (brigade) {
            for (int i = 0; i < brigade.length; i++) {
                if (brigade[i] != null) {
                    freePacket(brigade[i])
                    brigade[i] = null
                }
            }
        }
        gopCache?.cleanup()
    }
}
