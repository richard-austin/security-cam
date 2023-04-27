package security.cam.commands

import grails.validation.Validateable

class ResetPasswordFromLinkCommand implements Validateable {
    String resetKey
    String newPassword
    String confirmNewPassword

    static constraints = {
        resetKey(nullable: false, blank: false)
        newPassword(nullable: false, blank: false,
                validator: { newPassword, cmd ->
                    if (!newPassword.matches(/^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,64}$/))
                        return "Invalid password, must be minimum eight characters, at least one letter, one number and one special character. (must be <= 64 characters)"
                })

        confirmNewPassword(validator: { confirmNewPassword, cmd ->
            if (confirmNewPassword != cmd.newPassword)
                return "New passwords do not match"
        })
    }
}
