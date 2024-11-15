package com.securitycam

import com.securitycam.configuration.Config
import com.securitycam.proxies.CloudProxyProperties
import com.securitycam.services.LogService
import com.securitycam.services.Sc_processesService
import jakarta.annotation.PreDestroy
import jakarta.servlet.ServletContext
import jakarta.servlet.ServletException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.servlet.ServletContextInitializer
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.security.authentication.AuthenticationEventPublisher
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.RememberMeServices
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices

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
    AuthenticationEventPublisher authenticationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
    }

    @Bean
    RememberMeServices rememberMeServices(UserDetailsService userDetailsService) {
        TokenBasedRememberMeServices.RememberMeTokenAlgorithm encodingAlgorithm = TokenBasedRememberMeServices.RememberMeTokenAlgorithm.SHA256;
        TokenBasedRememberMeServices rememberMe = new TokenBasedRememberMeServices("supersecret", userDetailsService, encodingAlgorithm);
        rememberMe.setMatchingAlgorithm(TokenBasedRememberMeServices.RememberMeTokenAlgorithm.MD5);
        return rememberMe;
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


