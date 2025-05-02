package by.lobanov.cardmanagementservice.mapper;

import by.lobanov.cardmanagementservice.model.dto.UserDto;
import by.lobanov.cardmanagementservice.model.entity.Role;
import by.lobanov.cardmanagementservice.model.entity.User;
import java.util.Collections;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED
)
public interface UserMapper {

    User toEntity(UserDto userDto);
    UserDto toDto(User user);

    /**
     * Кастомный метод, который MapStruct будет использовать для преобразования
     * Set<Role> (из сущности User) в Set<String> (для UserDto).
     *
     * @param roles Набор сущностей Role.
     * @return Набор строковых имен ролей (например, "ROLE_USER", "ROLE_ADMIN").
     */
    default Set<String> roleSetToStringSet(Set<Role> roles) {
        if (roles == null) {
            return Collections.emptySet();
        }
        return roles.stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
    }
}