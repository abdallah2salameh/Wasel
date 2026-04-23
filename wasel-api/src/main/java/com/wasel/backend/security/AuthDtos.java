package com.wasel.backend.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class AuthDtos {

    public record RegisterRequest(
            @Email @NotBlank String email,
            @Size(min = 8, max = 120) String password
    ) {
    }

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {
    }

    public record RefreshRequest(
            @NotBlank String refreshToken
    ) {
    }

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            UserProfile user
    ) {
    }

    public record UserProfile(
            UUID id,
            String email,
            Role role,
            Instant createdAt
    ) {
    }
}
