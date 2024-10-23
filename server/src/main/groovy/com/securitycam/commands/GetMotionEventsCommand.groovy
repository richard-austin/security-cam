package com.securitycam.commands

import com.securitycam.controllers.Camera
import com.securitycam.controllers.Stream
import com.securitycam.validators.IsCamera
import jakarta.validation.constraints.NotNull


class GetMotionEventsCommand {
    @NotNull(message = "Stream must be defined")
    Stream stream

    @NotNull(message = "Camera must be defined")
    @IsCamera
    Camera cam
}
