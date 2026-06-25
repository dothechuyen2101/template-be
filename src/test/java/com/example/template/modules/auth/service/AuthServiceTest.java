package com.example.template.modules.auth.service;

import com.example.template.common.security.JwtService;
import com.example.template.modules.auth.dto.AuthResponse;
import com.example.template.modules.auth.dto.RegisterRequest;
import com.example.template.modules.auth.entity.RefreshToken;
import com.example.template.modules.role.entity.Role;
import com.example.template.modules.role.entity.RoleName;
import com.example.template.modules.role.repository.RoleRepository;
import com.example.template.modules.user.dto.UserResponse;
import com.example.template.modules.user.entity.User;
import com.example.template.modules.user.mapper.UserMapper;
import com.example.template.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCreatesUserAndReturnsTokenPair() {
        RegisterRequest request = new RegisterRequest(
                " New.User ",
                " USER@Example.com ",
                "password123",
                "New User"
        );
        Role userRole = new Role(RoleName.USER);
        given(userRepository.findByUsernameIgnoreCase("new.user")).willReturn(Optional.empty());
        given(userRepository.findByEmailIgnoreCase("user@example.com")).willReturn(Optional.empty());
        given(roleRepository.findByName(RoleName.USER)).willReturn(Optional.of(userRole));
        given(passwordEncoder.encode("password123")).willReturn("encoded-password");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        given(jwtService.generateAccessToken(any(User.class))).willReturn("access-token");
        given(jwtService.accessTokenExpiresInSeconds()).willReturn(900L);
        given(refreshTokenService.issue(any(User.class)))
                .willReturn(new RefreshTokenService.IssuedRefreshToken("refresh-token", new RefreshToken()));
        UserResponse userResponse = response();
        given(userMapper.toResponse(any(User.class))).willReturn(userResponse);

        AuthResponse result = authService.register(request);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.user()).isEqualTo(userResponse);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("new.user");
        assertThat(captor.getValue().getEmail()).isEqualTo("user@example.com");
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(captor.getValue().getRoles()).containsExactly(userRole);
    }

    private UserResponse response() {
        return new UserResponse(
                1L,
                "new.user",
                "user@example.com",
                "New User",
                true,
                Set.of("USER"),
                Instant.now(),
                Instant.now(),
                "system",
                "system"
        );
    }
}
