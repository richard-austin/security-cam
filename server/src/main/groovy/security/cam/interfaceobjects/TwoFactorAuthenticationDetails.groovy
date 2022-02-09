package security.cam.interfaceobjects

import groovy.transform.CompileStatic
import org.springframework.security.web.authentication.WebAuthenticationDetails
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest

@CompileStatic
class TwoFactorAuthenticationDetails extends WebAuthenticationDetails{
    final String xAuthToken

    TwoFactorAuthenticationDetails(HttpServletRequest request) {
        super(request)
        xAuthToken = getXAuthHeader(request)
    }

    private String getXAuthHeader(HttpServletRequest request) throws IOException, ServletException {
        return request.getHeader("x-auth-token")
    }
}
