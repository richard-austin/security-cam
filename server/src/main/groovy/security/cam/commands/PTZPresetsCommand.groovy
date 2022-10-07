package security.cam.commands

import grails.validation.Validateable
import security.cam.UtilsService

class PTZPresetsCommand implements Validateable{
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
        preset(nullable: false, empty: false,
        validator: {preset ->
            try {
                int iPreset = Integer.parseInt(preset)
                if(iPreset > 32 || iPreset < 1)
                    return "Invalid preset number (${iPreset})"
            }
            catch(Exception ex)
            {
                return "Invalid preset value (${preset}): ${ex.getMessage()}"
            }
        })
    }

}
