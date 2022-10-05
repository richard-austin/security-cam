package security.cam.commands

import grails.validation.Validateable
import security.cam.UtilsService

class MoveCommand implements Validateable{
    static final enum eMoveDirections {tiltUp, tiltDown, panLeft, panRight}

    eMoveDirections moveDirection

    String onvifBaseAddress

    static constraints = {
        moveDirection(blank: false, nullable: false,
                inList:[eMoveDirections.tiltUp,
                        eMoveDirections.tiltDown,
                        eMoveDirections.panLeft,
                        eMoveDirections.panRight] )
        onvifBaseAddress(blank: false, nullable: false,
        validator: {onvifBaseAddress ->
            if(!onvifBaseAddress.matches(UtilsService.onvifBaseAddressRegex))
                return "Onvif Base Address is invalid"
        })
    }
}
