package com.securitycam.security

import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.web.authentication.WebAuthenticationDetails

class TwoFactorAuthenticationDetails extends WebAuthenticationDetails{
    final String xAuthToken

    TwoFactorAuthenticationDetails(HttpServletRequest request) {
        super(request)
        xAuthToken = getXAuthHeader(request)
    }

    private static String getXAuthHeader(HttpServletRequest request) throws IOException, ServletException {
        return request.getHeader("x-auth-token")
    }
}

