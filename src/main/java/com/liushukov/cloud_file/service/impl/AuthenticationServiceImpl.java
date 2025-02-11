package com.liushukov.cloud_file.service.impl;

import com.liushukov.cloud_file.dto.UserLoginDto;
import com.liushukov.cloud_file.entity.User;
import com.liushukov.cloud_file.repository.UserRepository;
import com.liushukov.cloud_file.service.AuthenticationService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    public AuthenticationServiceImpl(AuthenticationManager authenticationManager, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }

    @Override
    public User authenticate(UserLoginDto loginDto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.email(), loginDto.password())
        );
        return userRepository
                .findUserByEmail(loginDto.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
    }
}
