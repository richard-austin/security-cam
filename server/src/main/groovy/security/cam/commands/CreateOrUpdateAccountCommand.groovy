package security.cam.commands

import grails.validation.Validateable
import security.cam.Role
import security.cam.User
import security.cam.UserAdminService
import security.cam.UtilsService

class CreateOrUpdateAccountCommand implements Validateable{
    String username
    String password
    String confirmPassword
    String email
    String confirmEmail
    boolean updateExisting = false

    UtilsService utilsService
    UserAdminService userAdminService

    static constraints = {
        username(nullable: false, blank: false,
        validator: {username, cmd ->
            def response = cmd.userAdminService.isGuest()
            if(response.responseObject.guestAccount)
                return "Guest not authorised to administer user account"

            User user = User.findByUsername(username)
            if(user) {
                boolean invalidUserName = false

                Set<Role> roles = user.getAuthorities()
                roles.forEach {role ->
                    if(role.authority == 'ROLE_GUEST' || role.authority == 'ROLE_CLOUD')
                        invalidUserName = true
                }
                if (invalidUserName)
                    return "Invalid user name"
            }
            if(!cmd.updateExisting && User.all.find{it.username != 'guest' && !it.cloudAccount} != null)
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
        updateExisting(nullable: false, inList:[true, false])
    }
}
