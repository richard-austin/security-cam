package com.securitycam.recording

import com.securitycam.services.LogService
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avformat.AVFormatContext
import org.bytedeco.ffmpeg.avformat.AVStream

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_clone
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free
import static org.bytedeco.ffmpeg.global.avcodec.AV_PKT_FLAG_KEY
import org.bytedeco.ffmpeg.global.avutil

class Recorder {
    LogService logService
    BucketBrigade brigade
    PacketQueue queue
    ExecutorService recordingThread

    AVFormatContext formatContext

    Recorder(int preambleTime, AVFormatContext formatContext, LogService logService) {
        queue = null
        this.formatContext = formatContext
        this.logService = logService
        brigade = new BucketBrigade(1500, preambleTime, formatContext, logService)
        recordingThread = null
    }

    void write(AVPacket packet, String streamId) {
        AVPacket clonedPacket = av_packet_clone(packet)
        AVPacket delayedPacket = brigade.write(clonedPacket, streamId)
        if (delayedPacket != null &&  brigade.phase == BucketBrigade.brigadePhase.eRunning && queue != null)
            queue.putPacket(delayedPacket)
    }

    void start() {
        queue = new PacketQueue(brigade.getGopCache())
        recordingThread = Executors.newSingleThreadExecutor()
        recordingThread.submit(() -> {
            for (; ;) {
                try {
                    AVPacket pkt = queue.getNextPacket()
                    if (pkt != null || pkt.size() != 0) {

                        av_packet_free(pkt) // Not doing anything with it yet
                    } else {
                        logService.cam.error("Null or empty packet in Recorder.start")
                    }
                }
                catch (Exception ex) {
                    logService.cam.error(ex.getClass().getName() + " in recording packet handler: " + ex.getMessage())
                }
            }
        })
    }

    void stop() {
        recordingThread.shutdown()
        recordingThread.close()
        recordingThread = null
        queue.stop()
        queue = null
    }
}
