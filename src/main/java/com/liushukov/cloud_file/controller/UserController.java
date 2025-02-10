package com.liushukov.cloud_file.controller;

import com.liushukov.cloud_file.dto.UserDto;
import com.liushukov.cloud_file.dto.UserUpdateDto;
import com.liushukov.cloud_file.entity.User;
import com.liushukov.cloud_file.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping(path = "/me")
    public ResponseEntity<User> me(Authentication authentication) {
        User user = userService.getUserFromAuthentication(authentication);
        return ResponseEntity.status(HttpStatus.OK).body(user);
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping(path = "/update")
    public ResponseEntity<UserDto> updateUser(
            Authentication authentication, @Valid @RequestBody UserUpdateDto userUpdateDto
    ) {
        User user = userService.getUserFromAuthentication(authentication);
        UserDto userDto = userService.updateUser(user, userUpdateDto);
        return ResponseEntity.status(HttpStatus.OK).body(userDto);
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping(path = "/delete")
    public ResponseEntity<Void> deleteUser(Authentication authentication) {
        User user = userService.getUserFromAuthentication(authentication);
        if (user.isEnabled()) {
            userService.deleteUser(user);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
}
