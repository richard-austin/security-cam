package com.securitycam.recording

import com.securitycam.configuration.Config
import com.securitycam.controllers.Camera
import com.securitycam.controllers.Stream
import com.securitycam.mediaserverutils.AVData
import com.securitycam.services.LogService
import org.bytedeco.ffmpeg.avcodec.AVCodec
import org.bytedeco.ffmpeg.avcodec.AVCodecContext
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avformat.AVFormatContext
import org.bytedeco.ffmpeg.avformat.AVStream
import org.bytedeco.ffmpeg.avutil.AVChannelLayout
import org.bytedeco.ffmpeg.avutil.AVFrame
import org.bytedeco.ffmpeg.avutil.AVRational
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.javacv.FFmpegFrameRecorder

import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import static com.securitycam.recording.deepCopy.deepCopyPacket
import static com.securitycam.recording.deepCopy.freePacket
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_AAC
import static org.bytedeco.ffmpeg.global.avcodec.AV_PKT_FLAG_KEY
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_alloc_context3
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_find_decoder
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_free_context
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_open2
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_copy
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_to_context
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_receive_frame
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_send_packet
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_context
import static org.bytedeco.ffmpeg.global.avformat.avformat_free_context
import static org.bytedeco.ffmpeg.global.avformat.avformat_new_stream
import static org.bytedeco.ffmpeg.global.avutil.AV_NOPTS_VALUE
import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_FLT
import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_FLTP
import static org.bytedeco.ffmpeg.global.avutil.av_channel_layout_copy
import static org.bytedeco.ffmpeg.global.avutil.av_channel_layout_default
import static org.bytedeco.ffmpeg.global.avutil.av_frame_alloc
import static org.bytedeco.ffmpeg.global.avutil.av_frame_free
import static org.bytedeco.ffmpeg.global.avutil.av_frame_unref
import static org.bytedeco.ffmpeg.global.avutil.av_make_q
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q

class Recorder {
    LogService logService
    BucketBrigade brigade = null
    PacketQueue queue
    ExecutorService recordingThread
    AVData avData
    AVFormatContext formatContext
    FFmpegFrameRecorder recorderHLS
    Camera camera
    Stream stream
    volatile boolean running
    Config config

    private AVFormatContext videoOnlyCtx = null
    // Lightweight decoder used only when transcoding (e.g. pcm_alaw -> AAC)
    private AVCodecContext audioDecoderCtx = null
    private AVFrame decodedAudioFrame = null

    Recorder(Camera cam, Stream stream, AVData avData, Config config, LogService logService) {
        this.avData = avData
        this.queue = null
        this.formatContext = avData.inputFormatContext
        this.config = config
        this.logService = logService

        final int preambleTime = stream.preambleTime
        this.brigade = new BucketBrigade(2500, preambleTime, formatContext, logService)
        this.recordingThread = null
        this.camera = cam
        this.stream = stream
        this.running = false
    }

    void write(AVPacket packet, String cameraName) {
        AVPacket clonedPacket = deepCopyPacket(packet)
        if (!clonedPacket.isNull()) {
            AVPacket delayedPacket = brigade.writeAndGetDelayed(clonedPacket, cameraName)
            if (delayedPacket != null && brigade.phase == BucketBrigade.brigadePhase.eRunning && queue != null) {
                queue.putPacket(delayedPacket)
            }
        } else {
            logService.cam.error("Cloned packet isNull in Recorder.write")
        }
    }

    void start() {
        if (!running) {
            try {
                running = true
                long epochTime = Instant.now().epochSecond
                final String location = stream?.recording?.location
                final boolean hasAudio = stream.audio && avData.audioStreamIndex != -1
                final boolean needsTranscode = hasAudio && avData.inputAudioCodecName?.toLowerCase() != "aac"
                final int outSampleRate = avData.sampleRate > 0 ? avData.sampleRate : 8000
                final int channels = hasAudio ? (needsTranscode ? 1 : avData.audioChannels) : 0

                recorderHLS = new FFmpegFrameRecorder(
                        "${config.camerasHomeDirectory}/${location}/${location}-${epochTime}_.m3u8",
                        avData.width,
                        avData.height,
                        channels
                )
                recorderHLS.setFormat("hls")
                recorderHLS.setVideoCodec(avData.inputVideoCodecId)
                recorderHLS.setVideoCodecName("copy")
                if (avData.frameRate > 0) {
                    recorderHLS.setFrameRate(avData.frameRate)
                }

                // HLS fMP4 muxer options
                recorderHLS.setOption("level", "3.0")
                recorderHLS.setOption("start_number", "0")
                recorderHLS.setOption("hls_time", "3")
                recorderHLS.setOption("hls_list_size", "0")
                recorderHLS.setOption("hls_segment_type", "fmp4")
                recorderHLS.setOption("hls_flags", "independent_segments")
                final String fmp4InitFileName = "${location}-${epochTime}_.mp4"
                recorderHLS.setOption("hls_fmp4_init_filename", fmp4InitFileName)
                recorderHLS.setOption("flush_packets", "1")
                recorderHLS.setOption("fflags", "nobuffer")
                // Prevent libavformat interleaver from shifting/delaying track start times
                recorderHLS.setOption("max_interleave_delta", "0")
                recorderHLS.setOption("avoid_negative_ts", "make_zero")

                if (hasAudio) {
                    recorderHLS.setSampleRate(outSampleRate)
                    recorderHLS.setAudioChannels(channels)
                    if (needsTranscode) {
                        // Let FFmpegFrameRecorder natively encode PCM samples -> AAC-LC
                        recorderHLS.setAudioCodec(AV_CODEC_ID_AAC)
                        recorderHLS.setAudioBitrate(64000)
                        recorderHLS.setAudioOption("profile", "aac_low")
                        initAudioDecoder(
                                avData.inputFormatContext.streams(avData.audioStreamIndex).codecpar(),
                                outSampleRate,
                                channels
                        )
                    } else {
                        // Camera already produces AAC: stream-copy directly
                        recorderHLS.setAudioCodec(avData.inputAudioCodecId)
                        recorderHLS.setAudioCodecName("copy")
                    }
                }

                // Start recorder:
                // - Pass inputFormatContext when stream-copying all active tracks
                // - Pass videoOnlyCtx when FFmpegFrameRecorder is encoding audio natively
                if (needsTranscode) {
                    videoOnlyCtx = avformat_alloc_context()
                    AVStream inVideoStream = avData.inputFormatContext.streams(avData.videoStreamIndex)
                    AVStream outVideoStream = avformat_new_stream(videoOnlyCtx, null)
                    avcodec_parameters_copy(outVideoStream.codecpar(), inVideoStream.codecpar())
                    outVideoStream.time_base(inVideoStream.time_base())
                    outVideoStream.r_frame_rate(inVideoStream.r_frame_rate())
                    outVideoStream.avg_frame_rate(inVideoStream.avg_frame_rate())
                    recorderHLS.start(videoOnlyCtx)
                } else {
                    recorderHLS.start(avData.inputFormatContext)
                }

                final AVStream inVideo = avData.inputFormatContext.streams(avData.videoStreamIndex)
                final AVStream inAudio = hasAudio ? avData.inputFormatContext.streams(avData.audioStreamIndex) : null
                final AVRational inVideoTb = inVideo.time_base()
                final AVRational inAudioTb = inAudio?.time_base()
                final AVRational usTb = av_make_q(1, 1_000_000)

                long startTimeUs = AV_NOPTS_VALUE
                long firstVideoTs = AV_NOPTS_VALUE
                long firstAudioTs = AV_NOPTS_VALUE
                long lastVideoDts = AV_NOPTS_VALUE
                long lastAudioDts = AV_NOPTS_VALUE

                queue = new PacketQueue(brigade.getGopCache())
                recordingThread = Executors.newSingleThreadExecutor()
                recordingThread.submit(() -> {
                    try {
                        while (running) {
                            try {
                                AVPacket pkt = queue.getNextPacket()
                                if (pkt == null || pkt.isNull()) continue
                                int idx = pkt.stream_index()

                                if (idx == avData.videoStreamIndex) {
                                    long refTs = (pkt.dts() != AV_NOPTS_VALUE) ? pkt.dts() : pkt.pts()

                                    // Wait until the first video keyframe so fMP4 starts cleanly on an IDR frame
                                    if (firstVideoTs == AV_NOPTS_VALUE) {
                                        if ((pkt.flags() & AV_PKT_FLAG_KEY) == 0 || refTs == AV_NOPTS_VALUE) {
                                            freePacket(pkt)
                                            continue
                                        }
                                        firstVideoTs = refTs
                                        startTimeUs = av_rescale_q(refTs, inVideoTb, usTb)
                                    }
                                    if (refTs == AV_NOPTS_VALUE) {
                                        freePacket(pkt)
                                        continue
                                    }
                                    long newDts = refTs - firstVideoTs
                                    long newPts = (pkt.pts() != AV_NOPTS_VALUE) ? (pkt.pts() - firstVideoTs) : newDts
                                    if (newDts < 0) newDts = 0
                                    if (lastVideoDts != AV_NOPTS_VALUE && newDts <= lastVideoDts) {
                                        newDts = lastVideoDts + 1
                                    }
                                    if (newPts < newDts) {
                                        newPts = newDts
                                    }
                                    lastVideoDts = newDts
                                    pkt.dts(newDts)
                                    pkt.pts(newPts)
                                    recorderHLS.recordPacket(pkt)
                                    freePacket(pkt)
                                } else if (hasAudio && idx == avData.audioStreamIndex) {
// Drop audio packets prior to the first video keyframe so audio never precedes video
                                    if (firstVideoTs == AV_NOPTS_VALUE) {
                                        freePacket(pkt)
                                        continue
                                    }
                                    if (needsTranscode) {
                                        decodeAndRecordAudio(pkt, inAudioTb, usTb, startTimeUs, outSampleRate, channels)
                                    } else {
                                        long refTs = (pkt.dts() != AV_NOPTS_VALUE) ? pkt.dts() : pkt.pts()
                                        if (refTs == AV_NOPTS_VALUE) {
                                            freePacket(pkt)
                                            continue
                                        }
// Drop stale pre-keyframe audio packets
                                        long pktUs = av_rescale_q(refTs, inAudioTb, usTb)
                                        if (pktUs < startTimeUs) {
                                            freePacket(pkt)
                                            continue
                                        }
// Anchor the first accepted audio packet directly to 0 so Firefox MSE doesn't stall
                                        if (firstAudioTs == AV_NOPTS_VALUE) {
                                            firstAudioTs = refTs
                                        }
                                        long newDts = refTs - firstAudioTs
                                        long newPts = (pkt.pts() != AV_NOPTS_VALUE) ? (pkt.pts() - firstAudioTs) : newDts
                                        if (newDts < 0) newDts = 0
                                        if (lastAudioDts != AV_NOPTS_VALUE && newDts <= lastAudioDts) {
                                            newDts = lastAudioDts + 1
                                        }
                                        if (newPts < newDts) {
                                            newPts = newDts
                                        }
                                        lastAudioDts = newDts
                                        pkt.dts(newDts)
                                        pkt.pts(newPts)
                                        recorderHLS.recordPacket(pkt)
                                    }
                                    freePacket(pkt)
                                } else {
                                    freePacket(pkt)
                                }
                            } catch (Exception ex) {
                                logService.cam.error("${ex.getClass().getName()} in recording packet handler loop: ${ex.getMessage()}")
                            }
                        }
                    } catch (Exception ex) {
                        logService.cam.error("Error in recording loop", ex)
                    } finally {
                        queue?.stop()
                        try {
                            recorderHLS?.stop()
                        } catch (Exception ignored) {
                        }
                        try {
                            recorderHLS?.close()
                        } catch (Exception ignored) {
                        }
                        try {
                            recorderHLS?.release()
                        } catch (Exception ignored) {
                        }
                        closeAudioDecoder()
                        if (videoOnlyCtx != null) {
                            avformat_free_context(videoOnlyCtx)
                            videoOnlyCtx = null
                        }
                    }
                })
            } catch (Exception ex) {
                logService.cam.error("${ex.getClass().getName()} in Recorder start: ${ex.getMessage()}")
                stop()
            }
        }
    }

    private void initAudioDecoder(AVCodecParameters inCodecPar, int defaultSampleRate, int channels) {
        AVCodec decoder = avcodec_find_decoder(inCodecPar.codec_id())
        if (decoder == null) {
            throw new IllegalStateException("Input audio codec decoder not found for codec_id=${inCodecPar.codec_id()}")
        }
        audioDecoderCtx = avcodec_alloc_context3(decoder)
        if (avcodec_parameters_to_context(audioDecoderCtx, inCodecPar) < 0) {
            throw new IllegalStateException("Could not copy input audio codec parameters")
        }
        if (audioDecoderCtx.sample_rate() <= 0) {
            audioDecoderCtx.sample_rate(defaultSampleRate)
        }
        if (audioDecoderCtx.ch_layout().nb_channels() <= 0) {
            AVChannelLayout chLayout = new AVChannelLayout()
            av_channel_layout_default(chLayout, channels)
            av_channel_layout_copy(audioDecoderCtx.ch_layout(), chLayout)
            chLayout.close()
        }
        if (avcodec_open2(audioDecoderCtx, decoder, (PointerPointer) null) < 0) {
            throw new IllegalStateException("Could not open input audio decoder")
        }
        decodedAudioFrame = av_frame_alloc()
    }

    private void decodeAndRecordAudio(
            AVPacket pkt,
            AVRational inAudioTb,
            AVRational usTb,
            long startTimeUs,
            int sampleRate,
            int channels
    ) {
        if (audioDecoderCtx == null || decodedAudioFrame == null) return
        if (avcodec_send_packet(audioDecoderCtx, pkt) >= 0) {
            while (avcodec_receive_frame(audioDecoderCtx, decodedAudioFrame) >= 0) {
                long framePts = decodedAudioFrame.pts() != AV_NOPTS_VALUE
                        ? decodedAudioFrame.pts()
                        : decodedAudioFrame.best_effort_timestamp()
                if (framePts != AV_NOPTS_VALUE && startTimeUs != AV_NOPTS_VALUE) {
                    long ptsUs = av_rescale_q(framePts, inAudioTb, usTb) - startTimeUs
                    if (ptsUs >= 0) {
                        recorderHLS.setTimestamp(ptsUs)
                        int totalSamples = decodedAudioFrame.nb_samples() * channels
                        int fmt = decodedAudioFrame.format()
                        if (fmt == AV_SAMPLE_FMT_FLTP || fmt == AV_SAMPLE_FMT_FLT) {
                            FloatBuffer floatSamples = decodedAudioFrame.data(0)
                                    .capacity((long) totalSamples * 4L)
                                    .asByteBuffer()
                                    .order(ByteOrder.nativeOrder())
                                    .asFloatBuffer()
                            recorderHLS.recordSamples(sampleRate, channels, floatSamples)
                        } else {
                            ShortBuffer pcmSamples = decodedAudioFrame.data(0)
                                    .capacity((long) totalSamples * 2L)
                                    .asByteBuffer()
                                    .order(ByteOrder.nativeOrder())
                                    .asShortBuffer()
                            recorderHLS.recordSamples(sampleRate, channels, pcmSamples)
                        }
                    }
                }
                av_frame_unref(decodedAudioFrame)
            }
        }
    }

    private void closeAudioDecoder() {
        if (decodedAudioFrame != null) {
            av_frame_free(decodedAudioFrame)
            decodedAudioFrame = null
        }
        if (audioDecoderCtx != null) {
            avcodec_free_context(audioDecoderCtx)
            audioDecoderCtx = null
        }
    }

    void stop() {
        if (running) {
            running = false
            try {
                recordingThread?.awaitTermination(5, TimeUnit.SECONDS)
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt()
            }
        }
        recordingThread = null
        queue = null
// Delete recordings older than 21 days
        final String location = stream?.recording?.location
        if (location) {
            Process p = Runtime.getRuntime().exec(["find", "${config.camerasHomeDirectory}/${location}", "-mtime", "+21", "-type", "f", "-delete"] as String[])
            p.waitFor()
        }
    }

    Timer timer

    void startFromFtp() {
        def recorder = this
        TimerTask task = new TimerTask() {
            @Override
            void run() {
                recorder.stop()
                timer.purge()
                timer = null
            }
        }
        if (timer != null) {
            timer.cancel()
        }
        timer = new Timer("ftp triggered timer")
        final minRecordingTime = 30  // Seconds
        timer.schedule(task, minRecordingTime * 1000)
        this.start()
    }
}
