package com.example.template.modules.auth.service;

import com.example.template.common.exception.DuplicateResourceException;
import com.example.template.common.exception.ResourceNotFoundException;
import com.example.template.common.security.JwtService;
import com.example.template.modules.auth.dto.AuthResponse;
import com.example.template.modules.auth.dto.LoginRequest;
import com.example.template.modules.auth.dto.RegisterRequest;
import com.example.template.modules.role.entity.Role;
import com.example.template.modules.role.entity.RoleName;
import com.example.template.modules.role.repository.RoleRepository;
import com.example.template.modules.user.entity.User;
import com.example.template.modules.user.mapper.UserMapper;
import com.example.template.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        String email = normalizeEmail(request.email());
        ensureUsernameAvailable(username, null);
        ensureEmailAvailable(email, null);

        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException("Default USER role is missing"));

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setEnabled(true);
        user.setRoles(Set.of(userRole));
        userRepository.save(user);

        log.info("Registered user id={} username={}", user.getId(), user.getUsername());
        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.identifier().trim(), request.password())
        );
        User user = userRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user no longer exists"));
        log.info("User logged in id={} username={}", user.getId(), user.getUsername());
        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotate(rawRefreshToken);
        User user = rotated.user();
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                rotated.rawToken(),
                "Bearer",
                jwtService.accessTokenExpiresInSeconds(),
                userMapper.toResponse(user)
        );
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private AuthResponse createAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.issue(user).rawToken();
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.accessTokenExpiresInSeconds(),
                userMapper.toResponse(user)
        );
    }

    private void ensureUsernameAvailable(String username, Long currentUserId) {
        userRepository.findByUsernameIgnoreCase(username)
                .filter(user -> !user.getId().equals(currentUserId))
                .ifPresent(user -> {
                    throw new DuplicateResourceException("Username is already in use");
                });
    }

    private void ensureEmailAvailable(String email, Long currentUserId) {
        userRepository.findByEmailIgnoreCase(email)
                .filter(user -> !user.getId().equals(currentUserId))
                .ifPresent(user -> {
                    throw new DuplicateResourceException("Email is already in use");
                });
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
