package security.cam.eventlisteners

import grails.gorm.transactions.Transactional
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetails
import security.cam.LogService
import security.cam.User
import security.cam.interfaceobjects.TwoFactorAuthenticationDetails

class TwoFactorAuthenticationProvider extends DaoAuthenticationProvider{
    LogService logService

    protected void additionalAuthenticationChecks(UserDetails userDetails,
                                                  UsernamePasswordAuthenticationToken authentication)
            throws AuthenticationException {

        super.additionalAuthenticationChecks(userDetails, authentication)

        Object details = authentication.details

        if(details != null) {  // UserDetails are null when change password used
            if (!(details instanceof TwoFactorAuthenticationDetails)) {
                logService.cam.debug("Authentication failed: authenticationToken principal is not a TwoFactorPrincipal")
                throw new BadCredentialsException(messages.getMessage(
                        "AbstractUserDetailsAuthenticationProvider.badCredentials",
                        "Bad credentials"))
            }
            TwoFactorAuthenticationDetails tfad = details
            String userName = userDetails.getUsername()
            if (getIsCloudAccount(userName) && tfad.xAuthToken != requiredXAuthToken(userName)) {
                logService.cam.debug("Authentication failed: authtoken incorrect for Cloud account valid")
                throw new BadCredentialsException(messages.getMessage(
                        "AbstractUserDetailsAuthenticationProvider.badCredentials",
                        "Bad credentials"))
            }
        }
        else if(getIsCloudAccount(userDetails.getUsername()))
        {
            logService.cam.debug("Authentication failed: no TwoFactorAuthenticationDetails for cloud account: "+userDetails.getUsername())
            throw new BadCredentialsException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.badCredentials",
                    "Bad credentials"))
        }
    }

    @Transactional
    private boolean getIsCloudAccount(String userName)
    {
        try {
            User user = User.findByUsername(userName)

            return user.cloudAccount
        }
        catch(Exception ex)
        {
            logService.cam.error(ex.getClass().getName()+" exception in isCloudAccount: "+ex.getMessage())
        }
        true
    }

    @Transactional
    private String requiredXAuthToken(String userName)
    {
        try {
            User user = User.findByUsername(userName)

            return user.header
        }
        catch(Exception ex)
        {
            logService.cam.error(ex.getClass().getName()+" exception in header: "+ex.getMessage())
        }
        ""
    }
}
