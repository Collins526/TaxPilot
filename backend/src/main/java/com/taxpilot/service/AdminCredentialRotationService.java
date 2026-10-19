package com.taxpilot.service;

import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taxpilot.domain.user.Role;
import com.taxpilot.domain.user.User;
import com.taxpilot.repository.UserRepository;

@Service
public class AdminCredentialRotationService {

    private static final String GENERATED_EMAIL_PREFIX = "system-admin-";
    private static final String GENERATED_EMAIL_SUFFIX = "@taxpilot.local";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminCredentialRotationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public boolean rotateGeneratedAdmin(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User existingTarget = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (existingTarget != null) {
            if (isAdmin(existingTarget) && passwordEncoder.matches(password, existingTarget.getPasswordHash())) {
                return false;
            }
            throw new IllegalStateException(
                    "The requested admin email already belongs to an account that cannot be safely changed.");
        }

        List<User> candidates = userRepository.findByEmailStartingWith(GENERATED_EMAIL_PREFIX).stream()
                .filter(user -> user.getEmail().toLowerCase(Locale.ROOT).endsWith(GENERATED_EMAIL_SUFFIX))
                .filter(this::isAdmin)
                .toList();
        if (candidates.size() != 1) {
            throw new IllegalStateException(candidates.isEmpty()
                    ? "No generated system administrator was found to rotate."
                    : "Multiple generated system administrator accounts were found; refusing an ambiguous rotation.");
        }

        User admin = candidates.get(0);
        admin.setEmail(normalizedEmail);
        admin.setPasswordHash(passwordEncoder.encode(password));
        userRepository.saveAndFlush(admin);
        return true;
    }

    private boolean isAdmin(User user) {
        return user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.SUPER_ADMIN);
    }
}
