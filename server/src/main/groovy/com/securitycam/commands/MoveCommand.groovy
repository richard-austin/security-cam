package com.securitycam.commands

class MoveCommand extends PtzCommand{
    static enum eMoveDirections {tiltUp, tiltDown, panLeft, panRight, zoomIn, zoomOut}
    eMoveDirections moveDirection
}
