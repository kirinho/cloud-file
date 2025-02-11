package com.liushukov.cloud_file.service;

import com.liushukov.cloud_file.dto.UserLoginDto;
import com.liushukov.cloud_file.entity.Role;
import com.liushukov.cloud_file.entity.User;
import com.liushukov.cloud_file.repository.UserRepository;
import static com.liushukov.cloud_file.service.AuthenticationServiceImplTest.TestResources.*;

import com.liushukov.cloud_file.service.impl.AuthenticationServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.Optional;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceImplTest {
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    @Test
    void givenLoginDto_authenticate_shouldReturnUser() {
        when(userRepository.findUserByEmail(USER_EMAIL)).thenReturn(Optional.of(buildUserEntity()));

        User user = authenticationService.authenticate(USER_LOGIN_DTO);

        verify(authenticationManager)
                .authenticate(
                        new UsernamePasswordAuthenticationToken(USER_LOGIN_DTO.email(), USER_LOGIN_DTO.password()
                ));
        verify(userRepository).findUserByEmail(USER_LOGIN_DTO.email());
        Assertions.assertNotNull(user);
        Assertions.assertEquals(user, buildUserEntity());
    }

    @Test
    void givenNonExistedUser_authenticate_shouldThrowBadCredentialsException() {
        when(userRepository.findUserByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        Assertions.assertThrows(BadCredentialsException.class, () -> {
            authenticationService.authenticate(USER_LOGIN_DTO);
        });
    }

    static class TestResources {
        static final String USER_FULL_NAME = "test_full_name";
        static final String USER_EMAIL = "test@gmail.com";
        static final String USER_PASSWORD = "test_password";
        static final UserLoginDto USER_LOGIN_DTO = new UserLoginDto(USER_EMAIL, USER_PASSWORD);

        static User buildUserEntity() {
            return new User()
                    .setFullName(USER_FULL_NAME)
                    .setEmail(USER_EMAIL)
                    .setRole(Role.USER)
                    .setEnabled(true)
                    .setPassword(USER_PASSWORD);
        }
    }
}
