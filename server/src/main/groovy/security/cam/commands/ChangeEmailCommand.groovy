package security.cam.commands

import grails.validation.Validateable
import security.cam.UtilsService

class ChangeEmailCommand implements Validateable{
    String email
    String confirmEmail

    UtilsService utilsService

    static constraints = {
        email(nullable: false, blank: false,
        validator: {email, cmd ->
            if(!email.matches(cmd.utilsService.emailRegex))
                return "Email address is not in the correct format"
        })
        confirmEmail(validator: {confirmEmail, cmd ->
            if(confirmEmail != cmd.email)
                return "email and conformEmail must match"
        })
    }
}
