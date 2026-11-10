package com.securitycam.mediaserverutils

class CodecStringDiscovery {
    static String err = ""

    static String constructCodecString(String codecName, String profile, int level) {
        String codec = codecName

        switch (codec) {
            case "hevc":
            codec = "hvc1"
            if (profile == "Main") {
                codec += ".1.4"
            } else if (profile == "Main 10") {
                codec += ".2.4"
            } else {
                err = "Unknown profile " + profile
                break
            }

            codec += ".L" + level.toString()
            codec += ".B0"
            break
            case "h264":
            codec = "avc1"
            if (profile == "Baseline") {
                codec += ".4200"
            } else if (profile == "Main") {
                codec += ".4D40"
            } else if (profile == "High") {
                codec += ".6400"
            } else {
                err ="Unknown profile " + profile
                break
            }
            codec += String.format("%X", level)
            break
            default:
            err = "Unknown codec name " + codecName
        }
        return codec
    }
}
