package by.lobanov.cardmanagementservice.service.impl;

import by.lobanov.cardmanagementservice.exception.ResourceNotFoundException;
import by.lobanov.cardmanagementservice.exception.TokenRefreshException;
import by.lobanov.cardmanagementservice.model.entity.RefreshToken;
import by.lobanov.cardmanagementservice.model.entity.User;
import by.lobanov.cardmanagementservice.repository.RefreshTokenRepository;
import by.lobanov.cardmanagementservice.repository.UserRepository;
import by.lobanov.cardmanagementservice.service.RefreshTokenService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);

    @Value("${app.jwt.refresh-token.duration-ms}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Autowired
    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    @Transactional // Важно для удаления и создания в одной транзакции
    public RefreshToken createRefreshToken(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User not found with id: " + userId)
        );

        // Удаляем старые токены этого пользователя для простоты (один активный токен на пользователя)
        // В более сложных сценариях можно разрешать несколько токенов (например, с разных устройств)
        int deletedCount = refreshTokenRepository.deleteByUser(user);
        if (deletedCount > 0) {
            logger.info("Deleted {} existing refresh token(s) for user ID: {}", deletedCount, userId);
        }

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString()); // Генерируем уникальный токен

        refreshToken = refreshTokenRepository.save(refreshToken);
        logger.info("Created new refresh token for user ID: {}", userId);
        return refreshToken;
    }

    @Override
    @Transactional // Если токен истек, мы его удаляем
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            logger.warn("Refresh token expired and will be deleted: {}", token.getToken());
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new signin request");
        }
        logger.debug("Refresh token is valid: {}", token.getToken());
        return token;
    }

    @Override
    @Transactional
    public int deleteByUserId(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User not found with id: " + userId)
        );
        int deletedCount = refreshTokenRepository.deleteByUser(user);
        logger.info("Deleted {} refresh token(s) for user ID: {} by explicit request (logout/etc).", deletedCount, userId);
        return deletedCount;
    }
}