package com.liushukov.cloud_file.service;

import com.liushukov.cloud_file.dto.UserDto;
import com.liushukov.cloud_file.dto.UserUpdateDto;
import com.liushukov.cloud_file.entity.User;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User getUserFromAuthentication(Authentication authentication);

    Optional<User> getUserById(long id);

    Optional<User> getUserByEmail(String email);

    List<User> getAllUsers(String sortBy, String orderBy, int pageNumber, int pageSize);

    UserDto createUser(UserDto userDto);

    UserDto updateUser(User user, UserUpdateDto userUpdateDto);

    void deleteUser(User user);
}
