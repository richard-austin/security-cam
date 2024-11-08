package com.securitycam.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class NotFoundController {
    @RequestMapping("/notFound")
    def notFound() {
        return "notFound"
    }
}
