package com.securitycam.recording

import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.javacpp.BytePointer
import static org.bytedeco.ffmpeg.global.avcodec.*

/**
 * Creates a completely independent deep copy of an AVPacket.
 * Safe to pass across threads without shared AVBufferRef race conditions.
 */
static AVPacket deepCopyPacket(AVPacket src) {
    if (src == null || src.isNull()) {
        return null
    }

    AVPacket dst = av_packet_alloc()
    if (dst == null || dst.isNull()) {
        throw new OutOfMemoryError("Failed to allocate AVPacket")
    }

    int size = src.size()
    if (size > 0) {
        int ret = av_new_packet(dst, size)
        if (ret < 0) {
            av_packet_free(dst)
            dst.setNull()
            throw new RuntimeException("av_new_packet failed with error code: " + ret)
        }
        BytePointer.memcpy(dst.data(), src.data(), size)
    }

    int propsRet = av_packet_copy_props(dst, src)
    if (propsRet < 0) {
        av_packet_free(dst)
        dst.setNull()
        throw new RuntimeException("av_packet_copy_props failed with error code: " + propsRet)
    }

    return dst
}

/**
 * Safely frees a deep-copied AVPacket and marks the JavaCPP pointer null
 * to prevent double-frees.
 */
static void freePacket(AVPacket pkt) {
    if (pkt != null && !pkt.isNull()) {
        av_packet_free(pkt)
        pkt.setNull()
    }
}
