package com.securitycam.commands

class PTZPresetsCommand extends PtzCommand {
    static enum ePresetOperations {moveTo, saveTo, clearFrom}
    ePresetOperations operation

    String preset
}
