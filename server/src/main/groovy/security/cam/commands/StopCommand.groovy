package security.cam.commands

import grails.validation.Validateable
import security.cam.UtilsService

class StopCommand extends PtzCommands{
    String onvifBaseAddress

    static constraints = {
        onvifBaseAddress(blank: false, nullable: false,
                validator: {onvifBaseAddress ->
                    if(!onvifBaseAddress.matches(UtilsService.onvifBaseAddressRegex))
                        return "Onvif Base Address is invalid"
                })
    }

}
