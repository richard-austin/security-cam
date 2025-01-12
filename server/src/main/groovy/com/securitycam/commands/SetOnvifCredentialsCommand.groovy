package com.securitycam.commands

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

class SetOnvifCredentialsCommand {
    @NotNull(message="onvifUserName cannot be null")
    @Size(max=20, message="onvifUserName max length is 20")
    @Pattern(regexp="^[a-zA-Z0-9](_(?!([._]))|\\.(?!([_.]))|[a-zA-Z0-9]){3,18}[a-zA-Z0-9]\$", message="Invalid user name, must be 5-20 characters containing a-z, A-Z, 0-9 . and _ and starting with an alpha numeric character.")
    String onvifUserName

    @NotNull(message="onvifPassword cannot be null")
    @Size(max=25, message="onvifPassword max length is 25")
    @Pattern(regexp="^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@\$!%*#?&])[A-Za-z\\d@\$!%*#?&]{8,64}\$", message="Invalid password, must be minimum eight characters, at least one letter, one number and one special character.")
    String onvifPassword
}
