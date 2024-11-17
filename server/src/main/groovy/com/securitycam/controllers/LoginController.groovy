package com.securitycam.controllers

import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class LoginController {
    @GetMapping("/login/auth")
    def login() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication()
        if (!auth || auth.authorities[0] == new SimpleGrantedAuthority('ROLE_ANONYMOUS'))
            return  'login'
        else
            return 'redirect:/'
    }
}
