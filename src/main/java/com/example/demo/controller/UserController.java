package com.example.demo.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public User getCurrentUser(@AuthenticationPrincipal Object principal) {

        if (principal instanceof DefaultOAuth2User oauthUser) {
            String email = oauthUser.getAttribute("email");
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no registrado"));
        }

        throw new RuntimeException("No hay usuario autenticado");
    }
}