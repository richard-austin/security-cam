package com.securitycam.securingweb

import com.securitycam.dao.RoleRepository
import com.securitycam.dto.UserDto
import com.securitycam.model.Role
import com.securitycam.services.LogService
import com.securitycam.services.Sc_processesService
import com.securitycam.services.UserService
import jakarta.validation.Validation
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class Authentication {

    @Autowired
    RoleRepository roleRepository
    @Autowired
    LogService logService
    @Autowired
    Sc_processesService sc_processesService

    @Bean
    CommandLineRunner run(UserService userService) {
        return (String[] args) -> {
            if(!userService.roleExists('ROLE_CLIENT'))
                userService.addRole('ROLE_CLIENT')

            if(!userService.roleExists('ROLE_CLOUD'))
                userService.addRole('ROLE_CLOUD')

            if(!userService.roleExists('ROLE_GUEST'))
                userService.addRole('ROLE_GUEST')

//            if(!userService.userNameExists('austin')) {
//                ValidatorFactory factory = Validation.buildDefaultValidatorFactory()
//                Validator validator = factory.getValidator()

//                Role role = roleRepository.findByName("ROLE_CLIENT")
//                if(role != null) {
//                    var user = new UserDto(username: "austin", password: "password", matchingPassword: "password", credentialsNonExpired: true, email: "a@b.com", cloudAccount: false, role: role.getId())
//                    Set<ConstraintViolation<UserDto>> violations = validator.validate(user)
//                    userService.registerNewUserAccount(user)
//                }
//            }
            if(!userService.userNameExists('cloud')) {
                Role role = roleRepository.findByName("ROLE_CLOUD")
                if (role != null)
                    userService.registerNewUserAccount(new UserDto(username: "cloud", password: "DrN3yuFAtSsK2w7AtTf66FFRVveBwtjU", credentialsNonExpired: true, header: "7yk=zJu+@77x@MTJG2HD*YLJgvBthkW!",  matchingPassword: "password", email: "a@c.com", cloudAccount: true, role: role.getId()))
            }

            if(!userService.userNameExists('guest')) {
                Role role = roleRepository.findByName("ROLE_GUEST")
                if (role != null)
                    userService.registerNewUserAccount(new UserDto(username: "guest", password: "", matchingPassword: "", credentialsNonExpired: false, email: "a@c.com", cloudAccount: false, header: "", role: role.getId()))
            }
        }
    }
}
