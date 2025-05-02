package by.lobanov.cardmanagementservice.repository;

import by.lobanov.cardmanagementservice.model.constant.RoleType;
import by.lobanov.cardmanagementservice.model.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    // Метод для поиска роли по её имени (enum)
    Optional<Role> findByName(RoleType name);
}