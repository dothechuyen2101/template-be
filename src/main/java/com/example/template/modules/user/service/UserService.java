package com.example.template.modules.user.service;

import com.example.template.common.exception.BusinessException;
import com.example.template.common.exception.DuplicateResourceException;
import com.example.template.common.exception.ResourceNotFoundException;
import com.example.template.common.response.PageResponse;
import com.example.template.modules.auth.service.RefreshTokenService;
import com.example.template.modules.role.entity.Role;
import com.example.template.modules.role.entity.RoleName;
import com.example.template.modules.role.repository.RoleRepository;
import com.example.template.modules.user.dto.CreateUserRequest;
import com.example.template.modules.user.dto.UpdateUserRequest;
import com.example.template.modules.user.dto.UserResponse;
import com.example.template.modules.user.entity.User;
import com.example.template.modules.user.mapper.UserMapper;
import com.example.template.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        return userMapper.toResponse(findByUsername(currentUsername()));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(String keyword, Pageable pageable) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return PageResponse.from(userRepository.search(normalizedKeyword, pageable).map(userMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userMapper.toResponse(findById(id));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String username = normalizeUsername(request.username());
        String email = normalizeEmail(request.email());
        ensureUsernameAvailable(username, null);
        ensureEmailAvailable(email, null);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setEnabled(request.enabled());
        user.setRoles(resolveRoles(request.roles()));
        userRepository.save(user);
        log.info("Admin created user id={} username={}", user.getId(), user.getUsername());
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = findById(id);
        boolean credentialsChanged = false;

        if (request.username() != null) {
            String username = normalizeRequired(request.username(), "Username");
            ensureUsernameAvailable(username, id);
            user.setUsername(username);
        }
        if (request.email() != null) {
            String email = normalizeRequired(request.email(), "Email");
            ensureEmailAvailable(email, id);
            user.setEmail(email);
        }
        if (request.password() != null) {
            user.setPassword(passwordEncoder.encode(request.password()));
            credentialsChanged = true;
        }
        if (request.fullName() != null) {
            user.setFullName(normalizeRequired(request.fullName(), "Full name"));
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
            credentialsChanged = credentialsChanged || !request.enabled();
        }
        if (request.roles() != null) {
            if (request.roles().isEmpty()) {
                throw new BusinessException("At least one role is required");
            }
            preventRemovingLastAdmin(user, request.roles());
            user.setRoles(resolveRoles(request.roles()));
            credentialsChanged = true;
        }

        userRepository.save(user);
        if (credentialsChanged) {
            refreshTokenService.revokeAllForUser(user.getId());
        }
        log.info("Admin updated user id={}", user.getId());
        return userMapper.toResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = findById(id);
        if (user.getUsername().equalsIgnoreCase(currentUsername())) {
            throw new BusinessException("You cannot delete your own account");
        }
        preventDeletingLastAdmin(user);
        userRepository.delete(user);
        log.info("Admin deleted user id={} username={}", id, user.getUsername());
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private User findByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Set<Role> resolveRoles(Set<RoleName> roleNames) {
        Set<Role> roles = new HashSet<>();
        for (RoleName roleName : roleNames) {
            roles.add(roleRepository.findByName(roleName)
                    .orElseThrow(() -> new IllegalStateException("Role is missing: " + roleName)));
        }
        return roles;
    }

    private void ensureUsernameAvailable(String username, Long currentId) {
        userRepository.findByUsernameIgnoreCase(username)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Username is already in use");
                });
    }

    private void ensureEmailAvailable(String email, Long currentId) {
        userRepository.findByEmailIgnoreCase(email)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Email is already in use");
                });
    }

    private void preventDeletingLastAdmin(User user) {
        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN);
        if (isAdmin && userRepository.countDistinctByRolesName(RoleName.ADMIN) <= 1) {
            throw new BusinessException("The last administrator cannot be deleted");
        }
    }

    private void preventRemovingLastAdmin(User user, Set<RoleName> requestedRoles) {
        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN);
        if (isAdmin
                && !requestedRoles.contains(RoleName.ADMIN)
                && userRepository.countDistinctByRolesName(RoleName.ADMIN) <= 1) {
            throw new BusinessException("The ADMIN role cannot be removed from the last administrator");
        }
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private String normalizeUsername(String username) {
        return normalizeRequired(username, "Username").toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        return normalizeRequired(email, "Email").toLowerCase(Locale.ROOT);
    }

    private String normalizeRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
