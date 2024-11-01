package com.securitycam

import com.securitycam.dao.RoleRepository
import com.securitycam.dto.UserDto
import com.securitycam.model.Role
import com.securitycam.services.LogService
import com.securitycam.services.Sc_processesService
import com.securitycam.services.UserService
import jakarta.annotation.PreDestroy
import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer
import org.springframework.security.web.util.matcher.AntPathRequestMatcher

import java.util.logging.Level
import java.util.logging.Logger

@SpringBootApplication
class ServerApplication {

    static void main(String[] args) {
        var logger = Logger.getLogger("org.apache")
        logger.setLevel(Level.OFF)
        SpringApplication.run(ServerApplication, args)
    }


//    @Bean
//    ApplicationRunner applicationRunner(OnvifService onvifService/*Environment environment*/) {
//        return (args) -> {
//            try {
//            }
//            catch (Exception ex) {
//                logService.cam.error("${ex.getClass()} when starting services: ${ex.getMessage()}")
//            }
//            //System.out.println("message from application.properties " + environment.getProperty("spring.jpa.properties.hibernate.globally_quoted_identifiers"))
//            logService.cam.info("Started NVR services")
//        }
//    }

    @PreDestroy
    void onExit() {
        try {
            sc_processesService.stopProcesses()
            logService.cam.info("NVR Services have been shut down")
        } catch (Exception ex) {
            logService.cam.error("${ex.getClass()} when shutting down services: ${ex.getMessage()}")
        }
    }
}


