package com.securitycam.controllers

import com.securitycam.commands.MoveCommand
import com.securitycam.commands.PTZPresetsCommand
import com.securitycam.commands.PtzCommand
import com.securitycam.enums.PassFail
import com.securitycam.error.NVRRestMethodException
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.services.LogService
import com.securitycam.services.OnvifService
import com.securitycam.validators.BadRequestResult
import com.securitycam.validators.GeneralValidator
import com.securitycam.validators.MoveCommandValidator
import com.securitycam.validators.PtzCommandValidator
import com.securitycam.validators.PtzPresetsCommandValidator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/ptz")
class PtzController {
    @Autowired
    OnvifService onvifService
    @Autowired
    LogService logService

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @PostMapping("/move")
    def move(@RequestBody MoveCommand cmd)
    {
        GeneralValidator gv = new GeneralValidator(cmd,  new MoveCommandValidator(logService))
        BindingResult results = gv.validate()
        if(results.hasErrors())
        {
            logService.cam.error "/ptz/move: Validation error: " + results.toString()
            def retVal = new BadRequestResult(results)
            return ResponseEntity<BadRequestResult>
                    .badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(retVal)
        }
        else {
            ObjectCommandResponse response = onvifService.move(cmd)
            if(response.status != PassFail.PASS) {
                logService.cam.error("Error in ptz/move: ${response.error}")
                throw new NVRRestMethodException(response.error, "ptz/move", "")
            }
            else
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body([message: 'move successful'])
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @PostMapping("/stop")
    def stop(@RequestBody PtzCommand cmd)
    {
        GeneralValidator gv = new GeneralValidator(cmd,  new PtzCommandValidator(logService))
        BindingResult results = gv.validate()
        if(results.hasErrors())
        {
            logService.cam.error "/ptz/stop: Validation error: " + results.toString()
            def retVal = new BadRequestResult(results)
            return ResponseEntity<BadRequestResult>
                    .badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(retVal)
        }
        else {
            ObjectCommandResponse response = onvifService.stop(cmd)
            if(response.status != PassFail.PASS) {
                logService.cam.error("Error in ptz/move: ${response.error}")
                throw new NVRRestMethodException(response.error, "ptz/stop", "")
            }
            else
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body([message: 'stop successful'])
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @PostMapping("/preset")
    def preset(@RequestBody PTZPresetsCommand cmd)
    {
        GeneralValidator gv = new GeneralValidator(cmd,  new PtzPresetsCommandValidator(logService))
        BindingResult results = gv.validate()
        if(results.hasErrors())
        {
            logService.cam.error "/ptz/preset: Validation error: " + results.toString()
            BadRequestResult retVal = new BadRequestResult(results)
            return ResponseEntity
                    .badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(retVal)
        }
        else{
            ObjectCommandResponse response = onvifService.preset(cmd)
            if(response.status != PassFail.PASS) {
                logService.cam.error("Error in ptz/preset: ${response.error}")
                throw new NVRRestMethodException(response.error, "ptz/preset", "")
            }
            else
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body([message: 'preset successful'])
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @PostMapping("/ptzPresetsInfo")
    ptzPresetsInfo(@RequestBody PtzCommand cmd)
    {
        GeneralValidator gv = new GeneralValidator(cmd,  new PtzCommandValidator(logService))
        BindingResult results = gv.validate()
        if(results.hasErrors())
        {
            logService.cam.error "/ptz/ptzPresetsInfo: Validation error: " + results.toString()
            def retVal = new BadRequestResult(results)
            return ResponseEntity<BadRequestResult>
                    .badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(retVal)
        }
        else{
            ObjectCommandResponse response = onvifService.ptzPresetsInfo(cmd)
            if(response.status != PassFail.PASS) {
                logService.cam.error("Error in ptz/preset: ${response.error}")
                throw new NVRRestMethodException(response.error, "ptz/ptzPresetsInfo", "")
            }
            else
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response.responseObject)
        }
    }
}
