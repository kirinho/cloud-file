package com.liushukov.cloud_file.controller;

import com.liushukov.cloud_file.dto.UserDto;
import com.liushukov.cloud_file.dto.UserUpdateDto;
import com.liushukov.cloud_file.entity.User;
import com.liushukov.cloud_file.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = "admin/users/")
public class AdminController {
    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(path = "/user/{userId}")
    public ResponseEntity<User> userById(@PathVariable(value = "userId") Long userId) {
        Optional<User> user = userService.getUserById(userId);
        return user
                .map(value -> ResponseEntity.status(HttpStatus.OK).body(value))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(path = "/all")
    public ResponseEntity<List<User>> allUsers(
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(value = "orderBy", defaultValue = "asc") String orderBy,
            @RequestParam(value = "pageNumber", defaultValue = "0", required = false) Integer pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10", required = false) Integer pageSize
    ) {
        List<User> users = userService.getAllUsers(sortBy, orderBy, pageNumber, pageSize);
        return ResponseEntity.status(HttpStatus.OK).body(users);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(path = "/create")
    public ResponseEntity<UserDto> createUserForAdmin(@Valid @RequestBody UserDto userDto) {
        Optional<User> existedUser = userService.getUserByEmail(userDto.email());
        if (existedUser.isEmpty()) {
            UserDto dto = userService.createUser(userDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping(path = "/update/{userId}")
    public ResponseEntity<UserDto> updateUserForAdmin(
            @PathVariable(value = "userId") Long userId, @Valid @RequestBody UserUpdateDto userUpdateDto
    ) {
        Optional<User> existedUser = userService.getUserById(userId);
        if (existedUser.isPresent()) {
            UserDto userDto = userService.updateUser(existedUser.get(), userUpdateDto);
            return ResponseEntity.status(HttpStatus.OK).body(userDto);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/delete/{userId}")
    public ResponseEntity<Void> deleteUserForAdmin(@PathVariable(value =  "userId") Long userId) {
        Optional<User> existedUser = userService.getUserById(userId);
        if (existedUser.isPresent()) {
            if (existedUser.get().isEnabled()) {
                userService.deleteUser(existedUser.get());
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
