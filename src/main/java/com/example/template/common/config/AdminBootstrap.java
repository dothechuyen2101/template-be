package com.example.template.common.config;

import com.example.template.modules.role.entity.RoleName;
import com.example.template.modules.user.dto.CreateUserRequest;
import com.example.template.modules.user.repository.UserRepository;
import com.example.template.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final BootstrapAdminProperties properties;
    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            return;
        }
        validateConfiguration();
        if (userRepository.existsByUsernameIgnoreCase(properties.username())
                || userRepository.existsByEmailIgnoreCase(properties.email())) {
            log.info("Admin bootstrap skipped because the configured username or email already exists");
            return;
        }

        userService.create(new CreateUserRequest(
                properties.username(),
                properties.email(),
                properties.password(),
                properties.fullName(),
                true,
                Set.of(RoleName.ADMIN, RoleName.USER)
        ));
        log.info("Bootstrap administrator created with username={}", properties.username());
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(properties.username())
                || !StringUtils.hasText(properties.email())
                || !StringUtils.hasText(properties.fullName())
                || !StringUtils.hasText(properties.password())
                || properties.password().length() < 8) {
            throw new IllegalStateException(
                    "Admin bootstrap is enabled but ADMIN_USERNAME, ADMIN_EMAIL, ADMIN_FULL_NAME, "
                            + "or ADMIN_PASSWORD is missing/invalid"
            );
        }
    }
}
