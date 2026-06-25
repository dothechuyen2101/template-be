package com.example.template.modules.user.service;

import com.example.template.common.exception.BusinessException;
import com.example.template.modules.auth.service.RefreshTokenService;
import com.example.template.modules.role.entity.Role;
import com.example.template.modules.role.entity.RoleName;
import com.example.template.modules.role.repository.RoleRepository;
import com.example.template.modules.user.dto.CreateUserRequest;
import com.example.template.modules.user.dto.UserResponse;
import com.example.template.modules.user.entity.User;
import com.example.template.modules.user.mapper.UserMapper;
import com.example.template.modules.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserService userService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createNormalizesAndMapsUser() {
        Role userRole = new Role(RoleName.USER);
        CreateUserRequest request = new CreateUserRequest(
                " Example.User ",
                " USER@Example.com ",
                "password123",
                "Example User",
                true,
                Set.of(RoleName.USER)
        );
        given(userRepository.findByUsernameIgnoreCase("example.user")).willReturn(Optional.empty());
        given(userRepository.findByEmailIgnoreCase("user@example.com")).willReturn(Optional.empty());
        given(roleRepository.findByName(RoleName.USER)).willReturn(Optional.of(userRole));
        given(passwordEncoder.encode("password123")).willReturn("encoded");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
        UserResponse response = response();
        given(userMapper.toResponse(any(User.class))).willReturn(response);

        UserResponse result = userService.create(request);

        assertThat(result).isEqualTo(response);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("example.user");
        assertThat(captor.getValue().getEmail()).isEqualTo("user@example.com");
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded");
    }

    @Test
    void deleteRejectsRemovingLastAdministrator() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("another-admin", null)
        );
        Role adminRole = new Role(RoleName.ADMIN);
        User admin = new User();
        admin.setId(1L);
        admin.setUsername("only-admin");
        admin.setRoles(Set.of(adminRole));
        given(userRepository.findById(1L)).willReturn(Optional.of(admin));
        given(userRepository.countDistinctByRolesName(RoleName.ADMIN)).willReturn(1L);

        assertThatThrownBy(() -> userService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("last administrator");
    }

    private UserResponse response() {
        return new UserResponse(
                2L,
                "example.user",
                "user@example.com",
                "Example User",
                true,
                Set.of("USER"),
                Instant.now(),
                Instant.now(),
                "admin",
                "admin"
        );
    }
}
