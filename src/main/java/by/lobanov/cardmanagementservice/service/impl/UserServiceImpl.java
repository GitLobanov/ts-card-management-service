package by.lobanov.cardmanagementservice.service.impl;

import by.lobanov.cardmanagementservice.exception.BadRequestException;
import by.lobanov.cardmanagementservice.exception.ResourceNotFoundException;
import by.lobanov.cardmanagementservice.model.constant.RoleType;
import by.lobanov.cardmanagementservice.model.dto.UserDto;
import by.lobanov.cardmanagementservice.model.dto.request.CreateUserRequest;
import by.lobanov.cardmanagementservice.model.dto.request.UpdateUserRequest;
import by.lobanov.cardmanagementservice.model.entity.Role;
import by.lobanov.cardmanagementservice.model.entity.User;
import by.lobanov.cardmanagementservice.repository.RoleRepository;
import by.lobanov.cardmanagementservice.repository.UserRepository;
import by.lobanov.cardmanagementservice.service.UserService;
import by.lobanov.cardmanagementservice.util.AuthenticationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationHelper authenticationHelper;

    // TODO подумать как заменить
    private static final Set<String> PROTECTED_EMAILS = Set.of("admin@example.com");

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> findAllUsers(Pageable pageable) {
        log.debug("Request to find all users. Pageable: {}", pageable);
        // Загружаем пользователей с ролями сразу (оптимизация)
        Page<User> userPage = userRepository.findAllWithRoles(pageable); // Нужен новый метод в репозитории
        return userPage.map(this::mapUserToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto findUserById(UUID id) {
        log.debug("Request to find user by ID: {}", id);
        User user = userRepository.findByIdWithRoles(id) // Используем оптимизированный метод
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapUserToDto(user);
    }

    @Override
    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        String email = request.getEmail();
        log.info("Request to create user with email: {}", email);

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email '" + email + "' is already taken!");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(mapRoleNamesToEntities(request.getRoles())); // Преобразуем имена ролей в сущности

        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {} and email: {}", savedUser.getId(), savedUser.getEmail());
        return mapUserToDto(savedUser); // Маппим сохраненную сущность с ID
    }

    @Override
    @Transactional
    public UserDto updateUser(UUID id, UpdateUserRequest request) {
        log.info("Request to update user with ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Обновляем пароль, если он передан
        if (StringUtils.hasText(request.getPassword())) {
            log.debug("Updating password for user ID: {}", id);
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Обновляем роли (полная замена)
        user.setRoles(mapRoleNamesToEntities(request.getRoles()));

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully with ID: {}", updatedUser.getId());
        return mapUserToDto(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        log.warn("Request to delete user with ID: {}", id); // Warn т.к. деструктивная операция

        UUID currentUserId = authenticationHelper.getCurrentUserDetails().getId();
        if (currentUserId.equals(id)) {
            throw new BadRequestException("You cannot delete your own account.");
        }

        User userToDelete = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (PROTECTED_EMAILS.contains(userToDelete.getEmail())) {
            throw new BadRequestException("Cannot delete protected user: " + userToDelete.getEmail());
        }

        // TODO: Подумать о дополнительной логике перед удалением (например, проверка баланса карт)
        userRepository.delete(userToDelete);
        log.info("User deleted successfully with ID: {}", id);
    }

    private UserDto mapUserToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        // Преобразуем Set<Role> в Set<String> с именами ролей
        dto.setRoles(user.getRoles().stream()
                .map(role -> role.getName().name()) // Получаем Enum и его имя
                .collect(Collectors.toSet()));
        return dto;
    }

    // Преобразует Set<String> с именами ролей в Set<Role> сущностей
    private Set<Role> mapRoleNamesToEntities(Set<String> roleNames) {
        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            try {
                RoleType roleType = RoleType.valueOf(roleName); // Преобразуем строку в Enum
                Role role = roleRepository.findByName(roleType)
                        .orElseThrow(() -> new BadRequestException("Role not found: " + roleName));
                roles.add(role);
            } catch (IllegalArgumentException e) {
                // Если строка не соответствует ни одному значению Enum
                throw new BadRequestException("Invalid role name provided: " + roleName);
            }
        }
        // Проверяем, что не пытаемся назначить недопустимые роли (если есть такая логика)
        // Например, нельзя создать второго админа и т.д.
        return roles;
    }
}