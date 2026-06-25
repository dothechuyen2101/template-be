package com.example.template.modules.auth.service;

import com.example.template.common.exception.ApiException;
import com.example.template.common.security.JwtService;
import com.example.template.common.util.TokenUtils;
import com.example.template.modules.auth.entity.RefreshToken;
import com.example.template.modules.auth.repository.RefreshTokenRepository;
import com.example.template.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Transactional
    public IssuedRefreshToken issue(User user) {
        String rawToken = TokenUtils.newRefreshToken();
        RefreshToken token = new RefreshToken();
        token.setTokenHash(TokenUtils.sha256(rawToken));
        token.setUser(user);
        token.setExpiresAt(jwtService.refreshTokenExpiresAt());
        token.setRevoked(false);
        refreshTokenRepository.save(token);
        return new IssuedRefreshToken(rawToken, token);
    }

    @Transactional
    public RotatedRefreshToken rotate(String rawToken) {
        RefreshToken current = refreshTokenRepository
                .findByTokenHashAndRevokedFalse(TokenUtils.sha256(rawToken))
                .orElseThrow(() -> invalidToken("Refresh token is invalid or revoked"));

        if (current.getExpiresAt().isBefore(Instant.now())) {
            current.setRevoked(true);
            refreshTokenRepository.save(current);
            throw invalidToken("Refresh token has expired");
        }
        if (!current.getUser().isEnabled()) {
            throw invalidToken("User account is disabled");
        }

        current.setRevoked(true);
        refreshTokenRepository.save(current);
        IssuedRefreshToken issued = issue(current.getUser());
        return new RotatedRefreshToken(issued.rawToken(), current.getUser());
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHashAndRevokedFalse(TokenUtils.sha256(rawToken))
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    private ApiException invalidToken(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message);
    }

    public record IssuedRefreshToken(String rawToken, RefreshToken entity) {
    }

    public record RotatedRefreshToken(String rawToken, User user) {
    }
}
