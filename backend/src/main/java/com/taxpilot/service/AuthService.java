package com.taxpilot.service;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.taxpilot.domain.user.Role;
import com.taxpilot.domain.user.User;
import com.taxpilot.dto.AuthenticationRequest;
import com.taxpilot.dto.AuthenticationResponse;
import com.taxpilot.dto.RegistrationRequest;
import com.taxpilot.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthenticationResponse register(RegistrationRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(email)
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(request.password()))
                .roles(EnumSet.of(Role.USER, Role.BUSINESS_OWNER))
                .enabled(true)
                .build();

        userRepository.save(user);
        return buildResponse(user);
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );

        if (!authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Authentication failed");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return buildResponse(user);
    }

    public AuthenticationResponse refreshToken(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new IllegalArgumentException("Refresh token is invalid or expired");
        }
        return buildResponse(user);
    }

    private AuthenticationResponse buildResponse(User user) {
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return new AuthenticationResponse(accessToken, refreshToken, user.getId(), user.getFirstName(), user.getLastName(), user.getEmail());
    }
}
