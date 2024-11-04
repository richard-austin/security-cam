package com.securitycam.securingweb;

import com.securitycam.security.MyUserDetailsService;
import com.securitycam.security.TwoFactorAuthenticationDetailsSource;
import com.securitycam.security.TwoFactorAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

@Configuration
public class SecSecurityConfig {
    @Value("${spring-security.enabled}")
     boolean enabled;
    @Autowired
    private MyUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        if(enabled) {
            http
                    .csrf(AbstractHttpConfigurer::disable)  // @TODO Makes Restful API calls available to any role, or no role
                    .authorizeHttpRequests((requests) -> requests
                            .requestMatchers("/", "/login/authenticate").permitAll()
                            .requestMatchers("/stomp").hasAnyRole("CLIENT", "CLOUD", "GUEST")
                            .requestMatchers("/*.css").hasAnyRole("CLIENT", "CLOUD", "GUEST")
                            .requestMatchers("/*.js").hasAnyRole("CLIENT", "CLOUD", "GUEST")
                            .requestMatchers("/*.ttf").hasAnyRole("CLIENT", "CLOUD", "GUEST")
                            .requestMatchers("/audio").hasAnyRole("CLIENT", "CLOUD")
                            .requestMatchers("/assets/*.woff2").hasAnyRole("CLIENT", "CLOUD", "GUEST")
                            .requestMatchers("/assets/images/*.png").hasAnyRole("CLIENT", "CLOUD", "GUEST")
                            .requestMatchers("/user/createOrUpdateAccountLocally").permitAll()
                            .requestMatchers("/user/checkForAccountLocally").permitAll()
                            .requestMatchers("/user/checkForActiveMQCreds").permitAll()
                            .requestMatchers("/user/addOrUpdateActiveMQCreds").permitAll()
                            .requestMatchers("/utils/setupSMTPClientLocally").permitAll()
                            .requestMatchers("/utils/getSMTPClientParamsLocally").permitAll()
                            .requestMatchers("/recover/sendResetPasswordLink").permitAll()
                            .requestMatchers("/recover/forgotPassword").permitAll()
                            .requestMatchers("/recover/resetPasswordForm").permitAll()
                            .requestMatchers("/recover/resetPassword").permitAll()
                            .anyRequest().authenticated()
                    )
                    .rememberMe(rememberMe -> rememberMe.key("uniqueAndSecret"))
                    .formLogin((form) -> form
                            .authenticationDetailsSource(authenticationDetailsSource())
                            .loginPage("/login")
                            .permitAll()
                    )
                    .logout(LogoutConfigurer::permitAll);
        }
        return http.build();
    }

    @Bean
    public TwoFactorAuthenticationProvider twoFactorAuthenticationProvider() {
        final TwoFactorAuthenticationProvider authProvider = new TwoFactorAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        //      authProvider.setPostAuthenticationChecks(differentLocationChecker());
        return authProvider;

    }

    @Bean
    public TwoFactorAuthenticationDetailsSource authenticationDetailsSource() {
        return new TwoFactorAuthenticationDetailsSource();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(11);
    }

    @Bean
    RememberMeServices rememberMeServices(UserDetailsService userDetailsService) {
        TokenBasedRememberMeServices.RememberMeTokenAlgorithm encodingAlgorithm = TokenBasedRememberMeServices.RememberMeTokenAlgorithm.SHA256;
        TokenBasedRememberMeServices rememberMe = new TokenBasedRememberMeServices("supersecret", userDetailsService, encodingAlgorithm);
        rememberMe.setMatchingAlgorithm(TokenBasedRememberMeServices.RememberMeTokenAlgorithm.MD5);
        return rememberMe;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(authenticationProvider);
    }
}
