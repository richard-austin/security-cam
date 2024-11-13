package com.securitycam

import com.securitycam.configuration.Config
import com.securitycam.dao.RoleRepository
import com.securitycam.dto.UserDto
import com.securitycam.model.Role
import com.securitycam.proxies.CloudProxyProperties
import com.securitycam.services.LogService
import com.securitycam.services.Sc_processesService
import com.securitycam.services.UserService
import jakarta.annotation.PreDestroy
import jakarta.servlet.ServletContext
import jakarta.servlet.ServletException
import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.servlet.ServletContextInitializer
import org.springframework.context.annotation.Bean
import org.springframework.session.web.http.CookieSerializer
import org.springframework.session.web.http.DefaultCookieSerializer

import java.util.logging.Level
import java.util.logging.Logger

@SpringBootApplication
class ServerApplication implements ServletContextInitializer{

    static void main(String[] args) {
        var logger = Logger.getLogger("org.apache")
        logger.setLevel(Level.OFF)
        SpringApplication.run(ServerApplication, args)
    }

    @Autowired
    LogService logService

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

    @Autowired
    Sc_processesService sc_processesService

    @PreDestroy
    void onExit() {
        try {
            sc_processesService.stopProcesses()
            logService.cam.info("NVR Services have been shut down")
        } catch (Exception ex) {
            logService.cam.error("${ex.getClass()} when shutting down services: ${ex.getMessage()}")
        }
    }


    @Bean
    CloudProxyProperties cloudProxyProperties(Config config, LogService logService) {
        return new CloudProxyProperties(config, logService)
    }

    @Override
    void onStartup(ServletContext servletContext) throws ServletException {
        servletContext.getSessionCookieConfig().setName("NVRSESSIONID")
    }
}


