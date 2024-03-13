package security.cam.commands

import grails.validation.Validateable
import security.cam.UtilsService

class PTZPresetsInfoCommand extends PtzCommands {
    String onvifBaseAddress

    static constraints = {
        onvifBaseAddress(nullable: false, empty: false,
                validator: { onvifBaseAddress ->
                    if (!onvifBaseAddress.matches(UtilsService.onvifBaseAddressRegex))
                        return "Invalid onvifBaseAddress ${onvifBaseAddress}"
                })
    }
}
