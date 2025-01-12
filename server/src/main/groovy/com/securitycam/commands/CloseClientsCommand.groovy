package com.securitycam.commands

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull


class CloseClientsCommand {
    @NotNull
    @NotBlank
    String accessToken
}
