package com.taxpilot.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taxpilot.domain.user.Role;
import com.taxpilot.domain.user.User;
import com.taxpilot.repository.UserRepository;

@Service
public class AdminPasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int PASSWORD_BYTES = 32;
    private static final String ADMIN_EMAIL = "admin@taxpilot.com";
    private static final String GENERATED_EMAIL_PREFIX = "system-admin-";
    private static final String GENERATED_EMAIL_SUFFIX = "@taxpilot.local";

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminPasswordResetService(
            JdbcTemplate jdbcTemplate, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Optional<ResetCredentials> resetOnce(UUID requestId) {
        if (!claimRequest(requestId)) {
            return Optional.empty();
        }

        User admin = findAdminToReset();

        String password = generatePassword();
        admin.setEmail(ADMIN_EMAIL);
        admin.setPasswordHash(passwordEncoder.encode(password));
        userRepository.saveAndFlush(admin);
        return Optional.of(new ResetCredentials(ADMIN_EMAIL, password));
    }

    @Transactional
    public boolean changePasswordOnce(UUID requestId, String currentPassword, String newPassword) {
        if (!claimRequest(requestId)) {
            return false;
        }

        User admin = userRepository.findByEmailIgnoreCase(ADMIN_EMAIL)
                .filter(this::isAdmin)
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot change admin password: no admin@taxpilot.com administrator was found."));
        if (!passwordEncoder.matches(currentPassword, admin.getPasswordHash())) {
            throw new IllegalArgumentException("Current admin password did not match; no change was made.");
        }

        admin.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.saveAndFlush(admin);
        return true;
    }

    private boolean claimRequest(UUID requestId) {
        return jdbcTemplate.update(
                "INSERT INTO admin_password_reset_requests (request_id) VALUES (?) ON CONFLICT DO NOTHING",
                requestId) == 1;
    }

    private User findAdminToReset() {
        Optional<User> target = userRepository.findByEmailIgnoreCase(ADMIN_EMAIL);
        if (target.isPresent()) {
            return target.filter(this::isAdmin)
                    .orElseThrow(() -> new IllegalStateException(
                            "Cannot reset admin password: admin@taxpilot.com belongs to a non-admin account."));
        }

        List<User> generatedAdmins = userRepository.findByEmailStartingWith(GENERATED_EMAIL_PREFIX).stream()
                .filter(user -> user.getEmail().toLowerCase(Locale.ROOT).endsWith(GENERATED_EMAIL_SUFFIX))
                .filter(this::isAdmin)
                .toList();
        if (generatedAdmins.size() != 1) {
            throw new IllegalStateException(generatedAdmins.isEmpty()
                    ? "Cannot reset admin password: no admin@taxpilot.com or generated system admin was found."
                    : "Cannot reset admin password: multiple generated system admins were found.");
        }
        return generatedAdmins.get(0);
    }

    private boolean isAdmin(User user) {
        return user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.SUPER_ADMIN);
    }

    private String generatePassword() {
        byte[] randomBytes = new byte[PASSWORD_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public record ResetCredentials(String email, String password) {
    }
}
