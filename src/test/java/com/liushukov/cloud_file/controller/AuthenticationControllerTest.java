package com.liushukov.cloud_file.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liushukov.cloud_file.dto.UserDto;
import com.liushukov.cloud_file.dto.UserLoginDto;
import com.liushukov.cloud_file.entity.Role;
import com.liushukov.cloud_file.entity.User;
import com.liushukov.cloud_file.mapper.UserMapper;
import com.liushukov.cloud_file.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.liushukov.cloud_file.controller.AuthenticationControllerTest.TestResources.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class AuthenticationControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ObjectMapper objectMapper;

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
        userRepository.deleteAll();
    }

    @Test
    void givenUserDto_registration_shouldSaveUserAndReturnDto() throws Exception {
        mockMvc.perform(post(URL_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildUserDto())))
                .andExpect(status().isCreated())
                .andDo(print())
                .andExpect(jsonPath("$.fullName").value(USER_FULL_NAME))
                .andExpect(jsonPath("$.email").value(USER_EMAIL));

        Assertions.assertEquals(1, userRepository.findAll().size());
        Assertions.assertTrue(userRepository.findUserByEmail(USER_EMAIL).isPresent());
        Assertions.assertEquals(USER_EMAIL, userRepository.findUserByEmail(USER_EMAIL).get().getEmail());
    }

    @Test
    void givenUserDto_registration_shouldNotSaveUserDueToConflict() throws Exception {
        User user = userMapper.toEntity(buildUserDto(), Role.USER, true, new BCryptPasswordEncoder());
        userRepository.save(user);
        mockMvc.perform(post(URL_REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildUserDto())))
                .andExpect(status().isConflict())
                .andDo(print());

        Assertions.assertEquals(1, userRepository.findAll().size()); // due to inserted user at the beginning of the method
    }

    @Test
    void givenUserLoginDto_login_shouldAuthenticateUserAndReturnJwt() throws Exception {
        User user = userMapper.toEntity(buildUserDto(), Role.USER, true, new BCryptPasswordEncoder());
        userRepository.save(user);
        mockMvc.perform(post(URL_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildUserLoginDto())))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    void givenDisabledUser_login_shouldReturnForbidden() throws Exception {
        User user = userMapper.toEntity(buildUserDto(), Role.USER, false, new BCryptPasswordEncoder());
        userRepository.save(user);
        mockMvc.perform(post(URL_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildUserDto())))
                .andExpect(status().isForbidden())
                .andDo(print());
    }

    static class TestResources {
        static final String USER_FULL_NAME = "test_full_name";
        static final String USER_EMAIL = "test@gmail.com";
        static final String USER_PASSWORD = "test_password";
        static final String URL_REGISTER = "/auth/register";
        static final String URL_LOGIN = "/auth/login";

        static UserDto buildUserDto() {
            return new UserDto(
                    USER_FULL_NAME,
                    USER_EMAIL,
                    USER_PASSWORD
            );
        }

        static UserLoginDto buildUserLoginDto() {
            return new UserLoginDto(
                    USER_EMAIL,
                    USER_PASSWORD
            );
        }
    }
}
