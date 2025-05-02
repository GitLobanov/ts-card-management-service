package by.lobanov.cardmanagementservice.service;

import by.lobanov.cardmanagementservice.model.entity.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenService {
    Optional<RefreshToken> findByToken(String token);
    RefreshToken createRefreshToken(UUID userId);
    RefreshToken verifyExpiration(RefreshToken token);
    int deleteByUserId(UUID userId);
}
