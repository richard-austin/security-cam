package security.cam.commands

import grails.validation.Validateable
import security.cam.UserAdminService

class CheckNotGuestCommand implements Validateable{
    // This is not used to receive any parameters, it's just to verify the that user is not a guest user
    Object ignore
    UserAdminService userAdminService

    static constraints = {
        ignore(nullable: true,
        validator: {ignore, cmd ->
            def result = cmd.userAdminService.isGuest()
            if(result.responseObject.guestAccount)
                return "Guest not authorised to use this function"
            return
        })
    }
}
