package com.securitycam.commands

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

class CameraParamsCommand {
    String address
    String uri
    String params
}
