package com.securitycam.controllers

import com.securitycam.enums.PassFail
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.services.CamService
import com.securitycam.services.LogService
import org.apache.coyote.BadRequestException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

import javax.management.BadAttributeValueExpException

@RestController
class CamController {
    @Autowired
    LogService logService

    @Autowired
    CamService camService

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @GetMapping('/cam/getCameras')
    def getCameras() {
        ObjectCommandResponse cameras = camService.getCameras()
//        throw new ResponseStatusException(
//                HttpStatus.BAD_REQUEST, "entity not found"
//        )
//        throw new BadAttributeValueExpException("entity not found")
        if (cameras.status != PassFail.PASS)
            throw new Exception(cameras.error) //render(status: 500, text: cameras.error)
        else {
            logService.cam.info("getCameras: success")
            return cameras.responseObject
        }
    }
}
