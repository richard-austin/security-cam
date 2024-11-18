package com.securitycam.validators

import com.securitycam.commands.ChangeEmailCommand
import com.securitycam.dao.UserRepository
import com.securitycam.model.User
import com.securitycam.security.TwoFactorAuthenticationProvider
import com.securitycam.services.UtilsService
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class ChangeEmailCommandValidator implements Validator {
    ChangeEmailCommandValidator(TwoFactorAuthenticationProvider authenticationManager, UserRepository userRepository) {
        this.authenticationManager = authenticationManager
        this.userRepository = userRepository
    }

    @Override
    boolean supports(Class<?> clazz) {
        return ChangeEmailCommand.class == clazz
    }

    TwoFactorAuthenticationProvider authenticationManager
    UserRepository userRepository

    @Override
    void validate(Object target, Errors errors) {
        String username = ""

        if (target instanceof ChangeEmailCommand) {
            if (target.password == null || target.password == "")
                errors.rejectValue("password", "password must not be null or blank")
            else {
                // Check the old password is correct
                Authentication auth = SecurityContextHolder.getContext().getAuthentication()
                def principal = auth.getPrincipal()
                if (principal) {   // No principal in dev mode
                    username = principal.getUsername()

                    boolean valid = true
                    try {
                        authenticationManager.authenticate new UsernamePasswordAuthenticationToken(username, target.password)
                    }
                    catch (BadCredentialsException ignored) {
                        valid = false
                    }

                    if (!valid /*!passwordEncoder.matches(oldPassword, pw)*/)
                        errors.rejectValue("password", "The password is incorrect")
                } else
                    errors.rejectValue("password", "Could not get principal for this user")
            }

            if (target.newEmail == null || target.newEmail == "")
                errors.rejectValue("newEmail", "newEmail must not be null or blank")

            else if (!target.newEmail.matches(UtilsService.emailRegex))
                errors.rejectValue("newEmail", "Email address is not in the correct format")
            else {
                User u = userRepository.findByEmail(target.newEmail)
                if(u != null && u.username != username)
                    errors.rejectValue("newEmail", "Cannot use this email address")
            }

            if (target.confirmNewEmail != target.newEmail)
                errors.rejectValue("confirmNewEmail", "newEmail and conformEmail must match")
        }
    }
}
