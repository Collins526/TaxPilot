package com.taxpilot.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.taxpilot.domain.user.Role;
import com.taxpilot.domain.user.User;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findByEmailStartingWith(String prefix);

    boolean existsByRolesContainingOrRolesContaining(Role role, Role otherRole);

    long countByRolesContainingOrRolesContaining(Role role, Role otherRole);

    long countByEnabledTrue();

    Page<User> findAllByOrderByEmailAsc(Pageable pageable);

    boolean existsByEmail(String email);
}
