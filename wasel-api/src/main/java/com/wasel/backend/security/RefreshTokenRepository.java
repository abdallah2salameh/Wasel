package com.wasel.backend.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenIdAndRevokedFalse(String tokenId);

    void deleteAllByExpiresAtBefore(Instant instant);
}
