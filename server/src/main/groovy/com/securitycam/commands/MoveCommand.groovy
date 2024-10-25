package com.securitycam.commands

class MoveCommand extends PtzCommands{
    static final enum eMoveDirections {tiltUp, tiltDown, panLeft, panRight, zoomIn, zoomOut}
    eMoveDirections moveDirection
    String onvifBaseAddress
}
