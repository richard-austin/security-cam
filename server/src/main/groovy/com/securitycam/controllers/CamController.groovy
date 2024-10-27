package com.securitycam.controllers

import com.securitycam.enums.PassFail
import com.securitycam.error.NVRRestMethodException
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.services.CamService
import com.securitycam.services.LogService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
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
        if (cameras.status != PassFail.PASS)
            throw new NVRRestMethodException(cameras.error, "cam/getCameras", "See logs") //render(status: 500, text: cameras.error)
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
            return response.responseObject
        else
            throw new NVRRestMethodException(response.error, "cam/getPublicKey", "See logs")
    }
}
