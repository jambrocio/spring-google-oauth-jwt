package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.model.RefreshToken;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.security.JwtService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthController(UserRepository userRepository, JwtService jwtService, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @GetMapping("/post-login")
    public ResponseEntity<?> postLogin(@AuthenticationPrincipal OidcUser oidcUser) {

        if (oidcUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No authenticated user"));
        }

        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();
        String picture = oidcUser.getPicture();

        if (email == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Google did not provide email"));
        }

        // Buscar o crear
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name != null ? name : "Google User");
            newUser.setPicture(picture);
            return userRepository.save(newUser);
        });

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = jwtService.generateAndStoreRefreshToken(user);

        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken.getToken(),
                "user", Map.of(
                        "email", user.getEmail(),
                        "name", user.getName(),
                        "picture", user.getPicture()
                )
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");

        return refreshTokenRepository.findByToken(refreshToken)
                .map(rt -> {
                    if (rt.getExpiryDate().isBefore(Instant.now())) {
                        refreshTokenRepository.delete(rt);
                        return ResponseEntity.status(401).body(Map.of("error", "refresh_expired"));
                    }

                    User user = rt.getUser();
                    String newAccess = jwtService.generateAccessToken(user);
                    return ResponseEntity.ok(Map.of("accessToken", newAccess));
                })
                .orElse(ResponseEntity.status(401).body(Map.of("error", "invalid_refresh")));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");

        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);

        return ResponseEntity.ok(Map.of("ok", true));
    }
}
