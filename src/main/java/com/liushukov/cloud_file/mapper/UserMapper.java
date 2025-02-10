package com.liushukov.cloud_file.mapper;

import com.liushukov.cloud_file.dto.UserDto;
import com.liushukov.cloud_file.dto.UserUpdateDto;
import com.liushukov.cloud_file.entity.Role;
import com.liushukov.cloud_file.entity.User;
import org.mapstruct.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "role", source = "role")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "password", expression = "java(passwordEncoder.encode(userDto.password()))")
    @Mapping(target = "authorities", ignore = true)
    User toEntity(UserDto userDto, Role role, boolean enabled, PasswordEncoder passwordEncoder);

    UserDto fromEntity(User user);

    default User updateUserFromDto(UserUpdateDto userUpdateDto, User user, PasswordEncoder passwordEncoder) {
        if (userUpdateDto.password() != null) {
            user.setPassword(passwordEncoder.encode(userUpdateDto.password()));
        }
        return update(userUpdateDto, user);
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "authorities", ignore = true)
    @Mapping(target = "enabled", expression = "java(user.getEnabled())")
    @Mapping(target = "password", expression = "java(user.getPassword())")
    @Mapping(target = "role", expression = "java(user.getRole())")
    User update(UserUpdateDto userUpdateDto, @MappingTarget User user);
}
