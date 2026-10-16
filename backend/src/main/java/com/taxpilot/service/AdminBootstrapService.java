package com.taxpilot.service;

import java.util.EnumSet;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taxpilot.domain.user.Role;
import com.taxpilot.domain.user.User;
import com.taxpilot.repository.UserRepository;

@Service
public class AdminBootstrapService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void createIfMissing(String firstName, String lastName, String email, String phone, String password) {
        if (userRepository.existsByRolesContainingOrRolesContaining(Role.ADMIN, Role.SUPER_ADMIN)) {
            return;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new IllegalStateException(
                    "Bootstrap admin email belongs to an existing non-admin account; refusing to change its privileges");
        }

        User admin = User.builder()
                .firstName(firstName.trim())
                .lastName(lastName.trim())
                .email(normalizedEmail)
                .phone(phone.trim())
                .passwordHash(passwordEncoder.encode(password))
                .roles(EnumSet.of(Role.ADMIN))
                .enabled(true)
                .build();
        userRepository.saveAndFlush(admin);
    }
}
