package com.securitycam.securingweb;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@ConditionalOnProperty(prefix="spring-security", name="enabled", havingValue="true")
@EnableMethodSecurity(
        securedEnabled = true,
        jsr250Enabled = true)
public class MethodSecurityConfig  {
}
