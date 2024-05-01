package security.cam.commands

import grails.validation.Validateable
import security.cam.UserAdminService
import security.cam.UtilsService

class AddOrUpdateActiveMQCredsCmd implements Validateable {
    String username
    String password
    String confirmPassword
    String mqHost

    UtilsService utilsService
    UserAdminService userAdminService
    public final activeMQPasswordRegex = /^$|^[A-Za-z0-9]{20}$/
    static constraints = {
        username(nullable: true, blank: true,
                validator: { username, cmd ->
                    def response = cmd.userAdminService.isGuest()
                    if (response.responseObject.guestAccount)
                        return "Guest not authorised to administer ActiveMQ Credentials"
                    if (username == null)
                        username = cmd.username = ""
                    if (!username.matches(cmd.utilsService.usernameRegex) && username != "")
                        return "Format or length of username is incorrect"
                })
        password(nullable: true, blank: true,
                validator: { password, cmd ->
                    if (password == null)
                        password = cmd.password = ""
                    if (!password.matches(cmd.activeMQPasswordRegex))
                        return "Password is invalid"

                    if(cmd.username == "" && password != "")
                        return "Password must be blank if username is blank"
                    else if (cmd.username != "" && password == "")
                        return "Password cannot be blank if username is not blank"
                })
        confirmPassword(nullable: true, blank: true,
                validator: { confirmPassword, cmd ->
                    if (confirmPassword == null)
                        confirmPassword = cmd.confirmPassword = ""
                    if (confirmPassword != cmd.password)
                        return "Password and confirm password do not match"
                })
        mqHost(nullable: false, blank: false,
                validator: { mqHost, cmd ->
                    if (!mqHost.matches(CameraParamsCommand.hostNameRegex) &&
                            !mqHost.matches(CameraParamsCommand.ipV4RegEx) &&
                            !mqHost.matches(CameraParamsCommand.ipV6RegEx)) {
                        return "Host name is invalid"
                    }
                })
    }
}
