package com.liushukov.cloud_file.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liushukov.cloud_file.dto.UserDto;
import com.liushukov.cloud_file.dto.UserUpdateDto;
import com.liushukov.cloud_file.entity.Role;
import com.liushukov.cloud_file.entity.User;
import com.liushukov.cloud_file.mapper.UserMapper;
import com.liushukov.cloud_file.repository.UserRepository;
import com.liushukov.cloud_file.service.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static com.liushukov.cloud_file.controller.AdminControllerTest.TestResources.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class AdminControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private ObjectMapper objectMapper;
    String jwt;

    @Container
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
        dynamicPropertyRegistry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        dynamicPropertyRegistry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        dynamicPropertyRegistry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @BeforeAll
    static void beforeAll() {
        postgreSQLContainer.start();
    }

    @AfterAll
    static void afterAll() {
        postgreSQLContainer.stop();
    }

    @BeforeEach
    void beforeEach() {
        Optional<User> adminUser = userRepository.findUserById(ADMIN_ID);
        adminUser.ifPresent(value -> jwt = jwtService.generateToken(value));
    }

    private void createUser(String userEmail, UserDto userDto, Role role, boolean enabled) {
        if (userRepository.findUserByEmail(userEmail).isEmpty()) {
            User user = userMapper.toEntity(userDto, role, enabled, new BCryptPasswordEncoder());
            userRepository.save(user);
        }
    }

    private void deleteUser(String userEmail) {
        Optional<User> user = userRepository.findUserByEmail(userEmail);
        user.ifPresent(value -> userRepository.delete(value));
    }

    private void disableUser(String userEmail) {
        Optional<User> user = userRepository.findUserByEmail(userEmail);
        user.ifPresent(value -> userRepository.save(value.setEnabled(false)));
    }

    @Test
    void givenAuthenticationAndUserId_userById_shouldReturnUser() throws Exception {
        createUser(USER_EMAIL, buildUserDto(), Role.USER, true);
        long userId = userRepository.findUserByEmail(USER_EMAIL)
                .map(User::getId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        mockMvc.perform(get(URL_GET_USER_BY_ID + userId)
                .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.fullName").value(USER_FULL_NAME))
                .andExpect(jsonPath("$.email").value(USER_EMAIL))
                .andDo(print());

        Optional<User> userInDb = userRepository.findUserByEmail(USER_EMAIL);
        Assertions.assertTrue(userInDb.isPresent());
        Assertions.assertEquals(USER_FULL_NAME, userInDb.get().getFullName());
        Assertions.assertEquals(USER_EMAIL, userInDb.get().getEmail());
    }

    @Test
    void givenAuthenticationAndInvalidUserId_userById_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get(URL_GET_USER_BY_ID + USER_INVALID_ID)
                .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound())
                .andDo(print());

        Optional<User> userInDb = userRepository.findUserById(USER_INVALID_ID);
        Assertions.assertTrue(userInDb.isEmpty());
    }

    @Test
    void givenAuthentication_allUsers_shouldReturnUsersAscendingList() throws Exception {
        mockMvc.perform(get(URL_GET_ALL_ASCENDING_USERS)
                .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ADMIN_ID))
                .andDo(print());
    }

    @Test
    void givenAuthentication_allUsers_shouldReturnUsersDescendingList() throws Exception {
        mockMvc.perform(get(URL_GET_ALL_DESCENDING_USERS)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ADMIN_ID))
                .andDo(print());
    }

    @Test
    void givenAuthenticationAndUserDto_createUserForAdmin_shouldCreateUserAndReturnUserDto() throws Exception {
        deleteUser(USER_CREATED_BY_ADMIN_EMAIL);

        mockMvc.perform(post(URL_CREATE_USER_BY_ADMIN)
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildUserDtoCreatedByAdmin())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value(USER_CREATED_BY_ADMIN_FULL_NAME))
                .andExpect(jsonPath("$.email").value(USER_CREATED_BY_ADMIN_EMAIL))
                .andDo(print());

        Optional<User> userInDb = userRepository.findUserByEmail(USER_CREATED_BY_ADMIN_EMAIL);
        Assertions.assertTrue(userInDb.isPresent());
        Assertions.assertEquals(USER_CREATED_BY_ADMIN_EMAIL, userInDb.get().getEmail());
        Assertions.assertEquals(USER_CREATED_BY_ADMIN_FULL_NAME, userInDb.get().getFullName());
    }

    @Test
    void givenAuthenticationAndUserDto_createUserForAdmin_shouldReturnConflict() throws Exception {
        createUser(USER_CREATED_BY_ADMIN_EMAIL, buildUserDtoCreatedByAdmin(), Role.USER, true);
        mockMvc.perform(post(URL_CREATE_USER_BY_ADMIN)
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildUserDtoCreatedByAdmin())))
                .andExpect(status().isConflict())
                .andDo(print());
    }

    @Test
    void givenAuthenticationAndUserUpdateDto_updateUserForAdmin_shouldUpdateUserAndReturnUserDto() throws Exception {
        createUser(USER_CREATED_BY_ADMIN_EMAIL, buildUserDtoCreatedByAdmin(), Role.USER, true);
        long userId = userRepository.findUserByEmail(USER_CREATED_BY_ADMIN_EMAIL)
                .map(User::getId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        mockMvc.perform(patch(URL_UPDATE_USER_BY_ADMIN + userId)
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildUserUpdateDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value(USER_CREATED_BY_ADMIN_FULL_NAME))
                .andExpect(jsonPath("$.email").value(USER_CREATED_BY_ADMIN_UPDATED_EMAIL))
                .andDo(print());

        Assertions.assertTrue(userRepository.findUserById(userId).isPresent());
        Assertions.assertEquals(USER_CREATED_BY_ADMIN_UPDATED_EMAIL,
                userRepository.findUserById(userId).get().getEmail());
        Assertions.assertEquals(USER_CREATED_BY_ADMIN_FULL_NAME,
                userRepository.findUserById(userId).get().getFullName());
    }

    @Test
    void givenAuthenticationAndInvalidUserIdForUpdate_updateUserForAdmin_shouldReturnNotFound() throws Exception {
        mockMvc.perform(patch(URL_UPDATE_USER_BY_ADMIN + USER_INVALID_ID)
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildUserUpdateDto())))
                .andExpect(status().isNotFound())
                .andDo(print());

        Assertions.assertTrue(userRepository.findUserById(USER_INVALID_ID).isEmpty());
    }

    @Test
    void givenAuthenticationAndUserId_deleteUserForAdmin_shouldDisableUserAndReturnNoContent() throws Exception {
        createUser(USER_CREATED_BY_ADMIN_EMAIL, buildUserDtoCreatedByAdmin(), Role.USER, true);
        long userId = userRepository.findUserByEmail(USER_CREATED_BY_ADMIN_EMAIL)
                .map(User::getId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        mockMvc.perform(delete(URL_DELETE_USER_BY_ADMIN + userId)
                .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent())
                .andDo(print());

        Assertions.assertTrue(userRepository.findUserById(userId).isPresent());
        Assertions.assertFalse(userRepository.findUserById(userId).get().getEnabled());
    }

    @Test
    void givenAuthenticationAndUserIdForDisabledUser_deleteUserForAdmin_shouldReturnBadRequest() throws Exception {
        if (userRepository.findUserByEmail(USER_CREATED_BY_ADMIN_EMAIL).isEmpty()) {
            createUser(USER_CREATED_BY_ADMIN_EMAIL, buildUserDtoCreatedByAdmin(), Role.USER, false);
        } else {
            disableUser(USER_CREATED_BY_ADMIN_EMAIL);
        }

        long userId = userRepository.findUserByEmail(USER_CREATED_BY_ADMIN_EMAIL)
                .map(User::getId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        mockMvc.perform(delete(URL_DELETE_USER_BY_ADMIN + userId)
                .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isBadRequest())
                .andDo(print());

        Assertions.assertTrue(userRepository.findUserById(userId).isPresent());
        Assertions.assertFalse(userRepository.findUserById(userId).get().getEnabled());
    }

    @Test
    void givenAuthenticationAndInvalidUserIdForDisable_deleteUserForAdmin_shouldReturnNotFound() throws Exception {
        mockMvc.perform(delete(URL_DELETE_USER_BY_ADMIN + USER_INVALID_ID)
                .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound())
                .andDo(print());

        Assertions.assertTrue(userRepository.findUserById(USER_INVALID_ID).isEmpty());
    }

    static class TestResources {
        static final Long ADMIN_ID = 1L;
        static final Long USER_INVALID_ID = 999L;
        static final String USER_FULL_NAME = "test_full_name";
        static final String USER_EMAIL = "test@gmail.com";
        static final String USER_PASSWORD = "test_password";
        static final String USER_CREATED_BY_ADMIN_FULL_NAME = "test_full_name_created_by_admin";
        static final String USER_CREATED_BY_ADMIN_EMAIL = "test_email_created_by_admin@gmail.com";
        static final String USER_CREATED_BY_ADMIN_PASSWORD = "test_password_created_by_admin";
        static final String USER_CREATED_BY_ADMIN_UPDATED_EMAIL = "test_updated_email_created_by_admin@gmail.com";
        static final String URL_GET_USER_BY_ID = "/admin/users/user/";
        static final String URL_GET_ALL_ASCENDING_USERS =
                "/admin/users/all?sortBy=id&orderBy=asc&pageNumber=0&pageSize=1";
        static final String URL_GET_ALL_DESCENDING_USERS =
                "/admin/users/all?sortBy=id&orderBy=desc&pageNumber=0&pageSize=1";
        static final String URL_CREATE_USER_BY_ADMIN = "/admin/users/create";
        static final String URL_UPDATE_USER_BY_ADMIN = "/admin/users/update/";
        static final String URL_DELETE_USER_BY_ADMIN = "/admin/users/delete/";

        static UserDto buildUserDto() {
            return new UserDto(
                    USER_FULL_NAME,
                    USER_EMAIL,
                    USER_PASSWORD
            );
        }

        static UserDto buildUserDtoCreatedByAdmin() {
            return new UserDto(
                    USER_CREATED_BY_ADMIN_FULL_NAME,
                    USER_CREATED_BY_ADMIN_EMAIL,
                    USER_CREATED_BY_ADMIN_PASSWORD
            );
        }

        static UserUpdateDto buildUserUpdateDto() {
            return new UserUpdateDto(
                    null,
                    USER_CREATED_BY_ADMIN_UPDATED_EMAIL,
                    null
            );
        }
    }
}
