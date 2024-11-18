package com.securitycam.validators

import com.securitycam.commands.ResetPasswordCommand
import com.securitycam.dao.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class ResetPasswordCommandValidator extends UserPasswordValidator implements Validator {
    ResetPasswordCommandValidator(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        super(userRepository, passwordEncoder)
        this.userRepository = userRepository
    }
    UserRepository userRepository

    @Override
    boolean supports(Class<?> clazz) {
        return ResetPasswordCommand.class == clazz
    }

    @Override
    void validate(Object target, Errors errors) {
        if (target instanceof ResetPasswordCommand) {
            if (target.oldPassword == null || target.oldPassword == "")
                errors.rejectValue("oldPassword", "oldPassword cannot be null or blank")
            else if (!isPasswordValid(target.oldPassword))
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
