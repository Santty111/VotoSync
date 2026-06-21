package com.votosync.identity.controller;

import com.votosync.identity.model.User;
import com.votosync.identity.repository.UserRepository;
import com.votosync.identity.security.JwtUtils;
import com.votosync.identity.service.ExternalValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final ExternalValidationService externalValidationService;

    // Manual Dependency Injection Constructor
    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils, ExternalValidationService externalValidationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.externalValidationService = externalValidationService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        log.info("Registering user: {}", user.getUsername());
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists");
        }
        if (userRepository.findByNationalId(user.getNationalId()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("National ID already registered");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already registered");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setHasVoted(false);
        User savedUser = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        log.info("Login attempt for username: {}", request.getUsername());
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());

        if (userOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOpt.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }

        User user = userOpt.get();
        String token = jwtUtils.generateToken(user.getUsername(), user.getNationalId(), user.getFullName());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("username", user.getUsername());
        response.put("nationalId", user.getNationalId());
        response.put("fullName", user.getFullName());
        response.put("hasVoted", user.getHasVoted());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate-signature")
    public ResponseEntity<?> validateSignature(@RequestBody SignatureValidationRequest request) {
        log.info("Validating signature request for national ID: {}", request.getNationalId());
        
        boolean isValid = externalValidationService.performFullValidation(
                request.getNationalId(), 
                request.getSignature()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("valid", isValid);
        response.put("mintelValid", externalValidationService.validateMintel(request.getNationalId()));
        response.put("arcotelValid", externalValidationService.validateArcotel(request.getNationalId()));
        response.put("firmaEcValid", externalValidationService.validateFirmaEc(request.getNationalId(), request.getSignature()));

        if (isValid) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestParam String token, @RequestParam String username) {
        boolean isValid = jwtUtils.validateToken(token, username);
        if (isValid) {
            Map<String, Object> claims = new HashMap<>();
            claims.put("valid", true);
            claims.put("nationalId", jwtUtils.extractClaim(token, c -> c.get("nationalId", String.class)));
            claims.put("fullName", jwtUtils.extractClaim(token, c -> c.get("fullName", String.class)));
            return ResponseEntity.ok(claims);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is invalid or expired");
    }

    @PostMapping("/mark-voted")
    public ResponseEntity<?> markVoted(@RequestParam String nationalId) {
        log.info("Marking citizen with national ID as voted: {}", nationalId);
        Optional<User> userOpt = userRepository.findByNationalId(nationalId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setHasVoted(true);
            userRepository.save(user);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
    }

    // Manual static DTO request structures replacing lombok @Data
    public static class LoginRequest {
        private String username;
        private String password;

        public LoginRequest() {}

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class SignatureValidationRequest {
        private String nationalId;
        private String signature;

        public SignatureValidationRequest() {}

        public String getNationalId() { return nationalId; }
        public void setNationalId(String nationalId) { this.nationalId = nationalId; }

        public String getSignature() { return signature; }
        public void setSignature(String signature) { this.signature = signature; }
    }
}
