package com.taxpilot.config;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.taxpilot.service.AdminPasswordResetService;
import com.taxpilot.service.AdminPasswordResetService.ResetCredentials;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AdminPasswordResetRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminPasswordResetRunner.class);

    private final boolean enabled;
    private final String requestId;
    private final String currentPassword;
    private final String newPassword;
    private final AdminPasswordResetService resetService;

    public AdminPasswordResetRunner(
            @Value("${app.admin-password-reset.enabled:false}") boolean enabled,
            @Value("${app.admin-password-reset.request-id:}") String requestId,
            @Value("${app.admin-password-reset.current-password:}") String currentPassword,
            @Value("${app.admin-password-reset.new-password:}") String newPassword,
            AdminPasswordResetService resetService) {
        this.enabled = enabled;
        this.requestId = requestId;
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.resetService = resetService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        UUID parsedRequestId;
        try {
            parsedRequestId = UUID.fromString(requestId);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD_RESET_REQUEST_ID must be a unique UUID when password reset is enabled.",
                    exception);
        }

        if (currentPassword.isBlank() && newPassword.isBlank()) {
            resetService.resetOnce(parsedRequestId).ifPresentOrElse(
                    this::logCredentials,
                    () -> LOGGER.info("Admin password reset request was already applied; no changes were made"));
            return;
        }

        validatePasswordChangeConfiguration();
        if (resetService.changePasswordOnce(parsedRequestId, currentPassword, newPassword)) {
            LOGGER.info("Admin password changed successfully; the configured request was applied once");
        } else {
            LOGGER.info("Admin password change request was already applied; no changes were made");
        }
    }

    private void validatePasswordChangeConfiguration() {
        if (currentPassword.isBlank() || newPassword.isBlank()) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD_RESET_CURRENT_PASSWORD and ADMIN_PASSWORD_RESET_NEW_PASSWORD "
                            + "must both be provided for a password change.");
        }
        if (newPassword.length() < 12) {
            throw new IllegalStateException("ADMIN_PASSWORD_RESET_NEW_PASSWORD must contain at least 12 characters.");
        }
        if (newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD_RESET_NEW_PASSWORD must be no more than 72 UTF-8 bytes.");
        }
    }

    private void logCredentials(ResetCredentials credentials) {
        LOGGER.warn("Admin password reset completed. These credentials appear only in this startup output; "
                + "protect the logs and change the password after signing in.");
        LOGGER.warn("Admin login email: {}", credentials.email());
        LOGGER.warn("Admin temporary password: {}", credentials.password());
    }
}
