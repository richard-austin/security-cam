package com.securitycam.recording

import com.securitycam.services.LogService
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.javacpp.BytePointer

import static org.bytedeco.ffmpeg.global.avcodec.AV_PKT_FLAG_KEY


class BucketBrigade {
    enum brigadePhase {
        eGetSettleStartTime, eInitialSettleTime, eCalculateBrigadeSize, eRunning
    }
    LogService logService
    AVPacket[] brigade
    int inputIndex
    int outputIndex
    int indexLimit
    final int length
    long startTime
    long preambleTime
    GOPCache gopCache
    final long initialSettleTime
    brigadePhase phase

    BucketBrigade(int length, long preambleTime, LogService logService) {
        initialSettleTime = 7
        phase = brigadePhase.eGetSettleStartTime
        this.logService = logService
        this.length = length
        brigade = new AVPacket[length]
        inputIndex = outputIndex = 0
        startTime = 0
        this.preambleTime = preambleTime
        indexLimit = length
        gopCache = new GOPCache(300, logService)
    }

    void write(AVPacket pkt, String stream) {

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
                    logService.cam.info("Bucket brigade " + stream + " " + length + " = " + indexLimit)
                } else {
                    brigade[inputIndex] = pkt
                    ++inputIndex
                }
                break
            case brigadePhase.eRunning:
                brigade[inputIndex] = pkt
                inputIndex = ++inputIndex % indexLimit
                outputIndex = (inputIndex + 1) % indexLimit

                final AVPacket outputPacket = brigade[outputIndex]
                final boolean keyFrame = (outputPacket.flags() & AV_PKT_FLAG_KEY) != 0
                if (keyFrame) {
                    gopCache.writeKeyFrame(outputPacket)
                }
                else
                    gopCache.write(outputPacket)

                BytePointer data = outputPacket.data()
                if (data != null && outputPacket.size() > 0) {
                    byte[] buffer = new byte[outputPacket.size()]
                    data.get(buffer)
                    //  logService.cam.info("Bucket brigade output len "+buffer.length)
                }
                break
        }
    }
}
