package com.securitycam.validators

import com.securitycam.dao.UserRepository
import com.securitycam.model.User
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder

class UserPasswordValidator {
    UserRepository userRepository
    PasswordEncoder passwordEncoder

    UserPasswordValidator(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository
        this.passwordEncoder = passwordEncoder
    }

    boolean isPasswordValid(String pw) {
        // Check the password is correct
        Authentication auth = SecurityContextHolder.getContext().getAuthentication()
        def principal = auth.getPrincipal()
        String userName = principal.getUsername()
        User u = userRepository.findByUsername(userName)
        return passwordEncoder.matches(pw, u.password)
    }
}
