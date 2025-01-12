package com.securitycam.validators

import com.securitycam.commands.CreateOrUpdateAccountCommand
import com.securitycam.dao.RoleRepository
import com.securitycam.dao.UserRepository
import com.securitycam.model.Role
import com.securitycam.model.User
import com.securitycam.services.UserAdminService
import com.securitycam.services.UtilsService
import org.springframework.validation.Errors

class CreateOrUpdateAccountCommandValidator extends CheckNotGuestCommandValidator {
    final UtilsService utilsService
    final UserRepository userRepository
    final RoleRepository roleRepository

    CreateOrUpdateAccountCommandValidator(UtilsService utilsService, UserAdminService userAdminService, UserRepository userRepository, RoleRepository roleRepository) {
        super(userAdminService)
        this.utilsService = utilsService
        this.userAdminService = userAdminService
        this.userRepository = userRepository
        this.roleRepository = roleRepository
    }


    @Override
    boolean supports(Class<?> clazz) {
        return CreateOrUpdateAccountCommand.class == clazz
    }

    @Override
    void validate(Object target, Errors errors) {
        if (target instanceof CreateOrUpdateAccountCommand) {
            super.validate(target, errors)
            if (target.username == null || target.username == "")
                errors.rejectValue("username", "username must not be null or empty")
            else {
                User user = userRepository.findByUsername(target.username)
                if (user) {
                    Set<Role> roles = user.getRoles()
                    roles.forEach { role ->
                        if (role.name == 'ROLE_CLOUD')
                            errors.rejectValue("username", "Invalid user, only the CLIENT role can use this function")
                    }
                }
                if (!target.updateExisting && userRepository.findByUsernameNotAndCloudAccount('guest', false) != null)
                    errors.rejectValue("username", "There is already a local web account defined")
                else if (!target.username.matches(utilsService.usernameRegex))
                    errors.rejectValue("username", "Format or length of username is incorrect")
            }

            if (target.password == null || target.password == "")
                errors.rejectValue("password", "password must not be null or empty")
            else {
                if (!target.password.matches(utilsService.passwordRegex))
                    errors.rejectValue("password", "password format is invalid")
            }

            if (target.confirmPassword != target.password)
                errors.rejectValue("password", "Password and confirm password do not match")

            if (target.email == null || target.email == "")
                errors.rejectValue("email", "email cannot be null or empty")
            else if (!target.email.matches(utilsService.emailRegex))
                errors.rejectValue("email", "Email format is not valid")

            if (target.confirmEmail != target.email)
                errors.rejectValue("email", "Email and confirm email do not match")
        }
    }
}
