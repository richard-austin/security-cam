package com.securitycam.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.web.authentication.WebAuthenticationDetails
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Service

class TwoFactorAuthenticationDetailsSource extends WebAuthenticationDetailsSource {
    @Override
    WebAuthenticationDetails buildDetails(HttpServletRequest context) {
        TwoFactorAuthenticationDetails details = new TwoFactorAuthenticationDetails(context)
        details
    }
}
