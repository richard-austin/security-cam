package com.securitycam.validators

import com.securitycam.commands.ResetPasswordCommand
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class ResetPasswordCommandValidator implements Validator {
    ResetPasswordCommandValidator(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager
    }

    AuthenticationManager authenticationManager

    @Override
    boolean supports(Class<?> clazz) {
        return ResetPasswordCommand.class == clazz
    }

    @Override
    void validate(Object target, Errors errors) {
        if (target instanceof ResetPasswordCommand) {
            if (target.oldPassword == null || target.oldPassword == "")
                errors.rejectValue("oldPassword", "oldPassword cannot be null or blank")
            else {
                // Check the old password is correct
                Authentication auth = SecurityContextHolder.getContext().getAuthentication()
                def principal = auth.getPrincipal()
                String userName = principal.getUsername()

                boolean valid = true
                try {
                    authenticationManager.authenticate new UsernamePasswordAuthenticationToken(userName, target.oldPassword)
                }
                catch (BadCredentialsException ignore) {
                    valid = false
                }
                if (!valid)
                    errors.rejectValue("oldPassword", "The old password given is incorrect")
            }

            if (target.newPassword == null || target.newPassword == "")
                errors.rejectValue("newPassword", "newPassword cannot be null or blank")
            else if (!target.newPassword.matches(/^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,64}$/))
                errors.rejectValue("newPassword", "Invalid password, must be minimum eight characters, at least one letter, one number and one special character. (must be <= 64 characters)")

            if (target.confirmNewPassword != target.newPassword)
                errors.rejectValue("confirmNewPassword", "confirmNewPasswords does not match newPassword")
        }
    }
}
