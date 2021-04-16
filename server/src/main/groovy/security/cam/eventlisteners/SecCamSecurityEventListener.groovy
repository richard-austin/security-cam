package security.cam.eventlisteners

import org.springframework.context.ApplicationListener
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.logout.LogoutHandler

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * SecCamSecurityEventListener: Bean to log successful login events and logouts.
 */
class SecCamSecurityEventListener implements ApplicationListener<AuthenticationSuccessEvent>, LogoutHandler{
    def logService
    void onApplicationEvent(AuthenticationSuccessEvent event) {

        loginSuccess(event?.authentication?.principal?.username as String)
    }

    void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {

        logoutAction(authentication?.principal?.username as String)
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
class SecCamAuthFailEventListener implements ApplicationListener<AuthenticationFailureBadCredentialsEvent>,  LogoutHandler
{
    def logService
    void onApplicationEvent(AuthenticationFailureBadCredentialsEvent event) {

        loginFailure(event?.authentication?.principal as String)
    }

    def loginFailure(String userName) {
        logAudit("USER-LOGIN-FAILURE", "user='${userName}")
    }

    private void logAudit(String auditType, def message) {
        logService.cam.info "Audit:${auditType}- ${message.toString()}"
    }

    @Override
    void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        // Do nothing, it's just included as it's expected to be here and an exception occurs without it.
    }
}