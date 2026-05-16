package com.codesentinel.service;

import com.codesentinel.model.Role;
import com.codesentinel.model.User;
import com.codesentinel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = findByEmail(email);
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                user.isActive(),
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional
    public User register(String fullName, String username, String email, String password) {
        if (userRepository.existsByEmail(email)) throw new IllegalArgumentException("Email already registered");
        if (userRepository.existsByUsername(username)) throw new IllegalArgumentException("Username already taken");
        User user = new User();
        user.setFullName(fullName);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(Role.DEVELOPER);
        return userRepository.save(user);
    }

    @Transactional
    public void createSeedUser(String fullName, String username, String email, String password, Role role) {
        if (userRepository.existsByEmail(email)) return;
        User user = new User();
        user.setFullName(fullName);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        userRepository.save(user);
    }

    @Transactional
    public void upsertSeedUser(String fullName, String username, String email, String password, Role role) {
        User user = userRepository.findByEmail(email)
                .or(() -> userRepository.findFirstByRole(role))
                .orElseGet(User::new);
        user.setFullName(fullName);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setActive(true);
        userRepository.save(user);
    }

    @Transactional
    public void recordLogin(String email) {
        User user = findByEmail(email);
        user.setLastLogin(LocalDateTime.now());
    }

    @Transactional
    public void toggleActive(Long id) {
        User user = findById(id);
        user.setActive(!user.isActive());
    }

    @Transactional
    public void updateRole(Long id, Role role) {
        User user = findById(id);
        user.setRole(role);
    }

    public long countAll() {
        return userRepository.count();
    }

    public long countActive() {
        return userRepository.countByActiveTrue();
    }

    public long countByRole(Role role) {
        return userRepository.countByRole(role);
    }
}
