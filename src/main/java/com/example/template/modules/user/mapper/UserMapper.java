package com.example.template.modules.user.mapper;

import com.example.template.modules.role.entity.Role;
import com.example.template.modules.user.dto.UserResponse;
import com.example.template.modules.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", expression = "java(mapRoles(user.getRoles()))")
    UserResponse toResponse(User user);

    default Set<String> mapRoles(Set<Role> roles) {
        return roles.stream()
                .map(role -> role.getName().name())
                .sorted()
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }
}
