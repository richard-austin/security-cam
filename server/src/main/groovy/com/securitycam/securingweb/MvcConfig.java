package com.securitycam.securingweb;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableMethodSecurity
public class MvcConfig implements WebMvcConfigurer {
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("login");
        registry.addViewController("/recover/forgotPassword").setViewName("forgotPassword");
        registry.addViewController("/recover/resetPasswordForm").setViewName("resetPasswordForm");
        registry.addViewController("/notFound").setViewName("notFound");
    }
}
