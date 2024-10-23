package com.securitycam.error

import lombok.Getter

@Getter
final class NVRRestMethodException extends RuntimeException{
    String reason
    String requestUri
    NVRRestMethodException(String message, String requestUri="", String reason="") {
        super(message)
        this.reason = reason
        this.requestUri = requestUri
    }
}
