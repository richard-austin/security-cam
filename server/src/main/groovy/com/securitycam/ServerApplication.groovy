package com.securitycam

import com.securitycam.dao.RoleRepository
import com.securitycam.dto.UserDto
import com.securitycam.model.Role
import com.securitycam.services.UserService
import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment

@SpringBootApplication
class ServerApplication {

    static void main(String[] args) {
        SpringApplication.run(ServerApplication, args)
    }

    @Autowired
    RoleRepository roleRepository

    @Bean
    @ConditionalOnProperty(prefix="spring-security", name="enabled", havingValue="true")
    CommandLineRunner run(UserService userService) {
        return (String[] args) -> {
            if(!userService.roleExists('ROLE_CLIENT'))
                userService.addRole('ROLE_CLIENT')

            if(!userService.roleExists('ROLE_CLOUD'))
                userService.addRole('ROLE_CLOUD')

            if(!userService.userNameExists('austin')) {
                ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
                Validator validator = factory.getValidator();

                Role role = roleRepository.findByName("ROLE_CLIENT")
                if(role != null) {
                    var user = new UserDto(username: "austin", password: "password", matchingPassword: "password", email: "a@b.com", cloudAccount: false, role: role.getId())
                    Set<ConstraintViolation<UserDto>> violations = validator.validate(user);
                    userService.registerNewUserAccount(user)
                }
            }
            if(!userService.userNameExists('cloud')) {
                Role role = roleRepository.findByName("ROLE_CLIENT")
                if (role != null)
                    userService.registerNewUserAccount(new UserDto(username: "cloud", password: "password", matchingPassword: "password", email: "a@c.com", cloudAccount: true, header: "123123123", role: role.getId()))
            }
        }
    }

    @Bean
    ApplicationRunner applicationRunner(Environment environment) {
        return (args) -> {
            System.out.println("message from application.properties " + environment.getProperty("spring.jpa.properties.hibernate.globally_quoted_identifiers"))
        }
    }

}
