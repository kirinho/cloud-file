package com.liushukov.cloud_file.controller;

import com.liushukov.cloud_file.dto.UserDto;
import com.liushukov.cloud_file.dto.UserLoginDto;
import com.liushukov.cloud_file.entity.User;
import com.liushukov.cloud_file.service.AuthenticationService;
import com.liushukov.cloud_file.service.JwtService;
import com.liushukov.cloud_file.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Optional;

@RestController
@RequestMapping(path = "/auth")
public class AuthenticationController {
    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final JwtService jwtService;

    public AuthenticationController(UserService userService, AuthenticationService authenticationService, JwtService jwtService) {
        this.userService = userService;
        this.authenticationService = authenticationService;
        this.jwtService = jwtService;
    }

    @PostMapping(path = "/register")
    public ResponseEntity<UserDto> registration(@Valid @RequestBody UserDto userDto) {
        Optional<User> existedUser = userService.getUserByEmail(userDto.email());
        if (existedUser.isEmpty()) {
            UserDto dto = userService.createUser(userDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping(path = "/login")
    public ResponseEntity<String> login(@Valid @RequestBody UserLoginDto loginDto) {
        User user = authenticationService.authenticate(loginDto);
        String jwt = jwtService.generateToken(user);
        return ResponseEntity.status(HttpStatus.OK).body(jwt);
    }
}
