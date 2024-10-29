package com.securitycam.validators

import com.securitycam.commands.UpdateCamerasCommand
import org.springframework.boot.configurationprocessor.json.JSONArray
import org.springframework.boot.configurationprocessor.json.JSONException
import org.springframework.boot.configurationprocessor.json.JSONObject
import org.springframework.validation.Errors
import org.springframework.validation.Validator


class UpdateCamerasCommandValidator implements Validator{
    @Override
    boolean supports(Class<?> clazz) {
        return UpdateCamerasCommand.class == clazz
    }

    @Override
    void validate(Object target, Errors errors) {
        if(target instanceof UpdateCamerasCommand) {
            try {
                new JSONObject(target.camerasJSON)
            } catch (JSONException ignore) {
                // edited, to include @Arthur's comment
                // e.g. in case JSONArray is valid as well...
                try {
                    new JSONArray(target.camerasJSON)
                } catch (JSONException ignored) {
                    errors.rejectValue("camerasJSON", 'The configuration data is not valid JSON')
                }
            }
        }
    }
}
