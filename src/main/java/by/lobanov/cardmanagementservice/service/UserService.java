package by.lobanov.cardmanagementservice.service;

import by.lobanov.cardmanagementservice.model.dto.UserDto;
import by.lobanov.cardmanagementservice.model.dto.request.CreateUserRequest;
import by.lobanov.cardmanagementservice.model.dto.request.UpdateUserRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    /**
     * Получает страницу всех пользователей.
     * @param pageable Параметры пагинации и сортировки.
     * @return Страница с DTO пользователей.
     */
    Page<UserDto> findAllUsers(Pageable pageable);

    /**
     * Находит пользователя по ID.
     * @param id ID пользователя.
     * @return DTO пользователя.
     */
    UserDto findUserById(UUID id);

    /**
     * Создает нового пользователя.
     * @param request DTO с данными для создания.
     * @return DTO созданного пользователя.
     */
    UserDto createUser(CreateUserRequest request);

    /**
     * Обновляет существующего пользователя (пароль и/или роли).
     * @param id ID пользователя для обновления.
     * @param request DTO с данными для обновления.
     * @return DTO обновленного пользователя.
     */
    UserDto updateUser(UUID id, UpdateUserRequest request);

    /**
     * Удаляет пользователя по ID.
     * @param id ID пользователя для удаления.
     */
    void deleteUser(UUID id);
}
