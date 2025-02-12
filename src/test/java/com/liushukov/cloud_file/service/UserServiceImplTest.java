package com.liushukov.cloud_file.service;

import com.liushukov.cloud_file.dto.UserDto;
import com.liushukov.cloud_file.dto.UserUpdateDto;
import com.liushukov.cloud_file.entity.Role;
import com.liushukov.cloud_file.entity.User;
import com.liushukov.cloud_file.mapper.UserMapper;
import com.liushukov.cloud_file.repository.UserRepository;
import com.liushukov.cloud_file.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.liushukov.cloud_file.service.UserServiceImplTest.TestResources.buildPageRequestDescending;
import static org.mockito.Mockito.*;
import static com.liushukov.cloud_file.service.UserServiceImplTest.TestResources.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private Authentication authentication;
    @Mock
    private UserDetails userDetails;
    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void givenAuthentication_getUserFromAuthentication_shouldReturnUser() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(USER_EMAIL);
        when(userRepository.findUserByEmail(USER_EMAIL)).thenReturn(Optional.of(buildUserEntity()));

        User user = userService.getUserFromAuthentication(authentication);

        Assertions.assertNotNull(user);
        verify(userRepository).findUserByEmail(user.getEmail());
        Assertions.assertEquals(user, buildUserEntity());
    }

    @Test
    void givenAuthentication_getUserFromAuthentication_shouldTrowAnError() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(USER_EMAIL);

        Assertions.assertThrows(ResponseStatusException.class, () -> {
            userService.getUserFromAuthentication(authentication);
        });
    }

    @Test
    void givenUserId_getUserById_shouldReturnNonEmptyUser() {
        when(userRepository.findUserById(USER_ID)).thenReturn(Optional.of(buildUserEntity()));

        Optional<User> user = userService.getUserById(USER_ID);

        verify(userRepository).findUserById(USER_ID);
        Assertions.assertTrue(user.isPresent());
    }

    @Test
    void givenUserId_getUserById_shouldReturnEmptyUser() {
        when(userRepository.findUserById(USER_ID)).thenReturn(Optional.empty());

        Optional<User> user = userService.getUserById(USER_ID);

        Assertions.assertTrue(user.isEmpty());
    }

    @Test
    void givenUserEmail_getUserByEmail_shouldReturnNonEmptyUser() {
        when(userRepository.findUserByEmail(USER_EMAIL)).thenReturn(Optional.of(buildUserEntity()));

        Optional<User> user = userService.getUserByEmail(USER_EMAIL);

        verify(userRepository).findUserByEmail(USER_EMAIL);
        Assertions.assertTrue(user.isPresent());
    }

    @Test
    void givenUserEmail_getUserByEmail_shouldReturnEmptyUser() {
        when(userRepository.findUserByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        Optional<User> user = userService.getUserByEmail(USER_EMAIL);

        Assertions.assertTrue(user.isEmpty());
    }

    @Test
    void givenPageable_getAllUsers_shouldReturnNonEmptyAscending() {
        Pageable pageable = buildPageRequestAscending();
        when(userRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(buildUserListAscending()));

        List<User> users = userService.getAllUsers(SORT_BY, ORDER_BY, PAGE_NUMBER, PAGE_SIZE);

        verify(userRepository).findAll(pageable);
        Assertions.assertEquals(2, users.size());
        Assertions.assertEquals(users.get(0), buildUserEntity());
    }

    @Test
    void givenPageable_getAllUsers_shouldReturnNonEmptyDescending() {
        Pageable pageable = buildPageRequestDescending();
        when(userRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(buildUserListDescending()));

        List<User> users = userService.getAllUsers(SORT_BY, ORDER_BY_DESC, PAGE_NUMBER, PAGE_SIZE);
        verify(userRepository).findAll(pageable);
        Assertions.assertEquals(2, users.size());
        Assertions.assertEquals(users.get(users.size() - 1), buildUserEntity());
    }

    @Test
    void givenPageable_getAllUsers_shouldReturnEmptyList() {
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(Collections.emptyList()));

        List<User> users = userService.getAllUsers(SORT_BY, ORDER_BY, PAGE_NUMBER, PAGE_SIZE);

        verify(userRepository).findAll(any(PageRequest.class));
        Assertions.assertEquals(0, users.size());
    }

    @Test
    void givenUserDto_createUser_shouldSaveUserAndReturnDto() {
        when(userMapper.toEntity(buildUserDto(), Role.USER, true, passwordEncoder))
                .thenReturn(buildUserEntity());
        when(userRepository.save(buildUserEntity())).thenReturn(buildUserEntity());
        when(userMapper.fromEntity(buildUserEntity())).thenReturn(buildUserDto());

        UserDto userDto = userService.createUser(buildUserDto());

        verify(userRepository).save(buildUserEntity());
        Assertions.assertNotNull(userDto);
    }

    @Test
    void givenUserAndUserUpdateDto_updateUser_shouldUpdateUserAndReturnDto() {
        User user = buildUserEntity();
        user.setEmail(USER_EMAIL);
        when(userMapper.updateUserFromDto(buildUserUpdateDto(), buildUserEntity(), passwordEncoder)).thenReturn(user);
        when(userRepository.save(buildUserEntity())).thenReturn(user);
        when(userMapper.fromEntity(user)).thenReturn(buildUserUpdatedDto());

        UserDto userDto = userService.updateUser(buildUserEntity(), buildUserUpdateDto());

        verify(userRepository).save(buildUserEntity());
        Assertions.assertNotNull(userDto);
        Assertions.assertEquals(userDto, buildUserUpdatedDto());
    }

    @Test
    void givenUser_deleteUser_shouldUpdateUserEnabled() {
        when(userRepository.save(any(User.class))).thenReturn(buildDisableUserEntity());

        User testedUser = buildUserEntity();
        userService.deleteUser(testedUser);

        verify(userRepository).save(buildDisableUserEntity());
        Assertions.assertFalse(testedUser.getEnabled());
    }

    static class TestResources {
        static final Long USER_ID = 1L;
        static final String USER_FULL_NAME = "test_full_name";
        static final String USER_EMAIL = "test@gmail.com";
        static final String USER_PASSWORD = "test_password";
        static final String SORT_BY = "id";
        static final String ORDER_BY = "asc";
        static final String ORDER_BY_DESC = "desc";
        static final int PAGE_NUMBER = 0;
        static final int PAGE_SIZE = 10;
        static final String USER_FULL_NAME_2 = "test_full_name_2";
        static final String USER_EMAIL_2 = "test_2@gmail.com";
        static final String USER_PASSWORD_2 = "test_password_2";

        static UserDto buildUserDto() {
            return new UserDto(
                    USER_FULL_NAME,
                    USER_EMAIL,
                    USER_PASSWORD
            );
        }

        static UserDto buildUserUpdatedDto() {
            return new UserDto(
                    USER_FULL_NAME,
                    USER_EMAIL_2,
                    USER_PASSWORD
            );
        }

        static UserUpdateDto buildUserUpdateDto() {
            return new UserUpdateDto(
                    null,
                    USER_EMAIL_2,
                    null
            );
        }

        static User buildUserEntity() {
            return new User()
                    .setFullName(USER_FULL_NAME)
                    .setEmail(USER_EMAIL)
                    .setRole(Role.USER)
                    .setEnabled(true)
                    .setPassword(USER_PASSWORD);
        }

        static User buildDisableUserEntity() {
            return new User()
                    .setFullName(USER_FULL_NAME)
                    .setEmail(USER_EMAIL)
                    .setRole(Role.USER)
                    .setEnabled(false)
                    .setPassword(USER_PASSWORD);
        }

        static PageRequest buildPageRequestAscending() {
            return PageRequest.of(PAGE_NUMBER, PAGE_SIZE, Sort.by(SORT_BY).ascending());
        }

        static Pageable buildPageRequestDescending() {
            return PageRequest.of(PAGE_NUMBER, PAGE_SIZE, Sort.by(SORT_BY).descending());
        }

        static User buildUserEntity2() {
            return new User()
                    .setFullName(USER_FULL_NAME_2)
                    .setEmail(USER_EMAIL_2)
                    .setRole(Role.ADMIN)
                    .setEnabled(true)
                    .setPassword(USER_PASSWORD_2);
        }

        static List<User> buildUserListAscending() {
            return List.of(buildUserEntity(), buildUserEntity2());
        }

        static List<User> buildUserListDescending() {
            return List.of(buildUserEntity2(), buildUserEntity());
        }
    }
}
