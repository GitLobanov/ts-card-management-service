package by.lobanov.cardmanagementservice.repository;

import by.lobanov.cardmanagementservice.model.entity.RefreshToken;
import by.lobanov.cardmanagementservice.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    int deleteByUser(User user);

    // TODO Опционально: метод для удаления просроченных токенов (можно вызывать по расписанию)
    // @Modifying
    // int deleteByExpiryDateBefore(Instant now);
}
