package com.securitycam.error

import lombok.Getter

@Getter
final class NVRRestMethodException extends Exception{
    String reason
    NVRRestMethodException(String message, String reason) {
        super(message)
        this.reason = reason
    }
}
