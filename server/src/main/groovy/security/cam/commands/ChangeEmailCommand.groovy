package security.cam.commands

import grails.plugin.springsecurity.SpringSecurityService
import grails.validation.Validateable
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import security.cam.UtilsService

class ChangeEmailCommand implements Validateable{
    String password
    String newEmail
    String confirmNewEmail

    UtilsService utilsService
    SpringSecurityService springSecurityService
    AuthenticationManager authenticationManager

    static constraints = {
        password(nullable: false, blank: false,
                validator: {password , cmd ->
                    // Check the old password is correct
                    def principal = cmd.springSecurityService.getPrincipal()
                    if(principal) {   // No principal in dev mode
                        String userName = principal.getUsername()

                        boolean valid = true
                        try {
                            cmd.authenticationManager.authenticate new UsernamePasswordAuthenticationToken(userName, password)
                        }
                        catch (BadCredentialsException ignored) {
                            valid = false
                        }

                        if (!valid /*!passwordEncoder.matches(oldPassword, pw)*/)
                            return "The password is incorrect"
                    }
                })
        newEmail(nullable: false, blank: false,
        validator: {email, cmd ->
            if(!email.matches(cmd.utilsService.emailRegex))
                return "Email address is not in the correct format"
        })
        confirmNewEmail(validator: { confirmNewEmail, cmd ->
            if(confirmNewEmail != cmd.newEmail)
                return "email and conformEmail must match"
        })
        authenticationManager(nullable: true)
    }
}
