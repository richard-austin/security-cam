package security.cam.commands

import grails.validation.Validateable
import security.cam.UserAdminService
import security.cam.UtilsService

class AddOrUpdateActiveMQCredsCmd implements Validateable {
    String username
    String password
    String confirmPassword

    UtilsService utilsService
    UserAdminService userAdminService

    static constraints = {
        username(nullable: false, blank: false,
                validator: { username, cmd ->
                    def response = cmd.userAdminService.isGuest()
                    if (response.responseObject.guestAccount)
                        return "Guest not authorised to administer ActiveMQ Credentials"

                    if (!username.matches(cmd.utilsService.usernameRegex))
                        return "Format or length of username is incorrect"
                })
        password(nullable: false, blank: false,
                validator: { password, cmd ->
                    if (!password.matches(cmd.utilsService.passwordRegex))
                        return "Password is invalid"
                })
        confirmPassword(validator: { confirmPassword, cmd ->
            if (confirmPassword != cmd.password)
                return "Password and confirm password do not match"
        })
    }
}
