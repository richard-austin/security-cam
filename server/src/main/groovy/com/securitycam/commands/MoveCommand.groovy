package com.securitycam.commands

class MoveCommand extends PtzCommand{
    static final enum eMoveDirections {tiltUp, tiltDown, panLeft, panRight, zoomIn, zoomOut}
    eMoveDirections moveDirection
}
