package com.securitycam.validators

import com.securitycam.commands.SetUpWifiCommand
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.validation.Errors
import org.springframework.validation.Validator

@Service
class SetupWifiValidator implements Validator{
    @Override
    boolean supports(Class<?> clazz) {
        return SetUpWifiCommand.class.isAssignableFrom(clazz);
    }

    @Override
    void validate(Object target, Errors errors) {
        SetUpWifiCommand suw = (SetUpWifiCommand) target;
        if (!StringUtils.hasLength(suw.getSsid()))
            errors.rejectValue("ssid", "ssid.required");
        else if(suw.getSsid() == null)
            errors.rejectValue('ssid', 'ssid,cannot.be.null');
        if (!StringUtils.hasLength(suw.getPassword()))
            errors.rejectValue("password", "password.required");
        else if (suw.getPassword() == null)
        // Add more validation rules as needed
            errors.rejectValue('password', "password.cannot.be.null")
    }

//    @Override
//    boolean supports(Class<?> clazz) {
//        return false
//    }
//
//    @Override
//    void validate(Object target, Errors errors) {
//
//    }
//

}
