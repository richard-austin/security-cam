package com.securitycam.commands

import com.securitycam.services.UtilsService

class PTZPresetsCommand extends PtzCommands {
    static final enum ePresetOperations {moveTo, saveTo, clearFrom}
    ePresetOperations operation
    String onvifBaseAddress

    String preset
}
