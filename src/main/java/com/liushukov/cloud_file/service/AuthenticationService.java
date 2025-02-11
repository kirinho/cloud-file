package com.liushukov.cloud_file.service;

import com.liushukov.cloud_file.dto.UserLoginDto;
import com.liushukov.cloud_file.entity.User;

import java.util.Optional;

public interface AuthenticationService {
    User authenticate(UserLoginDto loginDto);
}
