package com.taxpilot.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taxpilot.domain.user.Role;
import com.taxpilot.domain.user.User;
import com.taxpilot.repository.UserRepository;

@Service
public class AdminBootstrapService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int PASSWORD_BYTES = 32;
    private static final String ADMIN_EMAIL = "admin@taxpilot.com";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Optional<GeneratedCredentials> createIfMissing() {
        if (userRepository.existsByRolesContainingOrRolesContaining(Role.ADMIN, Role.SUPER_ADMIN)) {
            return Optional.empty();
        }

        if (userRepository.findByEmailIgnoreCase(ADMIN_EMAIL).isPresent()) {
            throw new IllegalStateException(
                    "Cannot create the system administrator: admin@taxpilot.com belongs to a non-admin account.");
        }
        String password = createPassword();

        User admin = User.builder()
                .firstName("System")
                .lastName("Administrator")
                .email(ADMIN_EMAIL)
                .phone("Not provided")
                .passwordHash(passwordEncoder.encode(password))
                .roles(EnumSet.of(Role.ADMIN))
                .enabled(true)
                .build();
        userRepository.saveAndFlush(admin);
        return Optional.of(new GeneratedCredentials(ADMIN_EMAIL, password));
    }

    private String createPassword() {
        byte[] randomBytes = new byte[PASSWORD_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public record GeneratedCredentials(String email, String password) {
    }
}
