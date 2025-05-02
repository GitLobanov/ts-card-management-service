package by.lobanov.cardmanagementservice.repository;

import by.lobanov.cardmanagementservice.model.entity.Card;
import by.lobanov.cardmanagementservice.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID>, JpaSpecificationExecutor<Card> {

    /**
     * Проверяет существование карты по значению столбца card_number (который хранит зашифрованное значение).
     * Используем @Query, т.к. поле cardNumber в Entity представляет расшифрованное значение (из-за конвертера).
     *
     * @param encryptedCardNumber Зашифрованный номер карты (как он хранится в БД).
     * @return true, если карта с таким зашифрованным номером существует, иначе false.
     */
    @Query("SELECT COUNT(c) > 0 FROM Card c WHERE c.cardNumber = :encryptedNumber")
    boolean existsByEncryptedCardNumber(@Param("encryptedNumber") String encryptedCardNumber);

    /**
     * Находит все карты, соответствующие спецификации, с пагинацией.
     * Владелец (owner) загружается сразу (JOIN FETCH) для избежания N+1.
     */
    @Override
    @EntityGraph(attributePaths = {"owner"}) // Указываем атрибут для жадной загрузки
    Page<Card> findAll(Specification<Card> spec, Pageable pageable);

    /**
     * Находит карту по ID.
     * Владелец (owner) загружается сразу (JOIN FETCH).
     */
    @Override
    @EntityGraph(attributePaths = {"owner"})
    Optional<Card> findById(UUID id);

    /**
     * Находит карту по ID и владельцу.
     * Владелец (owner) загружается сразу (JOIN FETCH).
     */
    @EntityGraph(attributePaths = {"owner"})
    Optional<Card> findByIdAndOwner(UUID id, User owner);

    /**
     * Находит все карты конкретного владельца с пагинацией.
     * Владелец (owner) загружается сразу (JOIN FETCH).
     */
    @EntityGraph(attributePaths = {"owner"})
    Page<Card> findByOwner(User owner, Pageable pageable);

    /**
     * Оптимизированный запрос для получения суммарного баланса карт пользователя.
     * Суммирование происходит на стороне БД.
     * Использует COALESCE для возврата 0, если у пользователя нет карт или баланс null.
     * @param owner Владелец карт.
     * @return Суммарный баланс карт пользователя.
     */
    @Query("SELECT COALESCE(SUM(c.balance), 0) FROM Card c WHERE c.owner = :owner")
    BigDecimal getSumBalanceByOwner(@Param("owner") User owner);
}
