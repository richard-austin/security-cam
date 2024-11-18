package com.securitycam.securingweb;

import com.securitycam.eventlisteners.SecCamSecurityEventListener;
import com.securitycam.security.MyUserDetailsService;
import com.securitycam.security.TwoFactorAuthenticationDetailsSource;
import com.securitycam.security.TwoFactorAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;

@Configuration
public class SecSecurityConfig {
    @Value("${spring-security.enabled}")
     boolean enabled;

    @Autowired
    RememberMeServices rememberMeServices;

    @Autowired
    SecCamSecurityEventListener secCamSecurityEventListener;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        if(enabled) {
            http
                    .csrf(AbstractHttpConfigurer::disable)  // @TODO Makes Restful API calls available to any role, or no role
                    .authorizeHttpRequests((requests) -> requests
                            .requestMatchers("/recover/forgotPassword").anonymous()
                            .requestMatchers("/recover/sendResetPasswordLink").anonymous()
                            .requestMatchers("/recover/resetPasswordForm").anonymous()
                            .requestMatchers("/recover/resetPassword").anonymous()
                    )
                    .authorizeHttpRequests((requests) -> requests
                            .requestMatchers("/stomp").hasAnyRole("CLIENT", "CLOUD", "GUEST")
                            .requestMatchers("/*.css").hasAnyRole("CLIENT", "CLOUD", "GUEST")
                            .requestMatchers("/*.map").hasAnyRole("CLIENT", "CLOUD", "GUEST")
                            .requestMatchers("/*.js").hasAnyRole("CLIENT", "CLOUD", "GUEST")
                            .requestMatchers("/*.ttf").hasAnyRole("CLIENT", "CLOUD", "GUEST")
                            .requestMatchers("/audio").hasAnyRole("CLIENT", "CLOUD")
                            .requestMatchers("/assets/*.woff2").hasAnyRole("CLIENT", "CLOUD", "GUEST")
                            .requestMatchers("/assets/images/*.png").hasAnyRole("CLIENT", "CLOUD", "GUEST")
                            .requestMatchers("/notFound").permitAll()
                            .requestMatchers("/favicon.ico").permitAll()
                            .requestMatchers("/stylesheets/*.css").permitAll()
                            .requestMatchers("/javascripts/*.js").permitAll()
                            .requestMatchers("/user/createOrUpdateAccountLocally").permitAll()
                            .requestMatchers("/user/checkForAccountLocally").permitAll()
                            .requestMatchers("/user/checkForActiveMQCreds").permitAll()
                            .requestMatchers("/user/addOrUpdateActiveMQCreds").permitAll()
                            .requestMatchers("/utils/setupSMTPClientLocally").permitAll()
                            .requestMatchers("/utils/getSMTPClientParamsLocally").permitAll()
                            .anyRequest().authenticated()
                    )

                    .rememberMe(rememberMe -> rememberMe
                            .rememberMeServices(rememberMeServices))
                    .formLogin((form) -> form
                            //      .failureHandler(eventAuthenticationFailureHandler)
                            .authenticationDetailsSource(authenticationDetailsSource())
                            .loginPage("/login/auth")
                            .loginProcessingUrl("/login/authenticate")
                            .defaultSuccessUrl("/", true)
                            .permitAll()
                    )
                    .logout(httpSecurityLogoutConfigurer ->
                            httpSecurityLogoutConfigurer
                                    .logoutUrl("/logout")
                                    .addLogoutHandler(secCamSecurityEventListener)
                                    .permitAll());
        }
        return http.build();
    }

    public TwoFactorAuthenticationDetailsSource authenticationDetailsSource() {
        return new TwoFactorAuthenticationDetailsSource();
    }

    @Bean
    TwoFactorAuthenticationProvider authenticationProvider(MyUserDetailsService userDetailsService) {
        return new TwoFactorAuthenticationProvider(userDetailsService, passwordEncoder());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(11);
    }
}
