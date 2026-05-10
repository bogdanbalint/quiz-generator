package com.quiz_generator.service;

import com.quiz_generator.domain.entity.User;
import com.quiz_generator.domain.model.RegisterRequest;
import com.quiz_generator.domain.model.Role;
import com.quiz_generator.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public void register(RegisterRequest request) {
        validateRegisterRequest(request);

        if (userRepository.existsByEmail(request.getEmail().trim())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new IllegalArgumentException("Username is already in use");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);

        userRepository.save(user);
        emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());

    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Invalid registration request");
        }

        if (isBlank(request.getUsername())) {
            throw new IllegalArgumentException("Username is required");
        }

        if (request.getUsername().trim().length() < 3 || request.getUsername().trim().length() > 100) {
            throw new IllegalArgumentException("Username must be between 3 and 100 characters");
        }

        if (isBlank(request.getEmail())) {
            throw new IllegalArgumentException("Email is required");
        }

        String email = request.getEmail().trim();
        if (email.length() > 150) {
            throw new IllegalArgumentException("Email must be at most 150 characters");
        }

        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Email format is invalid");
        }

        if (isBlank(request.getPassword())) {
            throw new IllegalArgumentException("Password is required");
        }

        if (request.getPassword().length() < 6 || request.getPassword().length() > 100) {
            throw new IllegalArgumentException("Password must be between 6 and 100 characters");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }
}