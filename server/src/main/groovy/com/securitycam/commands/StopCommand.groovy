package com.securitycam.commands

import com.securitycam.services.UtilsService


class StopCommand extends PtzCommands{
    String onvifBaseAddress

//    static constraints = {
//        onvifBaseAddress(blank: false, nullable: false,
//                validator: {onvifBaseAddress ->
//                    if(!onvifBaseAddress.matches(UtilsService.onvifBaseAddressRegex))
//                        return "Onvif Base Address is invalid"
//                })
//    }

}
