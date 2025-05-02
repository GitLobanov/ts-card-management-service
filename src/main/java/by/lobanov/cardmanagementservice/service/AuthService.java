package by.lobanov.cardmanagementservice.service;

import by.lobanov.cardmanagementservice.model.dto.request.RegisterRequest;

public interface AuthService {

    /**
     * Регистрирует нового пользователя в системе.
     *
     * @param registerRequest DTO с данными для регистрации.
     * @throws by.lobanov.cardmanagementservice.exception.BadRequestException если пользователь с таким email уже существует.
     * @throws by.lobanov.cardmanagementservice.exception.ResourceNotFoundException если роль ROLE_USER не найдена в БД.
     */
    void registerUser(RegisterRequest registerRequest);
}
