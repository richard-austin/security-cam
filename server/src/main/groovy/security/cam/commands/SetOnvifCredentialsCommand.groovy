package security.cam.commands

import grails.validation.Validateable

class SetOnvifCredentialsCommand implements Validateable {
    String onvifUserName
    String onvifPassword

    static constraints = {
        onvifUserName(nullable: false, maxSize: 20,
        validator: {camerasAdminUserName ->
            if(!camerasAdminUserName.matches(/^[a-zA-Z0-9](_(?!([._]))|\.(?!([_.]))|[a-zA-Z0-9]){3,18}[a-zA-Z0-9]$/))
                return "Invalid user name, must be 5-20 characters containing a-z, A-Z, 0-9 . and _ and starting with an alpha numeric character."
            return
        })
        onvifPassword(nullable: false, maxSize: 25,
        validator: {camerasAdminPassword ->
            if(!camerasAdminPassword.matches(/^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,64}$/))
                return "Invalid password, must be minimum eight characters, at least one letter, one number and one special character."
            return
        })
    }
}
