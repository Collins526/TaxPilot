package com.taxpilot.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

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

        String email = createUniqueAdminEmail();
        String password = createPassword();

        User admin = User.builder()
                .firstName("System")
                .lastName("Administrator")
                .email(email)
                .phone("Not provided")
                .passwordHash(passwordEncoder.encode(password))
                .roles(EnumSet.of(Role.ADMIN))
                .enabled(true)
                .build();
        userRepository.saveAndFlush(admin);
        return Optional.of(new GeneratedCredentials(email, password));
    }

    private String createUniqueAdminEmail() {
        String email;
        do {
            email = "system-admin-" + UUID.randomUUID().toString().replace("-", "") + "@taxpilot.local";
        } while (userRepository.findByEmailIgnoreCase(email).isPresent());
        return email.toLowerCase(Locale.ROOT);
    }

    private String createPassword() {
        byte[] randomBytes = new byte[PASSWORD_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public record GeneratedCredentials(String email, String password) {
    }
}
