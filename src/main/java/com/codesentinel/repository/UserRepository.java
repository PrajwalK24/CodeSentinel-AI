package com.codesentinel.repository;

import com.codesentinel.model.User;
import com.codesentinel.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findFirstByRole(Role role);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    long countByActiveTrue();
}
