package com.wasel.backend.security;

import com.wasel.backend.common.BadRequestException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserAccountRepository userAccountRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        if (userAccountRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BadRequestException("Email is already registered");
        }

        UserAccount user = new UserAccount();
        user.setEmail(request.email().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.CITIZEN);
        user.setEnabled(true);
        userAccountRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadRequestException("User not found"));
        return issueTokens(user);
    }

    @Transactional
    public AuthDtos.AuthResponse refresh(String refreshTokenValue) {
        if (jwtService.extractTokenType(refreshTokenValue) != TokenType.REFRESH) {
            throw new SecurityException("Invalid refresh token");
        }

        String tokenId = jwtService.extractRefreshTokenId(refreshTokenValue);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenIdAndRevokedFalse(tokenId)
                .orElseThrow(() -> new SecurityException("Refresh token is not active"));

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            refreshToken.setRevoked(true);
            throw new SecurityException("Refresh token has expired");
        }

        refreshToken.setRevoked(true);
        return issueTokens(refreshToken.getUser());
    }

    public AuthDtos.UserProfile me(UserAccount user) {
        return toProfile(user);
    }

    private AuthDtos.AuthResponse issueTokens(UserAccount user) {
        String tokenId = UUID.randomUUID().toString();
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenValue = jwtService.generateRefreshToken(user, tokenId);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenId(tokenId);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(60L * 60 * 24 * 7));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        return new AuthDtos.AuthResponse(accessToken, refreshTokenValue, toProfile(user));
    }

    private AuthDtos.UserProfile toProfile(UserAccount user) {
        return new AuthDtos.UserProfile(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
