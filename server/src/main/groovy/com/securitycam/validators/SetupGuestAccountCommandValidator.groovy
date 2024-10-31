package com.securitycam.validators

import com.securitycam.commands.SetupGuestAccountCommand
import com.securitycam.dao.UserRepository
import com.securitycam.model.User
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class SetupGuestAccountCommandValidator implements Validator{
    UserRepository userRepository
    SetupGuestAccountCommandValidator(UserRepository repo) {
        this.userRepository = repo
    }

    @Override
    boolean supports(Class<?> clazz) {
        return SetupGuestAccountCommand.class == clazz
    }
    static final Set<Boolean> enabledOptions = [true, false]
    @Override
    void validate(Object target, Errors errors) {
        if(target instanceof SetupGuestAccountCommand) {
            if(!enabledOptions.contains(target.enabled))
                errors.rejectValue("enabled", "Only true or false are valid values for enabled")

            if(target.confirmPassword != target.password)
                errors.rejectValue("confirmPassword", "Passwords do not match")
            else {
                if (target.password == null || target.password == "") { // Empty or blank password means don't change it
                    User u = userRepository.findByUsernameAndCloudAccount("guest", false)
                    if (target.enabled && !u.credentialsNonExpired && (target.password == "" || target.password == null))
                        errors.rejectValue("password", "The password must be set the first time the guest account is enabled")

                    if (target.password != null && target.password != "" && !target.password.matches(/^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,64}$/))
                        errors.rejectValue("password", "Invalid password, must be minimum eight characters, at least one letter, one number and one special character. (must be <= 64 characters)")

                }
            }
        }
    }
}
