package com.example.demo.security;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    private final OidcUserService oidcUserService = new OidcUserService();
    private final DefaultOAuth2UserService defaultOAuth2UserService = new DefaultOAuth2UserService();

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        OAuth2User oauth2User;

        // 1. Si viene como OIDC
        if (userRequest instanceof org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest oidcReq) {
            oauth2User = oidcUserService.loadUser(oidcReq);
        } 
        // 2. Proveedores OAuth2 simples (GitHub, Facebook, etc.)
        else {
            oauth2User = defaultOAuth2UserService.loadUser(userRequest);
        }

        // 3. Extraer datos
        Map<String, Object> attributes = oauth2User.getAttributes();

        String email = (String) attributes.get("email");
        if (email == null)
            throw new OAuth2AuthenticationException("Provider did not return email.");

        String name = (String) attributes.getOrDefault("name", email);
        String picture = (String) attributes.getOrDefault("picture", null);

        // 4. Registrar o actualizar usuario
        User user = userRepository.findByEmail(email)
                .map(u -> {
                    u.setName(name);
                    u.setPicture(picture);
                    u.setProvider(userRequest.getClientRegistration().getRegistrationId());
                    return userRepository.save(u);
                })
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(email)
                                .name(name)
                                .picture(picture)
                                .provider(userRequest.getClientRegistration().getRegistrationId())
                                .roles("USER")
                                .build()
                ));

        return oauth2User;
    }
}
