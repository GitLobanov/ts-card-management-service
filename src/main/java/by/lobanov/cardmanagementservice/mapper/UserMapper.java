package by.lobanov.cardmanagementservice.mapper;

import by.lobanov.cardmanagementservice.exception.BadRequestException;
import by.lobanov.cardmanagementservice.model.constant.RoleType;
import by.lobanov.cardmanagementservice.model.dto.UserDto;
import by.lobanov.cardmanagementservice.model.dto.request.CreateUserRequest;
import by.lobanov.cardmanagementservice.model.entity.Role;
import by.lobanov.cardmanagementservice.model.entity.User;
import java.util.Collections;

import by.lobanov.cardmanagementservice.repository.RoleRepository;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static by.lobanov.cardmanagementservice.util.ServiceMessagesUtil.INVALID_ROLE_NAME_PROVIDED;
import static by.lobanov.cardmanagementservice.util.ServiceMessagesUtil.ROLE_NOT_FOUND;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        collectionMappingStrategy = CollectionMappingStrategy.SETTER_PREFERRED
)
public interface UserMapper {

    @Mapping(target = "roles", expression = "java(mapRoleNamesToEntities(userDto.getRoles(), roleRepository))")
    @Mapping(target = "id", ignore = true)
    User toEntity(UserDto userDto, @Context RoleRepository roleRepository);

    @Mapping(target = "roles", expression = "java(mapRoleNamesToEntities(request.getRoles(), roleRepository))")
    User createUserRequestToEntity(CreateUserRequest request, @Context RoleRepository roleRepository);

    @Mapping(target = "roles", expression = "java(mapRoleSetToStringSet(user.getRoles()))")
    UserDto toDto(User user);

    /**
     * Set<Role> (из сущности User) в Set<String> (для UserDto).
     *
     * @param roles Набор сущностей Role.
     * @return Набор строковых имен ролей (например, "ROLE_USER", "ROLE_ADMIN").
     */
    default Set<String> mapRoleSetToStringSet(Set<Role> roles) {
        if (roles == null) {
            return Collections.emptySet();
        }
        return roles.stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
    }

    /**
     * Set<String> (UserDto) в Set<Role> (для User).
     *
     * @param roleNames строковых имен ролей (например, "ROLE_USER", "ROLE_ADMIN").
     * @return  roles Набор сущностей Role.
     */
    default Set<Role> mapRoleNamesToEntities(Set<String> roleNames, @Context RoleRepository roleRepository) {
        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            try {
                RoleType roleType = RoleType.valueOf(roleName);
                Role role = roleRepository.findByName(roleType)
                        .orElseThrow(() -> new BadRequestException(ROLE_NOT_FOUND + roleName));
                roles.add(role);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(INVALID_ROLE_NAME_PROVIDED + roleName);
            }
        }
        return roles;
    }
}