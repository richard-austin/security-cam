package com.securitycam.eventlisteners

import com.securitycam.services.LogService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.logout.LogoutHandler
import org.springframework.stereotype.Component

/**
 * SecCamSecurityEventListener: Bean to log successful login events and logouts.
 */
@Component
class SecCamSecurityEventListener implements ApplicationListener<AuthenticationSuccessEvent>, LogoutHandler {
    @Autowired LogService logService

    @Override
    void onApplicationEvent(AuthenticationSuccessEvent event) {
        loginSuccess(event?.authentication?.principal?.getUsername() as String)
    }

    @Override
    void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {

        logoutAction(authentication?.principal?.getUsername() as String)
    }

    def loginSuccess(String userName) {
        logAudit("USER-LOGIN_SUCCESS: ", "user='${userName}")
    }

    def logoutAction(String userName) {
        logAudit("USER-LOGOUT", "user='${userName}")
    }

    private void logAudit(String auditType, def message) {
        logService.cam.info "Audit:${auditType}- ${message.toString()}"
    }

}

/**
 * SecCamAuthFailEventListener: Bean to log unsuccessful log in events
 */
@Component
class SecCamAuthFailEventListener implements ApplicationListener<AuthenticationFailureBadCredentialsEvent>
{
    @Autowired LogService logService

    @Override
    void onApplicationEvent(AuthenticationFailureBadCredentialsEvent event) {

        loginFailure(event?.authentication?.principal as String)
    }

    def loginFailure(String userName) {
        logAudit("USER-LOGIN-FAILURE", "user='${userName}")
    }

    private void logAudit(String auditType, def message) {
        logService.cam.info "Audit:${auditType}- ${message.toString()}"
    }
}
