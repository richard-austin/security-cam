package com.securitycam.commands

import com.securitycam.validators.InListStr
import com.securitycam.validators.IsBoolean

class SetWifiStatusCommand {
    @InListStr(values=["on", "off"], message = "status value must be 'on' or 'off'")
    String status

    @IsBoolean
    boolean isCloud = true
}
