package com.securitycam.mediaserverutils

import org.bytedeco.ffmpeg.avformat.AVFormatContext
import org.bytedeco.ffmpeg.avformat.AVStream
import org.bytedeco.javacv.FFmpegFrameGrabber

import static org.bytedeco.ffmpeg.global.avcodec.avcodec_get_name
import org.bytedeco.ffmpeg.global.avutil

class AVData {
    String inputAudioCodecName
    int width
    int height
    int audioChannels

    int videoStreamIndex
    int videoStreamIndex2
    int audioStreamIndex

    int inputVideoCodecId
    int inputAudioCodecId
    String inputVideoCodecName
    AVFormatContext inputFormatContext

    AVData(FFmpegFrameGrabber grabber) {
        width = grabber.getImageWidth()
        height = grabber.getImageHeight()
        audioChannels = grabber.getAudioChannels()

        videoStreamIndex = grabber.getVideoStream()
        audioStreamIndex = grabber.getAudioStream()

        inputVideoCodecId = grabber.getFormatContext().streams(videoStreamIndex).codecpar().codec_id()
        inputAudioCodecId = audioStreamIndex != -1 ? grabber.getFormatContext().streams(audioStreamIndex).codecpar().codec_id() : -1
        inputVideoCodecName = avcodec_get_name(inputVideoCodecId).getString()

        if (audioChannels > 0)
            inputAudioCodecName = avcodec_get_name(inputAudioCodecId).getString()
        else
            inputAudioCodecName = "No Audio"

        inputFormatContext = grabber.getFormatContext()
        // Find the video stream inside your input format context
        videoStreamIndex2 = -1
        for (int i = 0; i < inputFormatContext.nb_streams(); i++) {
            AVStream stream = inputFormatContext.streams(i)
            if (stream.codecpar().codec_type() == avutil.AVMEDIA_TYPE_VIDEO) {
                videoStreamIndex2 = i
                break
            }
        }
    }
}

