package com.securitycam.securingweb

import com.securitycam.services.LogService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer
import org.springframework.security.web.util.matcher.AntPathRequestMatcher

@Configuration
@ConfigurationProperties(prefix = "spring-security")
class ApplicationNoSecurity {
    @Autowired
    LogService logService
    boolean enabled  // Set in application(-dev).properties
    @Bean
    WebSecurityCustomizer webSecurityCustomizer() {
        if(!enabled) {
            logService.cam.info("============================================")
            logService.cam.info("Spring Security is DISABLED!!!")
            logService.cam.info("============================================")
            return (web) ->
                    web.ignoring().requestMatchers("/**")
        }
        else {
            logService.cam.info("++++++++++++++++++++++++++++++++++++++++++++")
            logService.cam.info("Spring Security is enabled")
            logService.cam.info("++++++++++++++++++++++++++++++++++++++++++++")
            return (web) -> web.ignoring()
        }
    }
}
