package security.cam.commands

import grails.validation.Validateable

class DiscoverCameraDetailsCommand implements Validateable {
    String onvifUrl
    static constraints = {
        onvifUrl(nullable: false, blank: false,
                validator: { onvifUrl, cmd ->
                    if (!onvifUrl.matches(/^(?:http:\/\/)?[\w.-]+(?:\.[\w.-]+)+[\w\-._~:\/?#@!$&'()*+,;=]+$/))
                        return "onvifUrl is not a valid URL"

                    return
                })
    }
}
