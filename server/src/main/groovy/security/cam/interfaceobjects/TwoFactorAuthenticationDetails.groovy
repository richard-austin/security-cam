package security.cam.interfaceobjects

import groovy.transform.CompileStatic
import org.springframework.security.web.authentication.WebAuthenticationDetails
import security.cam.LogService
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest

@CompileStatic
class TwoFactorAuthenticationDetails extends WebAuthenticationDetails{
    LogService logService

    String xAuthToken

    TwoFactorAuthenticationDetails(HttpServletRequest request) {
        super(request)
        getXAuthHeader(request)
    }

    void getXAuthHeader(HttpServletRequest request) throws IOException, ServletException {
        xAuthToken = request.getHeader("x-auth-token")
    }
}
