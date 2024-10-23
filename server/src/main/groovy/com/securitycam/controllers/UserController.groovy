package com.securitycam.controllers

import com.securitycam.enums.PassFail
import com.securitycam.error.NVRRestMethodException
import com.securitycam.interfaceobjects.ObjectCommandResponse

import com.securitycam.services.UserAdminService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/user")
class UserController {
    @Autowired
    UserAdminService userAdminService

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @RequestMapping("isGuest")
    def isGuest() {
        ObjectCommandResponse result = userAdminService.isGuest()

        if (result.status != PassFail.PASS)
            throw new NVRRestMethodException(result.error, "user/isGuest", "See logs")
        else
           return ResponseEntity.ok(result.responseObject)
    }

    @Secured(['ROLE_CLIENT'])
    @RequestMapping("guestAccountEnabled")
    def guestAccountEnabled() {
        ObjectCommandResponse result = userAdminService.guestAccountEnabled()
        if (result.status != PassFail.PASS)
            throw new NVRRestMethodException(result.error, "user/guestAccountEnabled", "See logs")
        else
            return  ResponseEntity.ok(result.responseObject)
    }

}
