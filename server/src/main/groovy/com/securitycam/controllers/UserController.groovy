package com.securitycam.controllers

import com.securitycam.enums.PassFail
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.services.RestfulCallErrorService
import com.securitycam.services.UserAdminService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/user")
class UserController {
    @Autowired
    UserAdminService userAdminService

    @Autowired
    RestfulCallErrorService restfulCallErrorService

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @RequestMapping("isGuest")
    def isGuest() {
        ObjectCommandResponse result = userAdminService.isGuest()

        if (result.status != PassFail.PASS)
            return restfulCallErrorService.returnError(new Exception(), "isGuest", result.error, "", HttpStatus.INTERNAL_SERVER_ERROR)
        else
           return result.responseObject
    }

    @Secured(['ROLE_CLIENT'])
    @RequestMapping("guestAccountEnabled")
    def guestAccountEnabled() {
        ObjectCommandResponse result = userAdminService.guestAccountEnabled()
        if (result.status != PassFail.PASS)
            return restfulCallErrorService.returnError(new Exception(), "guestAccountEnabled", result.error, "", HttpStatus.INTERNAL_SERVER_ERROR)
        else
            return result.responseObject
    }

}
