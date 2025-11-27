package com.example.demo.security;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.*;

@Configuration
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;


    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/auth/**", "/error", "/actuator/health").permitAll()
                .anyRequest().authenticated()
        )
        .oauth2Login(oauth2 -> oauth2
            .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
            .defaultSuccessUrl("/auth/post-login", true)
        )
        .logout(logout -> logout
            .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
            .permitAll()
        )
        .csrf(csrf -> csrf.disable());

        return http.build();
    }
}