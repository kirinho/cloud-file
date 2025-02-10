package com.liushukov.cloud_file.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserDto(
        @NotNull(message = "fullName shouldn't be null")
        @Size(min = 3, max = 255, message = "size of fullName should be from 3 to 255")
        String fullName,
        @NotNull(message = "email shouldn't be null")
        @Size(max = 255, message = "size of email should be from up to 255")
        @Email(message = "email should has construction of EMAIL")
        String email,
        @NotNull(message = "password shouldn't be null")
        @Size(min = 8, max = 255, message = "size of password should be from 8 to 255")
        String password
) {
}
