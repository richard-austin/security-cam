package com.securitycam.commands

import com.securitycam.validators.IsBoolean
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

class SetUpWifiCommand {
    @NotNull
    @NotEmpty
    String ssid

    String password

    @IsBoolean
    boolean isCloud = true
 }
