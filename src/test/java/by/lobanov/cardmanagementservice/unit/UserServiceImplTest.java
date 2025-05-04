package by.lobanov.cardmanagementservice.unit;

import by.lobanov.cardmanagementservice.exception.BadRequestException;
import by.lobanov.cardmanagementservice.exception.ResourceNotFoundException;
import by.lobanov.cardmanagementservice.mapper.UserMapper;
import by.lobanov.cardmanagementservice.model.constant.RoleType;
import by.lobanov.cardmanagementservice.model.dto.UserDto;
import by.lobanov.cardmanagementservice.model.dto.request.CreateUserRequest;
import by.lobanov.cardmanagementservice.model.dto.request.UpdateUserRequest;
import by.lobanov.cardmanagementservice.model.entity.Role;
import by.lobanov.cardmanagementservice.model.entity.User;
import by.lobanov.cardmanagementservice.repository.RoleRepository;
import by.lobanov.cardmanagementservice.repository.UserRepository;
import by.lobanov.cardmanagementservice.security.SecurityUserDetails;
import by.lobanov.cardmanagementservice.service.impl.UserServiceImpl;
import by.lobanov.cardmanagementservice.util.AuthenticationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static by.lobanov.cardmanagementservice.util.ServiceMessagesUtil.CANNOT_DELETE_PROTECTED_USER;
import static by.lobanov.cardmanagementservice.util.ServiceMessagesUtil.EMAIL_IS_ALREADY_TAKEN;
import static by.lobanov.cardmanagementservice.util.ServiceMessagesUtil.USER_NOT_FOUND_WITH_ID;
import static by.lobanov.cardmanagementservice.util.ServiceMessagesUtil.YOU_CANNOT_DELETE_YOUR_OWN_ACCOUNT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationHelper authenticationHelper;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Captor
    private ArgumentCaptor<User> userArgumentCaptor;

    private User testUser;
    private User protectedAdminUser;
    private UserDto testUserDto;
    private Role userRole;
    private Role adminRole;
    private Pageable pageable;
    private UUID userId;
    private UUID adminId;
    private UUID currentUserId;
    private SecurityUserDetails currentUserDetails;


    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        currentUserId = UUID.randomUUID(); // Assume current user is different

        userRole = new Role(RoleType.ROLE_USER); userRole.setId(1);
        adminRole = new Role(RoleType.ROLE_ADMIN); adminRole.setId(2);

        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setRoles(Set.of(userRole));

        protectedAdminUser = new User();
        protectedAdminUser.setId(adminId);
        protectedAdminUser.setEmail("admin@example.com");
        protectedAdminUser.setPassword("hashedAdminPassword");
        protectedAdminUser.setRoles(Set.of(userRole, adminRole));

        testUserDto = new UserDto();
        testUserDto.setId(userId);
        testUserDto.setEmail(testUser.getEmail());
        testUserDto.setRoles(Set.of(RoleType.ROLE_USER.name()));

        pageable = PageRequest.of(0, 10);

        currentUserDetails = mock(SecurityUserDetails.class);
    }

    @Nested
    @DisplayName("findAllUsers Tests")
    class FindAllUsersTests {
        @Test
        @DisplayName("Should return mapped page of users")
        void findAllUsers_shouldReturnMappedPage() {
            List<User> userList = Collections.singletonList(testUser);
            Page<User> userPage = new PageImpl<>(userList, pageable, 1);
            when(userRepository.findAllWithRoles(pageable)).thenReturn(userPage);
            when(userMapper.toDto(testUser)).thenReturn(testUserDto);

            Page<UserDto> result = userService.findAllUsers(pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals(1, result.getContent().size());
            assertEquals(testUserDto, result.getContent().get(0));
            verify(userRepository).findAllWithRoles(pageable);
            verify(userMapper).toDto(testUser);
        }
    }

    @Nested
    @DisplayName("findUserById Tests")
    class FindUserByIdTests {
        @Test
        @DisplayName("Should return mapped UserDto when user found")
        void findUserById_whenFound_shouldReturnDto() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userMapper.toDto(testUser)).thenReturn(testUserDto);

            UserDto result = userService.findUserById(userId);
            assertNotNull(result);
            assertEquals(testUserDto, result);
            verify(userRepository).findById(userId);
            verify(userMapper).toDto(testUser);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void findUserById_whenNotFound_shouldThrowResourceNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
                userService.findUserById(userId);
            });
            assertTrue(exception.getMessage().contains(USER_NOT_FOUND_WITH_ID + userId));
            verify(userRepository).findById(userId);
            verify(userMapper, never()).toDto(any());
        }
    }

    @Nested
    @DisplayName("createUser Tests")
    class CreateUserTests {
        @Test
        @DisplayName("Should create user successfully when email not taken")
        void createUser_whenEmailAvailable_shouldSucceed() {
            CreateUserRequest request = new CreateUserRequest();
            request.setEmail("new@example.com");
            request.setPassword("password123");
            request.setRoles(Set.of("ROLE_USER"));

            User mappedUser = new User();
            mappedUser.setEmail(request.getEmail());
            mappedUser.setRoles(Set.of(userRole));
            mappedUser.setPassword("hashedPasswordFromMapper");

            User savedUser = new User();
            savedUser.setId(UUID.randomUUID());
            savedUser.setEmail(request.getEmail());
            savedUser.setPassword("hashedPasswordFromMapper");
            savedUser.setRoles(Set.of(userRole));

            when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(userMapper.createUserRequestToEntity(request, roleRepository)).thenReturn(mappedUser);
            when(userRepository.save(mappedUser)).thenReturn(savedUser);
            when(userMapper.toDto(savedUser)).thenReturn(testUserDto); // Map the final saved user

            UserDto result = userService.createUser(request);

            assertNotNull(result);
            assertEquals(testUserDto, result);

            verify(userRepository).existsByEmail(request.getEmail());
            verify(userMapper).createUserRequestToEntity(request, roleRepository); // Verify mapper call
            verify(userRepository).save(mappedUser); // Verify save with the user object from mapper
            verify(userMapper).toDto(savedUser); // Verify mapping the *saved* user
        }

        @Test
        @DisplayName("Should throw BadRequestException when email is taken")
        void createUser_whenEmailTaken_shouldThrowBadRequest() {
            CreateUserRequest request = new CreateUserRequest();
            request.setEmail("existing@example.com");
            request.setPassword("password123");
            request.setRoles(Set.of("ROLE_USER"));

            when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

            
            BadRequestException exception = assertThrows(BadRequestException.class, () -> {
                userService.createUser(request);
            });

            assertTrue(exception.getMessage().contains(String.format(EMAIL_IS_ALREADY_TAKEN, request.getEmail())));
            verify(userRepository).existsByEmail(request.getEmail());
            verify(userMapper, never()).createUserRequestToEntity(any(), any());
            verify(userRepository, never()).save(any());
        }
    }


    @Nested
    @DisplayName("updateUser Tests")
    class UpdateUserTests {
        @Test
        @DisplayName("Should update roles and password when provided")
        void updateUser_whenPasswordAndRolesProvided_shouldUpdateAndSave() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setPassword("newPassword123");
            request.setRoles(Set.of("ROLE_ADMIN")); // Change role

            User existingUser = new User(); // User loaded from DB
            existingUser.setId(userId);
            existingUser.setEmail("test@example.com");
            existingUser.setPassword("oldHashedPassword");
            existingUser.setRoles(Set.of(userRole));

            Set<Role> newRoles = Set.of(adminRole); // Roles returned by mapper
            String hashedNewPassword = "hashedNewPassword";

            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.encode(request.getPassword())).thenReturn(hashedNewPassword);
            when(userMapper.mapRoleNamesToEntities(request.getRoles(), roleRepository)).thenReturn(newRoles);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0)); // Return saved user
            when(userMapper.toDto(any(User.class))).thenReturn(testUserDto); // Mock final mapping

            UserDto result = userService.updateUser(userId, request);

            assertNotNull(result);

            verify(userRepository).findById(userId);
            verify(passwordEncoder).encode(request.getPassword());
            verify(userMapper).mapRoleNamesToEntities(request.getRoles(), roleRepository);
            verify(userRepository).save(userArgumentCaptor.capture());
            User savedUser = userArgumentCaptor.getValue();

            assertEquals(userId, savedUser.getId());
            assertEquals(hashedNewPassword, savedUser.getPassword()); // Check new password
            assertEquals(newRoles, savedUser.getRoles()); // Check new roles

            verify(userMapper).toDto(savedUser);
        }

        @Test
        @DisplayName("Should update only roles when password is not provided")
        void updateUser_whenOnlyRolesProvided_shouldUpdateRolesOnly() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setRoles(Set.of("ROLE_USER", "ROLE_ADMIN"));

            User existingUser = new User();
            existingUser.setId(userId);
            existingUser.setEmail("test@example.com");
            existingUser.setPassword("oldHashedPassword");
            existingUser.setRoles(Set.of(userRole));

            Set<Role> newRoles = Set.of(userRole, adminRole);

            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
            when(userMapper.mapRoleNamesToEntities(request.getRoles(), roleRepository)).thenReturn(newRoles);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userMapper.toDto(any(User.class))).thenReturn(testUserDto);

            userService.updateUser(userId, request);

            verify(userRepository).findById(userId);
            verify(passwordEncoder, never()).encode(anyString()); // Encoder NOT called
            verify(userMapper).mapRoleNamesToEntities(request.getRoles(), roleRepository);
            verify(userRepository).save(userArgumentCaptor.capture());
            User savedUser = userArgumentCaptor.getValue();

            assertEquals(userId, savedUser.getId());
            assertEquals("oldHashedPassword", savedUser.getPassword()); // Password NOT changed
            assertEquals(newRoles, savedUser.getRoles()); // Roles ARE changed

            verify(userMapper).toDto(savedUser);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user to update not found")
        void updateUser_whenUserNotFound_shouldThrowResourceNotFound() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setRoles(Set.of("ROLE_USER"));
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            
            assertThrows(ResourceNotFoundException.class, () -> {
                userService.updateUser(userId, request);
            });

            verify(userRepository).findById(userId);
            verify(passwordEncoder, never()).encode(any());
            verify(userMapper, never()).mapRoleNamesToEntities(any(), any());
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteUser Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should delete user successfully when valid")
        void deleteUser_whenValid_shouldSucceed() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser)); // Not protected email
            doNothing().when(userRepository).delete(testUser);
            when(currentUserDetails.getId()).thenReturn(currentUserId);
            lenient().when(authenticationHelper.getCurrentUserDetails()).thenReturn(currentUserDetails);

            userService.deleteUser(userId);

            verify(authenticationHelper).getCurrentUserDetails();
            verify(userRepository).findById(userId);
            verify(userRepository).delete(testUser);
        }

        @Test
        @DisplayName("Should throw BadRequestException when deleting self")
        void deleteUser_whenDeletingSelf_shouldThrowBadRequest() {
            when(currentUserDetails.getId()).thenReturn(userId);
            when(authenticationHelper.getCurrentUserDetails()).thenReturn(currentUserDetails);

            BadRequestException exception = assertThrows(BadRequestException.class, () -> {
                userService.deleteUser(userId);
            });

            assertEquals(YOU_CANNOT_DELETE_YOUR_OWN_ACCOUNT, exception.getMessage());
            verify(authenticationHelper).getCurrentUserDetails();
            verify(userRepository, never()).findById(any());
            verify(userRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user to delete not found")
        void deleteUser_whenUserNotFound_shouldThrowResourceNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            when(currentUserDetails.getId()).thenReturn(currentUserId);
            lenient().when(authenticationHelper.getCurrentUserDetails()).thenReturn(currentUserDetails);

            assertThrows(ResourceNotFoundException.class, () -> {
                userService.deleteUser(userId);
            });

            verify(authenticationHelper).getCurrentUserDetails();
            verify(userRepository).findById(userId);
            verify(userRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw BadRequestException when deleting protected user")
        void deleteUser_whenDeletingProtectedUser_shouldThrowBadRequest() {
            when(currentUserDetails.getId()).thenReturn(currentUserId);
            lenient().when(authenticationHelper.getCurrentUserDetails()).thenReturn(currentUserDetails);

            when(userRepository.findById(adminId)).thenReturn(Optional.of(protectedAdminUser)); // User with protected email

            BadRequestException exception = assertThrows(BadRequestException.class, () -> {
                userService.deleteUser(adminId);
            });

            assertTrue(exception.getMessage().contains(CANNOT_DELETE_PROTECTED_USER + protectedAdminUser.getEmail()));
            verify(authenticationHelper).getCurrentUserDetails();
            verify(userRepository).findById(adminId);
            verify(userRepository, never()).delete(any());
        }
    }
}