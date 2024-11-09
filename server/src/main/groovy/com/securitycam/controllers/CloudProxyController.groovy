package com.securitycam.controllers

import com.securitycam.enums.PassFail
import com.securitycam.error.NVRRestMethodException
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.services.CloudProxyService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/cloudProxy")
class CloudProxyController {
    @Autowired
    CloudProxyService cloudProxyService

    @Secured(['ROLE_CLIENT'])
    @PostMapping("isTransportActive")
    def isTransportActive() {
        ObjectCommandResponse resp =  cloudProxyService.isTransportActive()
        if(resp.status == PassFail.PASS)
            return [transportActive: resp.responseObject]
        else
            throw new NVRRestMethodException(resp.error, "cloudProxy/isTransportActive")
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @PostMapping("/start")
    def start()
    {
        ObjectCommandResponse resp =  cloudProxyService.start()
        if(resp.status == PassFail.PASS)
            return ResponseEntity.ok("")
        else
            throw new NVRRestMethodException(resp.error, "cloudProxy/start")
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @PostMapping("/stop")
    def stop()
    {
        ObjectCommandResponse resp = cloudProxyService.stop()
        if(resp.status == PassFail.PASS)
            return ResponseEntity.ok("")
        else
            throw new NVRRestMethodException(resp.error, "cloudProxy/stop")
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @PostMapping("/restart")
    def restart()
    {
        ObjectCommandResponse resp = cloudProxyService.restart()
        if(resp.status == PassFail.PASS)
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resp.responseObject)
        else
            throw new NVRRestMethodException(resp.error, "cloudProxy/restart")
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @PostMapping("status")
    def status()
    {
        ObjectCommandResponse resp = cloudProxyService.status()
        if(resp.status == PassFail.PASS)
           return resp.responseObject
        else
            throw new NVRRestMethodException(resp.error , "cloudProxy/status")
    }
}
