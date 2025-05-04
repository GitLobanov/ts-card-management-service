package by.lobanov.cardmanagementservice.service.impl;

import by.lobanov.cardmanagementservice.exception.BadRequestException;
import by.lobanov.cardmanagementservice.exception.ResourceNotFoundException;
import by.lobanov.cardmanagementservice.mapper.UserMapper;
import by.lobanov.cardmanagementservice.model.dto.UserDto;
import by.lobanov.cardmanagementservice.model.dto.request.CreateUserRequest;
import by.lobanov.cardmanagementservice.model.dto.request.UpdateUserRequest;
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

import java.util.Set;
import java.util.UUID;

import static by.lobanov.cardmanagementservice.util.ServiceMessagesUtil.CANNOT_DELETE_PROTECTED_USER;
import static by.lobanov.cardmanagementservice.util.ServiceMessagesUtil.EMAIL_IS_ALREADY_TAKEN;
import static by.lobanov.cardmanagementservice.util.ServiceMessagesUtil.USER_NOT_FOUND_WITH_ID;
import static by.lobanov.cardmanagementservice.util.ServiceMessagesUtil.YOU_CANNOT_DELETE_YOUR_OWN_ACCOUNT;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationHelper authenticationHelper;
    private final UserMapper userMapper;

    // TODO подумать как заменить
    private static final Set<String> PROTECTED_EMAILS = Set.of("admin@example.com");

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> findAllUsers(Pageable pageable) {
        log.debug("Request to find all users. Pageable: {}", pageable);
        Page<User> userPage = userRepository.findAllWithRoles(pageable);
        return userPage.map(userMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto findUserById(UUID id) {
        log.debug("Request to find user by ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_WITH_ID + id));
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        String email = request.getEmail();
        log.info("Request to create user with email: {}", email);

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException(String.format(EMAIL_IS_ALREADY_TAKEN, email));
        }

        User user = userMapper.createUserRequestToEntity(request, roleRepository);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User savedUser = userRepository.save(user);
        log.info("User created successfully with id {} and email {}", savedUser.getId(), savedUser.getEmail());
        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    public UserDto updateUser(UUID id, UpdateUserRequest request) {
        log.info("Request to update user with ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_WITH_ID + id));

        if (StringUtils.hasText(request.getPassword())) {
            log.debug("Updating password for user ID: {}", id);
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        user.setRoles(userMapper.mapRoleNamesToEntities(request.getRoles(), roleRepository));

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully with ID: {}", updatedUser.getId());
        return userMapper.toDto(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        log.warn("Request to delete user with ID: {}", id);

        UUID currentUserId = authenticationHelper.getCurrentUserDetails().getId();
        if (currentUserId.equals(id)) {
            throw new BadRequestException(YOU_CANNOT_DELETE_YOUR_OWN_ACCOUNT);
        }

        User userToDelete = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_WITH_ID + id));

        if (PROTECTED_EMAILS.contains(userToDelete.getEmail())) {
            throw new BadRequestException(CANNOT_DELETE_PROTECTED_USER + userToDelete.getEmail());
        }

        // TODO: Подумать о дополнительной логике перед удалением (например, проверка баланса карт)
        userRepository.delete(userToDelete);
        log.info("User deleted successfully with ID: {}", id);
    }
}