package com.securitycam.audiobackchannel

class RtspException extends Throwable {
    RtspException(String message) {
        super("RtspException: "+message)
    }
}
