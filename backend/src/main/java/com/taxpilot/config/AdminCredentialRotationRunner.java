package com.taxpilot.config;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.taxpilot.service.AdminCredentialRotationService;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AdminCredentialRotationRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminCredentialRotationRunner.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;

    private final boolean enabled;
    private final String email;
    private final String password;
    private final AdminCredentialRotationService rotationService;

    public AdminCredentialRotationRunner(
            @Value("${app.admin-credential-rotation.enabled:false}") boolean enabled,
            @Value("${app.admin-credential-rotation.email:}") String email,
            @Value("${app.admin-credential-rotation.password:}") String password,
            AdminCredentialRotationService rotationService) {
        this.enabled = enabled;
        this.email = email;
        this.password = password;
        this.rotationService = rotationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        validateConfiguration();
        if (rotationService.rotateGeneratedAdmin(email, password)) {
            LOGGER.info("Generated system administrator credentials updated to {}",
                    email.trim().toLowerCase(Locale.ROOT));
        } else {
            LOGGER.info("System administrator credentials already match the configured rotation; no update needed");
        }
    }

    private void validateConfiguration() {
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "Admin credential rotation is enabled but ADMIN_CREDENTIAL_ROTATION_EMAIL or "
                            + "ADMIN_CREDENTIAL_ROTATION_PASSWORD is missing.");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalStateException("ADMIN_CREDENTIAL_ROTATION_EMAIL must be a valid email address.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("ADMIN_CREDENTIAL_ROTATION_PASSWORD must contain at least 12 characters.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES) {
            throw new IllegalStateException(
                    "ADMIN_CREDENTIAL_ROTATION_PASSWORD must be no more than 72 UTF-8 bytes.");
        }
    }
}
