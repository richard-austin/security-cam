package security.cam.commands

import grails.validation.Validateable
import security.cam.User
import security.cam.UtilsService

class CreateAccountCommand implements Validateable{
    String username
    String password
    String confirmPassword
    String email
    String confirmEmail

    UtilsService utilsService

    static constraints = {
        username(nullable: false, blank: false,
        validator: {username, cmd ->
            if(User.findByCloudAccount(false))
                return "There is already a local web account defined"
            else if(!username.matches(cmd.utilsService.usernameRegex))
                return "Format or length of username is incorrect"
        })
        password(nullable: false, blank: false,
        validator: {password, cmd ->
            if(!password.matches(cmd.utilsService.passwordRegex))
                return "Password is invalid"
        })
        confirmPassword(validator: {confirmPassword, cmd ->
            if(confirmPassword != cmd.password)
                return "Password and confirm password do not match"
        })
        email(nullable: false, blank: false,
        validator: {email, cmd ->
            if(!email.matches(cmd.utilsService.emailRegex))
                return "Email format is not valid"
        })
        confirmEmail(validator: {confirmEmail, cmd ->
            if(confirmEmail != cmd.email)
                return "Email and confirm email do not match"
        })
    }
}
