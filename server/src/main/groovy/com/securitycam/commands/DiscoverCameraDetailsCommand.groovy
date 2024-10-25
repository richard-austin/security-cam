package com.securitycam.commands


class DiscoverCameraDetailsCommand {
    String onvifUrl
    String onvifUserName
    String onvifPassword
    static constraints = {
        onvifUrl(nullable: true, blank: true,
                validator: { onvifUrl, cmd ->
                    if(onvifUrl == null || onvifUrl == "")
                        return
                    else if (!onvifUrl.matches(/^(?:http:\/\/)?[\w.-]+(?:\.[\w.-]+)+[\w\-._~:\/?#@!$&'()*+,;=]+$/))
                        return "onvifUrl is not a valid URL"

                    return
                })
        onvifUserName(blank: true, nullable: true,  maxSize: 20,
                validator: { onvifUserName, cmd ->
                    if (onvifUserName == null || onvifUserName == "") {
                        if (cmd.onvifPassword == null || cmd.onvifPassword == "")
                            return
                        else
                            return "If onvifUserName is empty, onvifPassword must also be empty"
                    } else if (!onvifUserName.matches(/^[a-zA-Z0-9](_(?!([._]))|\.(?!([_.]))|[a-zA-Z0-9]){3,18}[a-zA-Z0-9]$/))
                        return "Invalid user name, must be 5-20 characters containing a-z, A-Z, 0-9 . and _ and starting with an alpha numeric character."
                    return
                })
        onvifPassword(nullable: true, blank: true, maxSize: 25,
                validator: { onvifPassword, cmd ->
                    if (onvifPassword == null || onvifPassword == "") {
                        if (cmd.onvifUserName == null || cmd.onvifUserName == "")
                            return
                        else
                            return "If onvifPassword is empty, onvifUserName must also be empty"
                    }
                    if (!onvifPassword.matches(/^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,64}$/))
                        return "Invalid password, must be minimum eight characters, at least one letter, one number and one special character."
                    return
                })
    }
}
