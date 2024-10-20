package com.securitycam.controllers

import com.securitycam.enums.PassFail
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.services.CamService
import com.securitycam.services.LogService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/cam")
class CamController {
    @Autowired
    LogService logService

    @Autowired
    CamService camService

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @RequestMapping('/getCameras')
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

   @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @RequestMapping('/getPublicKey')
    def getPublicKey() {
        ObjectCommandResponse response = camService.getPublicKey()
        if(response.status == PassFail.PASS)
            return response.responseObject as byte[]
        else
            throw new Exception(response.error)
    }


}
