package security.cam.interfaceobjects

import org.springframework.security.web.authentication.WebAuthenticationDetails
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource

import javax.servlet.http.HttpServletRequest

class TwoFactorAuthenticationDetailsSource extends WebAuthenticationDetailsSource {

    @Override
    WebAuthenticationDetails buildDetails(HttpServletRequest context) {
        TwoFactorAuthenticationDetails details = new TwoFactorAuthenticationDetails(context)
        details
    }
}
