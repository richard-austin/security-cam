package com.securitycam.recording

import com.securitycam.controllers.Camera
import com.securitycam.controllers.Stream
import com.securitycam.mediaserverutils.AVData
import com.securitycam.services.LogService
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avformat.AVFormatContext
import org.bytedeco.javacv.FFmpegFrameRecorder

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import static com.securitycam.recording.deepCopy.deepCopyPacket
import static com.securitycam.recording.deepCopy.freePacket

class Recorder {
    LogService logService
    BucketBrigade brigade
    PacketQueue queue
    ExecutorService recordingThread
    AVData avData
    AVFormatContext formatContext
    FFmpegFrameRecorder recorderFLV
    Camera camera
    boolean running

    Recorder(Camera cam, Stream stream, AVData avData, LogService logService) {
        this.avData = avData
        queue = null
        this.formatContext = avData.inputFormatContext
        this.logService = logService
        final int preambleTime = stream.preambleTime
        brigade = new BucketBrigade(1500, preambleTime, formatContext, logService)
        recordingThread = null
        camera = cam
        running = false
    }

    void write(AVPacket packet, String cameraName) {
        AVPacket clonedPacket = deepCopyPacket(packet)
        if (!clonedPacket.isNull()) {
            AVPacket delayedPacket = brigade.write(clonedPacket, cameraName)
            if (delayedPacket != null && brigade.phase == BucketBrigade.brigadePhase.eRunning && queue != null)
                queue.putPacket(delayedPacket)
        } else
            logService.cam.error("Cloned packet isNull in Recorder.write")
    }

    void start() {
        if(!running) {
            running = true
            recorderFLV = new FFmpegFrameRecorder("/home/richard/recording.flv", avData.width, avData.height, avData.audioChannels)
            recorderFLV.setFormat("flv")
            recorderFLV.setVideoCodec(avData.inputVideoCodecId)
            recorderFLV.setSampleRate(avData.sampleRate)
            recorderFLV.setAudioCodec(avData.inputAudioCodecId)
            recorderFLV.setVideoCodecName("copy")
            recorderFLV.setAudioCodecName("copy")
            recorderFLV.setOption("flvflags", "no_sequence_end")
            recorderFLV.setOption("flush_packets", "1")
            recorderFLV.setOption("fflags", "nobuffer")


            recorderFLV.start(formatContext)
            queue = new PacketQueue(brigade.getGopCache())
            recordingThread = Executors.newSingleThreadExecutor()
            recordingThread.submit(() -> {
                for (; ;) {
                    try {
                        AVPacket pkt = queue.getNextPacket()
                        recorderFLV.recordPacket(pkt)
                        freePacket(pkt) // Not doing anything with it yet
                    }
                    catch (Exception ex) {
                        logService.cam.error(ex.getClass().getName() + " in recording packet handler: " + ex.getMessage())
                    }
                }
            })
        }
    }

    void stop() {
        if(running) {
            running = false
            queue.stop()
            recordingThread.shutdown()
            recordingThread = null
            queue = null
            recorderFLV.close()
            recorderFLV.stop()
        }
    }
}
