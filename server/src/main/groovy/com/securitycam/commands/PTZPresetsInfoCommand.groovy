package com.securitycam.commands

import com.securitycam.services.UtilsService

class PTZPresetsInfoCommand extends PtzCommands {
    String onvifBaseAddress

//    static constraints = {
//        onvifBaseAddress(nullable: false, empty: false,
//                validator: { onvifBaseAddress ->
//                    if (!onvifBaseAddress.matches(UtilsService.onvifBaseAddressRegex))
//                        return "Invalid onvifBaseAddress ${onvifBaseAddress}"
//                })
//    }
}
