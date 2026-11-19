package com.securitycam.recording

import com.securitycam.services.LogService
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avformat.AVFormatContext
import org.bytedeco.ffmpeg.avformat.AVStream
import java.util.concurrent.LinkedBlockingQueue
import org.bytedeco.ffmpeg.global.avutil

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

    AVPacket write(AVPacket pkt, String cameraName) {
        AVPacket delayedPacket = null
        switch (phase) {
            case brigadePhase.eGetSettleStartTime:
                startTime = System.currentTimeSeconds()
                phase = brigadePhase.eInitialSettleTime
                break
            case brigadePhase.eInitialSettleTime:
                if (System.currentTimeSeconds() - startTime >= initialSettleTime) {
                    phase = brigadePhase.eCalculateBrigadeSize
                    startTime = System.currentTimeSeconds()
                }
                break
            case brigadePhase.eCalculateBrigadeSize:
                if (System.currentTimeSeconds() - startTime > preambleTime) {
                    phase = brigadePhase.eRunning
                    indexLimit = inputIndex + 1
                    outputIndex = (inputIndex + 1) % indexLimit
                    logService.cam.info("Bucket brigade " + cameraName + " " + length + " = " + indexLimit)
                } else {
                    brigade[inputIndex] = pkt
                    ++inputIndex
                }
                break
            case brigadePhase.eRunning:
                synchronized (brigade) {
                    brigade[inputIndex] = pkt
                    delayedPacket = brigade[outputIndex]
                    inputIndex = ++inputIndex % indexLimit
                    outputIndex = (inputIndex + 1) % indexLimit

                    final AVPacket outputPacket = delayedPacket
                    if(outputPacket == null || outputPacket.isNull()) {
                        logService.cam.error("Output packet was null in the bucket brigade")
                        break
                    } else if(formatContext == null || formatContext.isNull()) {
                        logService.cam.error("formatContext was null in the bucket brigade")
                        break
                    }
                    AVStream avStream = formatContext.streams(outputPacket.stream_index())
                    if(avStream == null)
                        logService.cam.error("avStream is null")

                    int codecType = avStream?.codecpar()?.codec_type()

                    final boolean keyFrame = (codecType == avutil.AVMEDIA_TYPE_VIDEO && (outputPacket.flags() & AV_PKT_FLAG_KEY) != 0)
                    if (keyFrame) {
                        gopCache.writeKeyFrame(outputPacket)
                    } else
                        gopCache.write(outputPacket)
                }
                break
        }
        return delayedPacket
    }

}
