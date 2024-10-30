package com.securitycam.commands

import com.securitycam.proxies.IResetTimerCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull


class ResetTimerCommand implements IResetTimerCommand {
    @NotNull
    @NotBlank
    String accessToken
}
