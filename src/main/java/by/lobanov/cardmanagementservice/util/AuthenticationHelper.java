package by.lobanov.cardmanagementservice.util;

import by.lobanov.cardmanagementservice.exception.ResourceNotFoundException;
import by.lobanov.cardmanagementservice.model.entity.User;
import by.lobanov.cardmanagementservice.repository.UserRepository;
import by.lobanov.cardmanagementservice.security.SecurityUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationHelper {

    @Autowired
    private UserRepository userRepository; // Инжектим репозиторий для получения полной сущности User

    /**
     * Получает объект Authentication текущего пользователя из SecurityContext.
     * @return Authentication или null, если пользователь не аутентифицирован.
     */
    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Получает AppUserDetails текущего аутентифицированного пользователя.
     * @return AppUserDetails текущего пользователя.
     * @throws IllegalStateException если пользователь не аутентифицирован или principal не AppUserDetails.
     */
    public SecurityUserDetails getCurrentUserDetails() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof SecurityUserDetails)) {
            // Это не должно происходить в защищенных эндпоинтах, но лучше проверить
            throw new IllegalStateException("User is not authenticated or principal is not AppUserDetails");
        }
        return (SecurityUserDetails) authentication.getPrincipal();
    }

    /**
     * Получает сущность User текущего аутентифицированного пользователя из базы данных.
     * @return Сущность User.
     * @throws ResourceNotFoundException если пользователь не найден в базе данных (маловероятно, если он аутентифицирован).
     * @throws IllegalStateException если пользователь не аутентифицирован.
     */
    public User getCurrentUser() {
        SecurityUserDetails userDetails = getCurrentUserDetails();
        // Используем ID из UserDetails для получения актуальной сущности из БД
        // Это гарантирует, что мы работаем с последними данными и имеем доступ ко всем полям (включая LAZY)
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found in database with ID: " + userDetails.getId()));
    }

    /**
     * Получает email текущего аутентифицированного пользователя.
     * @return Email пользователя.
     * @throws IllegalStateException если пользователь не аутентифицирован.
     */
    public String getCurrentUserEmail() {
        return getCurrentUserDetails().getUsername(); // Email хранится в username
    }
}
