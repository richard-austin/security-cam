package com.securitycam.recording

import com.securitycam.controllers.Camera
import com.securitycam.controllers.Stream
import com.securitycam.mediaserverutils.AVData
import com.securitycam.services.LogService
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avformat.AVFormatContext
import org.bytedeco.ffmpeg.avformat.AVStream
import org.bytedeco.ffmpeg.avutil.AVRational
import org.bytedeco.javacv.FFmpegFrameRecorder

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_copy
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_AAC
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_PCM_ALAW
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_from_context
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_context
import static org.bytedeco.ffmpeg.global.avformat.avformat_free_context
import static org.bytedeco.ffmpeg.global.avformat.avformat_new_stream
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import static com.securitycam.recording.deepCopy.deepCopyPacket
import static com.securitycam.recording.deepCopy.freePacket
import static org.bytedeco.ffmpeg.global.avutil.av_make_q


class Recorder {
    LogService logService
    BucketBrigade brigade
    PacketQueue queue
    ExecutorService recordingThread
    AVData avData
    AVFormatContext formatContext
    FFmpegFrameRecorder recorderHLS
    Camera camera
    Stream stream
    boolean running
    AVFormatContext filteredCtx
    AudioTranscoder audioTranscoder = null

    Recorder(Camera cam, Stream stream, AVData avData, LogService logService) {
        this.avData = avData
        queue = null
        this.formatContext = avData.inputFormatContext
        this.logService = logService
        final int preambleTime = stream.preambleTime
        brigade = new BucketBrigade(2500, preambleTime, formatContext, logService)
        recordingThread = null
        camera = cam
        this.stream = stream
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
        if (!running) {
            try {
                running = true
                long epochTime = Instant.now().epochSecond
                final String location = stream?.recording?.location
                final boolean hasAudio = stream.audio && avData.audioStreamIndex != -1
                final boolean needsTranscode = hasAudio && avData.inputAudioCodecName?.toLowerCase() != "aac"
                final int outSampleRate = needsTranscode ? 44100 : avData.sampleRate
                int channels = hasAudio ? (needsTranscode ? 1 : avData.audioChannels) : 0

                recorderHLS = new FFmpegFrameRecorder(
                        "/home/richard/${location}/${location}-${epochTime}_.m3u8",
                        avData.width,
                        avData.height,
                        channels
                )
                recorderHLS.setFormat("hls")
                recorderHLS.setVideoCodec(avData.inputVideoCodecId)
                recorderHLS.setVideoCodecName("copy")

                if (hasAudio) {
                    recorderHLS.setSampleRate(outSampleRate)
                    recorderHLS.setAudioCodec(needsTranscode ? AV_CODEC_ID_AAC : avData.inputAudioCodecId)
                    recorderHLS.setAudioCodecName("copy")
                }

                recorderHLS.setOption("level", "3.0")
                recorderHLS.setOption("start_number", "0")
                recorderHLS.setOption("hls_time", "3")
                recorderHLS.setOption("hls_list_size", "0")
                recorderHLS.setOption("hls_segment_type", "fmp4")

                final fmp4InitFileName = "${stream?.recording?.location}-${epochTime}_.mp4"
                recorderHLS.setOption("hls_fmp4_init_filename", "${fmp4InitFileName}")
                recorderHLS.setOption("flush_packets", "1")
                recorderHLS.setOption("fflags", "nobuffer")

                AVFormatContext contextToUse = avData.inputFormatContext
                AVStream inAudio = hasAudio ? avData.inputFormatContext.streams(avData.audioStreamIndex) : null
                AVStream outAudio = null

                if (!hasAudio || needsTranscode) {
                    filteredCtx = avformat_alloc_context()

                    // Stream 0: Video
                    AVStream inVideo = avData.inputFormatContext.streams(avData.videoStreamIndex)
                    AVStream outVideo = avformat_new_stream(filteredCtx, null)
                    avcodec_parameters_copy(outVideo.codecpar(), inVideo.codecpar())
                    outVideo.time_base(inVideo.time_base())
                    outVideo.r_frame_rate(inVideo.r_frame_rate())
                    outVideo.avg_frame_rate(inVideo.avg_frame_rate())

                    // Stream 1: Transcoded AAC Audio
                    if (needsTranscode) {
                        int inCodecId = avData.inputAudioCodecId // AV_CODEC_ID_PCM_ALAW or AV_CODEC_ID_PCM_MULAW
                        int inSampleRate = avData.sampleRate ?: 8000
                        audioTranscoder = new AudioTranscoder(inCodecId, inSampleRate, outSampleRate, channels, 64000)

                        // Add Stream 1 to filteredCtx and copy AAC encoder parameters (including extradata!)
                        outAudio = avformat_new_stream(filteredCtx, null)
                        avcodec_parameters_from_context(outAudio.codecpar(), audioTranscoder.getEncoderContext())
                        outAudio.time_base(av_make_q(1, outSampleRate))
                    }

                    contextToUse = filteredCtx
                }

                recorderHLS.start(contextToUse)

                // Capture the actual output stream time_base assigned by the fMP4 muxer after start()
                final AVRational inAudioTb = inAudio?.time_base()
                final AVRational outAudioTb = needsTranscode ? outAudio.time_base() : null

                queue = new PacketQueue(brigade.getGopCache())
                recordingThread = Executors.newSingleThreadExecutor()
                recordingThread.submit(() -> {
                    for (; ;) {
                        try {
                            AVPacket pkt = queue.getNextPacket()
                            int idx = pkt.stream_index()

                            if (idx == avData.videoStreamIndex) {
                                if (filteredCtx != null) {
                                    pkt.stream_index(0)
                                }
                                recorderHLS.recordPacket(pkt)
                            } else if (hasAudio && idx == avData.audioStreamIndex) {
                                if (needsTranscode) {
                                    List<AVPacket> aacPackets = audioTranscoder.transcodePacket(pkt, inAudioTb, outAudioTb)
                                    aacPackets.each { AVPacket aacPkt ->
                                        aacPkt.stream_index(1)
                                        recorderHLS.recordPacket(aacPkt)
                                        av_packet_free(aacPkt)
                                    }
                                } else {
                                    if (filteredCtx != null) {
                                        pkt.stream_index(1)
                                    }
                                    recorderHLS.recordPacket(pkt)
                                }
                            }
                            freePacket(pkt)
                        }
                        catch (Exception ex) {
                            logService.cam.error(ex.getClass().getName() + " in recording packet handler: " + ex.getMessage())
                        }
                    }
                })
            }
            catch (Exception ex) {
                logService.cam.error(ex.getClass().getName() + " in recording packet handler: " + ex.getMessage())
                stop()
            }
        }
    }

    void stop() {
        if (running) {
            running = false
            if (audioTranscoder != null) {
                audioTranscoder?.close()
                audioTranscoder = null
            }

            if (filteredCtx != null) {
                avformat_free_context(filteredCtx)
                filteredCtx = null
            }
            queue.stop()
            recordingThread.shutdown()
            recordingThread = null
            queue = null
            recorderHLS.close()
            recorderHLS.stop()
            recorderHLS.release()
        }
    }
}
