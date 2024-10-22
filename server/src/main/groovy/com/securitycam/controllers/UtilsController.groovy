package com.securitycam.controllers

import com.securitycam.commands.StartAudioOutCommand
import com.securitycam.enums.PassFail
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.services.LogService
import com.securitycam.services.RestfulCallErrorService
import com.securitycam.services.UtilsService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.security.access.annotation.Secured
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

import javax.management.BadAttributeValueExpException

@RestController
@RequestMapping("/utils")
class UtilsController {
    @Autowired
    UtilsService utilsService

    @Autowired
    LogService logService

    @Autowired
    RestfulCallErrorService restfulCallErrorService
    /**
     * getTemperature: Get the core temperature (Raspberry pi only). This is called at intervals to keep the session alive
     * @return: The temperature as a string. On non Raspberry pi systems an error is returned.
     */
    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @RequestMapping("getTemperature")
    def getTemperature() {
        ObjectCommandResponse response = utilsService.getTemperature()

        if (response.status != PassFail.PASS)
            return restfulCallErrorService.returnError(new Exception(), "utils/getTemperature", response.error, "", HttpStatus.INTERNAL_SERVER_ERROR)
        else
            return response.responseObject
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @RequestMapping("audioInUse")
    def audioInUse() {
        return [audioInUse: utilsService.getAudioInUse()]
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @RequestMapping("startAudioOut")
    def startAudioOut(@Valid @RequestBody StartAudioOutCommand cmd) {
        ObjectCommandResponse response = utilsService.startAudioOut(cmd)

        if (response.status != PassFail.PASS)
            return restfulCallErrorService.returnError(new Exception(), "utils/startAudioOut", response.error, "", HttpStatus.INTERNAL_SERVER_ERROR)
        else
            return ResponseEntity.ok(response.responseObject)
    }

    @MessageMapping(value = "/audio")
    protected def audio(@Payload byte[] data) {
        utilsService.audio(data)
    }


//    // Exception handler for invalid method arguments
//    @ResponseStatus(HttpStatus.BAD_REQUEST)
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    Map<String, String> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
//        Map<String, String> errors = new HashMap<>();
//        ex.getBindingResult().getAllErrors().forEach((error) -> {
//            String fieldName = ((FieldError) error).getField();
//            String errorMessage = error.getDefaultMessage();
//            errors.put(fieldName, errorMessage);
//        });
//        return errors;
//    }

}
