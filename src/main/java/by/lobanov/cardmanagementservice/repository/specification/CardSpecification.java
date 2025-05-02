package by.lobanov.cardmanagementservice.repository.specification;

import by.lobanov.cardmanagementservice.model.constant.CardStatus;
import by.lobanov.cardmanagementservice.model.entity.Card;
import by.lobanov.cardmanagementservice.model.entity.Card_;
import by.lobanov.cardmanagementservice.model.entity.User;
import by.lobanov.cardmanagementservice.model.entity.User_;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CardSpecification {

    /**
     * Создает спецификацию для фильтрации карт на основе параметров.
     * Поддерживает фильтрацию по статусу, email владельца (для админа), диапазону баланса.
     *
     * @param status      Статус карты (может быть null).
     * @param ownerEmail  Email владельца (для админа, может быть null).
     * @param minBalance  Минимальный баланс (может быть null).
     * @param maxBalance  Максимальный баланс (может быть null).
     * @param currentUser Текущий пользователь (если не null, фильтрует только его карты).
     * @return Specification<Card> для использования в репозитории.
     */
    public static Specification<Card> filterBy(
            CardStatus status,
            String ownerEmail,
            BigDecimal minBalance,
            BigDecimal maxBalance,
            User currentUser
    ) {
        return (Root<Card> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (currentUser != null) {
                predicates.add(criteriaBuilder.equal(root.get(Card_.owner), currentUser));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get(Card_.status), status));
            }

            if (currentUser == null && StringUtils.hasText(ownerEmail)) {
                Join<Card, User> ownerJoin = root.join(Card_.owner, JoinType.INNER);
                predicates.add(criteriaBuilder.equal(ownerJoin.get(User_.email), ownerEmail));
            }

            if (minBalance != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(Card_.balance), minBalance));
            }

            if (maxBalance != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(Card_.balance), maxBalance));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    // TODO защитить, только админ может вызывать
    public static Specification<Card> filterBy(
            CardStatus status, String ownerEmail, BigDecimal minBalance, BigDecimal maxBalance) {
        return filterBy(status, ownerEmail, minBalance, maxBalance, null);
    }
}
