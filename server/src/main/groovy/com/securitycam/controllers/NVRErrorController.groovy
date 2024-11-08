package com.securitycam.controllers

import jakarta.servlet.RequestDispatcher
import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class NVRErrorController implements ErrorController  {

    @RequestMapping("/error")
    String handleError(Model model, HttpServletRequest request) {
        def status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)
        def message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE)
        def requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI)

        if(status == null)
            return null
        model.addAttribute("status", status)
        model.addAttribute("message", message)
        model.addAttribute("requestUri", requestUri)
        return "error"
    }
}
