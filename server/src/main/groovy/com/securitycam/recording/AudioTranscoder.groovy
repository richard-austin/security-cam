package com.securitycam.recording

import org.bytedeco.ffmpeg.avcodec.AVCodec
import org.bytedeco.ffmpeg.avcodec.AVCodecContext
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avutil.AVChannelLayout
import org.bytedeco.ffmpeg.avutil.AVFrame
import org.bytedeco.ffmpeg.avutil.AVRational
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.ffmpeg.global.swresample
import org.bytedeco.ffmpeg.swresample.SwrContext
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.PointerPointer

class AudioTranscoder implements AutoCloseable {

    private AVCodecContext decoderCtx
    private AVCodecContext encoderCtx
    private SwrContext swrCtx
    private AVFrame decodedFrame
    private AVFrame resampledFrame
    private long nextPts = 0
    private boolean ptsInitialized = false

    AudioTranscoder(AVCodecParameters inCodecPar, int outSampleRate = 16000, int channels = 1, int aacBitrate = 64000) {
        AVChannelLayout channelLayout = new AVChannelLayout()
        avutil.av_channel_layout_default(channelLayout, channels)

        // 1. Setup Decoder using the stream's actual codec parameters
        AVCodec decoder = avcodec.avcodec_find_decoder(inCodecPar.codec_id())
        if (decoder == null) throw new IllegalStateException("Input codec not found")
        decoderCtx = avcodec.avcodec_alloc_context3(decoder)
        checkError(avcodec.avcodec_parameters_to_context(decoderCtx, inCodecPar), "Could not copy decoder params")

        // Fallback if container didn't specify sample rate for raw G.711
        if (decoderCtx.sample_rate() <= 0) {
            decoderCtx.sample_rate(8000)
        }
        if (decoderCtx.ch_layout().nb_channels() <= 0) {
            avutil.av_channel_layout_copy(decoderCtx.ch_layout(), channelLayout)
        }
        checkError(avcodec.avcodec_open2(decoderCtx, decoder, (PointerPointer) null), "Could not open decoder")
        // 2. Setup AAC Encoder
        AVCodec encoder = avcodec.avcodec_find_encoder(avcodec.AV_CODEC_ID_AAC)
        if (encoder == null) throw new IllegalStateException("AAC encoder not found")

        encoderCtx = avcodec.avcodec_alloc_context3(encoder)
        encoderCtx.sample_rate(outSampleRate)
        avutil.av_channel_layout_copy(encoderCtx.ch_layout(), channelLayout)
        encoderCtx.sample_fmt(avutil.AV_SAMPLE_FMT_FLTP)
        encoderCtx.bit_rate(aacBitrate)
        encoderCtx.time_base(avutil.av_make_q(1, outSampleRate))

        // Required for MP4/fMP4 containers so extradata (AudioSpecificConfig) is generated
        encoderCtx.flags(encoderCtx.flags() | avcodec.AV_CODEC_FLAG_GLOBAL_HEADER)

        checkError(avcodec.avcodec_open2(encoderCtx, encoder, (PointerPointer) null), "Could not open AAC encoder")

        // 3. Setup Resampler (Converts S16 @ inSampleRate -> FLTP @ outSampleRate)
        swrCtx = new SwrContext()
        checkError(swresample.swr_alloc_set_opts2(
                swrCtx,
                encoderCtx.ch_layout(), encoderCtx.sample_fmt(), encoderCtx.sample_rate(),
                decoderCtx.ch_layout(), decoderCtx.sample_fmt(), decoderCtx.sample_rate(),
                0, null
        ), "Could not allocate resampler")
        checkError(swresample.swr_init(swrCtx), "Could not initialize resampler")

        // 4. Allocate reusable AVFrames
        decodedFrame = avutil.av_frame_alloc()
        resampledFrame = avutil.av_frame_alloc()
        resampledFrame.format(encoderCtx.sample_fmt())
        avutil.av_channel_layout_copy(resampledFrame.ch_layout(), encoderCtx.ch_layout())
        resampledFrame.sample_rate(encoderCtx.sample_rate())
        resampledFrame.nb_samples(encoderCtx.frame_size())
        checkError(avutil.av_frame_get_buffer(resampledFrame, 0), "Could not allocate resampled frame buffer")

        channelLayout.close()
    }

    AVCodecContext getEncoderContext() {
        return encoderCtx
    }

    /**
     * Transcodes an incoming audio AVPacket and aligns timestamps with the input stream's time_base.
     */
    List<AVPacket> transcodePacket(AVPacket inputPacket, AVRational inputTimeBase, AVRational outputTimeBase) {
        List<AVPacket> outputPackets = []

        // Anchor initial PTS to the incoming stream's timeline so audio & video stay in sync
        if (!ptsInitialized && inputPacket.pts() != avutil.AV_NOPTS_VALUE) {
            nextPts = avutil.av_rescale_q(inputPacket.pts(), inputTimeBase, encoderCtx.time_base())
            ptsInitialized = true
        }

        if (avcodec.avcodec_send_packet(decoderCtx, inputPacket) < 0) {
            return outputPackets
        }

        while (avcodec.avcodec_receive_frame(decoderCtx, decodedFrame) >= 0) {
            swresample.swr_convert(swrCtx, null, 0, decodedFrame.data(), decodedFrame.nb_samples())

            while (swresample.swr_get_out_samples(swrCtx, 0) >= encoderCtx.frame_size()) {
                avutil.av_frame_make_writable(resampledFrame)
                swresample.swr_convert(swrCtx, resampledFrame.data(), encoderCtx.frame_size(), null, 0)

                resampledFrame.pts(nextPts)
                nextPts += encoderCtx.frame_size()

                if (avcodec.avcodec_send_frame(encoderCtx, resampledFrame) >= 0) {
                    outputPackets.addAll(drainEncoder(outputTimeBase))
                }
            }
            avutil.av_frame_unref(decodedFrame)
        }

        return outputPackets
    }

    private List<AVPacket> drainEncoder(AVRational outputTimeBase) {
        List<AVPacket> packets = []
        while (true) {
            AVPacket outPkt = avcodec.av_packet_alloc()
            int ret = avcodec.avcodec_receive_packet(encoderCtx, outPkt)
            if (ret == avutil.AVERROR_EAGAIN() || ret == avutil.AVERROR_EOF() || ret < 0) {
                avcodec.av_packet_free(outPkt)
                break
            }

            // Rescale PTS/DTS/duration from encoder time_base (1/44100) to muxer stream time_base
            avcodec.av_packet_rescale_ts(outPkt, encoderCtx.time_base(), outputTimeBase)
            outPkt.stream_index(1) // Explicitly assign to Stream 1 (Audio)
            packets.add(outPkt)
        }
        return packets
    }

    private static void checkError(int code, String message) {
        if (code < 0) {
            BytePointer errBuf = new BytePointer(256)
            avutil.av_strerror(code, errBuf, 256)
            throw new RuntimeException("${message}: ${errBuf.getString()} (${code})")
        }
    }

    @Override
    void close() {
        if (decodedFrame != null) avutil.av_frame_free(decodedFrame)
        if (resampledFrame != null) avutil.av_frame_free(resampledFrame)
        if (swrCtx != null) swresample.swr_free(swrCtx)
        if (decoderCtx != null) avcodec.avcodec_free_context(decoderCtx)
        if (encoderCtx != null) avcodec.avcodec_free_context(encoderCtx)
    }
}
