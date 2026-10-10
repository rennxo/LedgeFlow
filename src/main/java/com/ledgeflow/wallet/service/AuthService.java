package com.ledgeflow.wallet.service;

import com.ledgeflow.wallet.dto.auth.AuthRequest;
import com.ledgeflow.wallet.dto.auth.AuthResponse;
import com.ledgeflow.wallet.dto.auth.RegisterRequest;
import com.ledgeflow.wallet.entity.User;
import com.ledgeflow.wallet.enums.Currency;
import com.ledgeflow.wallet.enums.Role;
import com.ledgeflow.wallet.exception.ResourceNotFoundException;
import com.ledgeflow.wallet.repository.UserRepository;
import com.ledgeflow.wallet.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final WalletService walletService;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       WalletService walletService,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.walletService = walletService;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already in use: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                Role.ROLE_USER
        );

        User savedUser = userRepository.save(user);

        // Automatically create a default primary USD wallet for new users
        walletService.createDefaultWalletForUser(savedUser, Currency.USD);

        auditService.logAction("USER_REGISTRATION", savedUser.getUsername(),
                "User registered with email: " + savedUser.getEmail(), null);

        String token = tokenProvider.generateTokenFromUsername(savedUser.getUsername());
        return new AuthResponse(token, savedUser.getUsername(), savedUser.getEmail(), savedUser.getRole());
    }

    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUsername()));

        String token = tokenProvider.generateToken(authentication);

        auditService.logAction("USER_LOGIN", user.getUsername(), "Successful login", null);

        return new AuthResponse(token, user.getUsername(), user.getEmail(), user.getRole());
    }
}
