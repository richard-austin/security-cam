package com.securitycam.recording

import org.bytedeco.ffmpeg.avcodec.AVCodec
import org.bytedeco.ffmpeg.avcodec.AVCodecContext
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avformat.AVStream
import org.bytedeco.ffmpeg.avutil.AVAudioFifo
import org.bytedeco.ffmpeg.avutil.AVChannelLayout
import org.bytedeco.ffmpeg.avutil.AVDictionary
import org.bytedeco.ffmpeg.avutil.AVFrame
import org.bytedeco.ffmpeg.avutil.AVRational
import org.bytedeco.ffmpeg.swresample.SwrContext

import static org.bytedeco.ffmpeg.global.avcodec.*
import static org.bytedeco.ffmpeg.global.avutil.*
import static org.bytedeco.ffmpeg.global.swresample.*

class AudioTranscoder {
    private AVCodecContext decCtx
    private AVCodecContext encCtx
    private SwrContext swrCtx
    private AVAudioFifo fifo

    private AVFrame decodedFrame
    private AVFrame resampledFrame
    private AVFrame encFrame
    private AVPacket outPacket

    private AVRational inTimeBase
    private AVRational encTimeBase
    private long nextPts = 0
    private boolean ptsInitialized = false

    final int outSampleRate = 44100
    final int outChannels = 1

    AudioTranscoder(AVStream inAudioStream) {
        inTimeBase = inAudioStream.time_base()

        // 1. Setup Decoder (e.g., pcm_alaw, pcm_mulaw, g726)
        AVCodec decoder = avcodec_find_decoder(inAudioStream.codecpar().codec_id())
        if (decoder == null) {
            throw new RuntimeException("Unsupported input audio codec")
        }
        decCtx = avcodec_alloc_context3(decoder)
        avcodec_parameters_to_context(decCtx, inAudioStream.codecpar())
        if (avcodec_open2(decCtx, decoder, (AVDictionary) null) < 0) {
            throw new RuntimeException("Could not open audio decoder")
        }

        // 2. Setup AAC Encoder
        AVCodec encoder = avcodec_find_encoder(AV_CODEC_ID_AAC)
        if (encoder == null) {
            throw new RuntimeException("AAC encoder not found")
        }
        encCtx = avcodec_alloc_context3(encoder)
        AVChannelLayout outChLayout = new AVChannelLayout()
        av_channel_layout_default(outChLayout, outChannels)

        av_channel_layout_copy(encCtx.ch_layout(), outChLayout)
        encCtx.sample_rate(outSampleRate)
        encCtx.sample_fmt(AV_SAMPLE_FMT_FLTP)
        encCtx.bit_rate(64000)
        encTimeBase = av_make_q(1, outSampleRate)
        encCtx.time_base(encTimeBase)

        if (avcodec_open2(encCtx, encoder, (AVDictionary) null) < 0) {
            throw new RuntimeException("Could not open AAC encoder")
        }

        // 3. Setup Resampler (Input Format -> FLTP 44.1kHz)
        swrCtx = new SwrContext()
        int ret = swr_alloc_set_opts2(
                swrCtx,
                outChLayout,
                AV_SAMPLE_FMT_FLTP,
                outSampleRate,
                decCtx.ch_layout(),
                decCtx.sample_fmt(),
                decCtx.sample_rate(),
                0,
                null
        )
        if (ret < 0 || swr_init(swrCtx) < 0) {
            throw new RuntimeException("Could not initialize SwrContext")
        }
        av_channel_layout_uninit(outChLayout)

        // 4. Setup FIFO & Reusable Frames/Packet
        fifo = av_audio_fifo_alloc(AV_SAMPLE_FMT_FLTP, outChannels, encCtx.frame_size() * 2)
        decodedFrame = av_frame_alloc()

        resampledFrame = av_frame_alloc()
        resampledFrame.format(AV_SAMPLE_FMT_FLTP)
        resampledFrame.sample_rate(outSampleRate)
        av_channel_layout_copy(resampledFrame.ch_layout(), encCtx.ch_layout())

        encFrame = av_frame_alloc()
        encFrame.nb_samples(encCtx.frame_size())
        encFrame.format(AV_SAMPLE_FMT_FLTP)
        encFrame.sample_rate(outSampleRate)
        av_channel_layout_copy(encFrame.ch_layout(), encCtx.ch_layout())
        av_frame_get_buffer(encFrame, 0)

        outPacket = av_packet_alloc()
    }

    /** Copies AAC encoder parameters onto the output stream's AVCodecParameters */
    void copyParametersTo(AVCodecParameters destParams) {
        avcodec_parameters_from_context(destParams, encCtx)
    }

    /**
     * Takes one raw camera audio AVPacket and invokes onEncodedPacket(AVPacket)
     * for each 1024-sample AAC packet produced.
     */
    void transcode(AVPacket inPkt, int outStreamIndex, Closure onEncodedPacket) {
        if (!ptsInitialized && inPkt.pts() != AV_NOPTS_VALUE) {
            nextPts = av_rescale_q(inPkt.pts(), inTimeBase, encTimeBase)
            ptsInitialized = true
        }

        if (avcodec_send_packet(decCtx, inPkt) < 0) {
            return
        }

        while (avcodec_receive_frame(decCtx, decodedFrame) >= 0) {
            if (swr_convert_frame(swrCtx, resampledFrame, decodedFrame) >= 0) {
                int samples = resampledFrame.nb_samples()
                if (samples > 0) {
                    av_audio_fifo_realloc(fifo, av_audio_fifo_size(fifo) + samples)
                    av_audio_fifo_write(fifo, resampledFrame.data(), samples)
                }
                av_frame_unref(resampledFrame)
            }
            av_frame_unref(decodedFrame)

            // Pull exact 1024-sample frames for AAC encoding
            int frameSize = encCtx.frame_size()
            while (av_audio_fifo_size(fifo) >= frameSize) {
                av_frame_make_writable(encFrame)
                av_audio_fifo_read(fifo, encFrame.data(), frameSize)
                encFrame.pts(nextPts)
                nextPts += frameSize

                if (avcodec_send_frame(encCtx, encFrame) >= 0) {
                    while (avcodec_receive_packet(encCtx, outPacket) >= 0) {
                        outPacket.stream_index(outStreamIndex)
                        // Rescale from encoder time_base (1/44100) back to input stream time_base
                        // because recorderHLS.recordPacket() rescales from input stream time_base!
                        av_packet_rescale_ts(outPacket, encTimeBase, inTimeBase)
                        onEncodedPacket(outPacket)
                        av_packet_unref(outPacket)
                    }
                }
            }
        }
    }

    void close() {
        if (outPacket != null) av_packet_free(outPacket)
        if (encFrame != null) av_frame_free(encFrame)
        if (resampledFrame != null) av_frame_free(resampledFrame)
        if (decodedFrame != null) av_frame_free(decodedFrame)
        if (fifo != null) av_audio_fifo_free(fifo)
        if (swrCtx != null) swr_free(swrCtx)
        if (encCtx != null) avcodec_free_context(encCtx)
        if (decCtx != null) avcodec_free_context(decCtx)
    }
}
