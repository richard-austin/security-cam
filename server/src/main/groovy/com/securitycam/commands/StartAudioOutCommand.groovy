package com.securitycam.commands

import com.securitycam.controllers.Camera
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

class StartAudioOutCommand{
    @NotNull(message="Camera is mandatory")
    Camera cam

    @NotNull(message="netcam_uri is mandatory")
    @NotBlank(message="netcam_uri must not be empty")
    String netcam_uri
}
