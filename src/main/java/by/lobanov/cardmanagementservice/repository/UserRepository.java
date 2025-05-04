package by.lobanov.cardmanagementservice.repository;

import by.lobanov.cardmanagementservice.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    /**
     * Находит пользователя по ID вместе с его ролями.
     * @param id ID пользователя.
     * @return Optional<User> с загруженными ролями.
     */
    @EntityGraph(attributePaths = {"roles"})
    Optional<User> findById(UUID id);

    /**
     * Находит всех пользователей с пагинацией, загружая их роли.
     * @param pageable Параметры пагинации.
     * @return Page<User> с загруженными ролями.
     */
    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT u FROM User u")
    Page<User> findAllWithRoles(Pageable pageable);

     @Override
     @EntityGraph(attributePaths = {"roles"})
     Page<User> findAll(Pageable pageable);
}
