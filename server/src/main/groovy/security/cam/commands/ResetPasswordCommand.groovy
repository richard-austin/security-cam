package security.cam.commands

import grails.plugin.springsecurity.SpringSecurityService
import grails.validation.Validateable
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import security.cam.User

class ResetPasswordCommand implements Validateable{
    String oldPassword
    String newPassword
    SpringSecurityService springSecurityService

    static constraints = {
        oldPassword(nullable: false, blank: false,
        validator: {oldPassword ->
            // Check the old password is correct
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder()
            String userName = springSecurityService.getPrincipal().getName()
            User user = User.findByUsername(userName)

            if(!passwordEncoder.matches(oldPassword, user.getPassword()))
                return "The old password given is incorrect"
        })

        newPassword(nullable: false, blank: false,
        validator: {newPassword, cmd ->
            if(!/^[-\[\]!\"#$%&\'()*+,.\/:;<=>?@^_\`{}|~\\0-9A-Za-z]{1,64}$/.matches(newPassword))
                return "New password contains invalid characters or is too long (must be <= 64 characters)"
        })
    }
}
