package security.cam.commands

import grails.validation.Validateable
import security.cam.User

class SetupGuestAccountCommand implements Validateable {
    boolean enabled
    String password
    String confirmPassword

    static constraints = {
        enabled(nullable: false, inList: [true, false])
        password(nullable: true, blank: true, // Empty or blank password means don't change it
                validator: { password, cmd ->
                    User u = User.all.find { it.username == 'guest' && !it.cloudAccount }
                    if (cmd.enabled && u.passwordExpired && (password == "" || password == null))
                        return "The password must be set the first time the guest account is enabled"

                    if (password != null && !password.matches(/^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,64}$/))
                        return "Invalid password, must be minimum eight characters, at least one letter, one number and one special character. (must be <= 64 characters)"
                })
        confirmPassword(nullable: true, blank: true,
                validator: { confirmNewPassword, cmd ->
                    if (confirmNewPassword != cmd.password)
                        return "Passwords do not match"
                })
    }
}
