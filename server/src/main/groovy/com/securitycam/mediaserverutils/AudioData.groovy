package com.securitycam.mediaserverutils

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper

@JsonIgnoreProperties(["metaClass"])
class AudioData {
    String codec_type
    String codec_name
    String sample_fmt
    String sample_rate
    int channels
    String bit_rate

    private static final ObjectMapper MAPPER = new ObjectMapper()

    AudioData(AVData data) {
        codec_type = "audio"
        codec_name = data.inputAudioCodecName
        sample_rate = data.sampleRate
        sample_fmt = data.sampleFormat
        channels = data.audioChannels
        bit_rate = data.audioBitRate
    }

    String toTextMessage() {
        String json

        try {
            // 1. Serialize POJO to JSON String
            json = MAPPER.writeValueAsString(this)
            // 2. Send via Spring WebSocket
        } catch (Exception e) {
            e.printStackTrace()
            json = ""
        }
        return json
    }
}
