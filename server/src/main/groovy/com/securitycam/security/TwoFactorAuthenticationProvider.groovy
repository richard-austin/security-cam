package com.securitycam.security

import com.securitycam.model.User
import com.securitycam.services.LogService
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component


@Component
class TwoFactorAuthenticationProvider extends DaoAuthenticationProvider{
    LogService logService

    TwoFactorAuthenticationProvider(MyUserDetailsService userDetailsService, PasswordEncoder passwordEncoder, LogService logService) {
        super(passwordEncoder)
        super.userDetailsService = userDetailsService
        super.passwordEncoder = passwordEncoder
        this.logService = logService
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails,  UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        super.additionalAuthenticationChecks(userDetails, authentication)
        Object details = authentication.details

        if(details != null) {  // UserDetails are null when change password used
            if (!(details instanceof TwoFactorAuthenticationDetails)) {
                logService.cam.debug("Authentication failed: authenticationToken principal is not a TwoFactorPrincipal")
                throw new BadCredentialsException(messages.getMessage(
                        "AbstractUserDetailsAuthenticationProvider.badCredentials",
                        "Bad credentials"))
            }
            if(!userDetails.isCredentialsNonExpired()) {
                logService.cam.debug("User credentials for ${userDetails.username} have expired")
                throw new BadCredentialsException(messages.getMessage(
                        "AbstractUserDetailsAuthenticationProvider.badCredentials",
                        "User credentials have expired"))
            }
            TwoFactorAuthenticationDetails tfad = details
            String userName = userDetails.getUsername()
            if (getIsCloudAccount(userName) && tfad.xAuthToken != requiredXAuthToken(userName)) {
                logService.cam.debug("Authentication failed: authtoken incorrect for Cloud account")
                throw new BadCredentialsException(messages.getMessage(
                        "AbstractUserDetailsAuthenticationProvider.badCredentials",
                        "Bad credentials for cloud account"))
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

    boolean getIsCloudAccount(String userName)
    {
        try {
            User user = userDetailsService.loadUserByUsername(userName) as User

            return user.cloudAccount
        }
        catch(Exception ex)
        {
            logService.cam.error(ex.getClass().getName()+" exception in isCloudAccount: "+ex.getMessage())
        }
        false
    }

    String requiredXAuthToken(String userName)
    {
        try {
            User user = userDetailsService.loadUserByUsername(userName) as User

            return user.header
        }
        catch(Exception ex)
        {
            logService.cam.error(ex.getClass().getName()+" exception in header: "+ex.getMessage())
        }
        ""
    }

    @Override
    boolean supports(Class<?> authentication) {
        return authentication == UsernamePasswordAuthenticationToken.class
    }
}
