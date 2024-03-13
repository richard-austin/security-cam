package security.cam.commands

import grails.validation.Validateable
import security.cam.UtilsService

class PTZPresetsCommand extends PtzCommands{
    static final enum ePresetOperations {moveTo, saveTo, clearFrom}
    ePresetOperations operation
    String onvifBaseAddress
    String preset

    static constraints = {
        operation(nullable: false, inList:[ePresetOperations.moveTo, ePresetOperations.saveTo, ePresetOperations.clearFrom])
        onvifBaseAddress(nullable: false, empty: false,
        validator: {onvifBaseAddress ->
            if(!onvifBaseAddress.matches(UtilsService.onvifBaseAddressRegex))
                return "Invalid onvifBaseAddress ${onvifBaseAddress}"
        })
        preset(nullable: false, empty: false)
    }
}
