package com.securitycam.validators

import com.securitycam.commands.UpdateAdHocDeviceListCommand
import org.springframework.boot.configurationprocessor.json.JSONArray
import org.springframework.boot.configurationprocessor.json.JSONException
import org.springframework.boot.configurationprocessor.json.JSONObject
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class UpdateAdHocDeviceListCommandValidator implements Validator{
    @Override
    boolean supports(Class<?> clazz) {
        return UpdateAdHocDeviceListCommand.class == clazz
    }

    @Override
    void validate(Object target, Errors errors) {
        if(target instanceof UpdateAdHocDeviceListCommand) {
            try {
                new JSONObject(target.adHocDeviceListJSON)
            } catch (JSONException ignore) {
               try {
                    new JSONArray(target.adHocDeviceListJSON)
                } catch (JSONException ignored) {
                    errors.rejectValue("adHocDeviceListJSON", 'The configuration data is not valid JSON')
                }
            }
        }
    }
}
