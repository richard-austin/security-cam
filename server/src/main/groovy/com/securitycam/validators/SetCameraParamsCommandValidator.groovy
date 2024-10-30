package com.securitycam.validators

import com.securitycam.commands.SetCameraParamsCommand
import com.securitycam.controllers.cameraType
import org.springframework.validation.Errors
import org.springframework.validation.Validator

import java.nio.charset.StandardCharsets

class SetCameraParamsCommandValidator implements Validator {
    @Override
    boolean supports(Class<?> clazz) {
        return SetCameraParamsCommand.class == clazz
    }

    static private final Set<Integer> cameraTypeValues = [1, 2]
    static private final Set<String> infraredstatValues = [null, '', 'auto', 'open', 'close']
    static private final Set<String> wdrValues = [null, "on", "off"]
    static private final Set<String> lamp_modeValues = [null, "0", "1", "2"]
    static private final Set<Boolean> rebootValues = [true, false]

    @Override
    void validate(Object target, Errors errors) {
        if (target instanceof SetCameraParamsCommand) {
            if (target.cameraType == null)
                errors.rejectValue("cameraType", "cameraType cannot ne null")
            else {
                if (!cameraTypeValues.contains(target.cameraType))
                    errors.rejectValue("cameraType", "cameraType has an invalid value (${target.cameraType})")
            }
            target.params = target.params2 = ""

            if (!infraredstatValues.contains(target.infraredstat))
                errors.rejectValue("infraredstat", "infraredstat has an invalid value (${target.infraredstat})")
            else {
                if (target.getCameraType() == cameraType.sv3c.ordinal() && !target.reboot) {
                    // Not validating, just setting up params in the base class from the command values
                    target.params = 'cmd=setinfrared'
                    target.params += "&-infraredstat=${target.infraredstat}"
                    target.params += "&cmd=setoverlayattr&-region=0&cmd=setoverlayattr&-region=1"
                    target.params += "&-name=${target.cameraName}"
                }
            }

            if (!wdrValues.contains(target.wdr))
                errors.rejectValue("wdr", "wdr has an invalid value (${target.wdr})")

            if (!lamp_modeValues.contains(target.lamp_mode))
                errors.rejectValue("lamp_mode", "lamp_mode has an invalid value (${target.lamp_mode})")
            else {
                // Not validating, just setting up params in the base class from the command values
                if (target.getCameraType() == cameraType.zxtechMCW5B10X.ordinal() && !target.reboot) {
                    target.params = 'cmd=setlampattrex'
                    target.params += "&-lamp_mode=${target.lamp_mode}"
                    target.params2 = "cmd=setimageattr"
                    target.params2 += "&-wdr=${target.wdr}"
                    target.params2 += "&cmd=setoverlayattr&-region=0&cmd=setoverlayattr&-region=1&-show=1"
                    target.params2 += "&-name=${target.cameraName}&-place=0"
                }
            }

            if(!rebootValues.contains(target.reboot))
                errors.rejectValue("reboot", "reboot has an incorrect value (${target.reboot})")
            else {
                // Not validating, just setting up params in the base class from the command value
                if (target.reboot)
                    target.params = 'cmd=sysreboot'
            }
        }
    }
}
