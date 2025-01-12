package com.securitycam.interfaceobjects

import org.springframework.security.authentication.AccountStatusException

class TwoFactorAuthenticationException extends AccountStatusException {
    TwoFactorAuthenticationException(String message) {
        super(message)
    }

    TwoFactorAuthenticationException(String msg, Throwable cause) {
        super(msg, cause)
    }
}
