package by.lobanov.cardmanagementservice.controller.impl;

import by.lobanov.cardmanagementservice.controller.AdminUserController;
import by.lobanov.cardmanagementservice.model.dto.UserDto;
import by.lobanov.cardmanagementservice.model.dto.request.CreateUserRequest;
import by.lobanov.cardmanagementservice.model.dto.request.UpdateUserRequest;
import by.lobanov.cardmanagementservice.model.dto.response.PagedResponse;
import by.lobanov.cardmanagementservice.service.UserService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserControllerImpl implements AdminUserController {

    private final UserService userService;

    @Autowired
    public AdminUserControllerImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    @GetMapping
    public ResponseEntity<PagedResponse<UserDto>> getAllUsers(
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        Page<UserDto> userPage = userService.findAllUsers(pageable);
        return ResponseEntity.ok(PagedResponse.fromPage(userPage));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID id) {
        UserDto userDto = userService.findUserById(id);
        return ResponseEntity.ok(userDto);
    }

    @Override
    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDto createdUser = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        UserDto updatedUser = userService.updateUser(id, request);
        return ResponseEntity.ok(updatedUser);
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}