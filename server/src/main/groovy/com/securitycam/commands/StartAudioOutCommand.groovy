package com.securitycam.commands

import com.securitycam.controllers.Camera
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

class StartAudioOutCommand{
    @NotNull
    @NotBlank
    Camera cam

    @NotNull
    @NotBlank
    String netcam_uri
}
