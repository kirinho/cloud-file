package com.liushukov.cloud_file.controller;

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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.liushukov.cloud_file.controller.UserControllerTest.TestResources.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class UserControllerTest {
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
    User user;

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
        user = userMapper.toEntity(buildUserDto(), Role.USER, true, new BCryptPasswordEncoder());
        userRepository.save(user);
    }

    @Test
    void givenAuthentication_me_shouldValidateJwtAndReturnUser() throws Exception {
        jwt = jwtService.generateToken(user);
        mockMvc.perform(get(URL_ME)
                .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value(USER_FULL_NAME))
                .andExpect(jsonPath("$.email").value(USER_EMAIL))
                .andDo(print());
    }

    @Test
    void givenInvalidAuthentication_me_shouldNotValidateAndReturnInternalServerError() throws Exception {
        mockMvc.perform(get(URL_ME)
                .header("Authorization",
                        "Bearer sdlfklksjfsdnfdsflsdflsjfnsndfnsnfdsdfsmfnsknlfdsmfmsmfnsklfnsfnsfdsmfdsmf"))
                .andExpect(status().isInternalServerError())
                .andDo(print());
    }

    @Test
    void givenExpiredJwt_me_shouldNotValidateAndReturnForbidden() throws Exception {
        mockMvc.perform(get(URL_ME)
                        .header("Authorization",
                                "Bearer " + JWT_EXPIRED))
                .andExpect(status().isForbidden())
                .andDo(print());
    }

    @Test
    void givenInvalidJwtSignature_me_shouldNotValidateAndReturnForbidden() throws Exception {
        mockMvc.perform(get(URL_ME)
                        .header("Authorization",
                                "Bearer " + JWT_INVALID_SIGNATURE))
                .andExpect(status().isForbidden())
                .andDo(print());
    }

    @Test
    void givenAuthenticationAndUserUpdateDto_updateUser_shouldValidateJwtAndUpdateUserAndReturnUserDto()
            throws Exception {
        jwt = jwtService.generateToken(user);
        mockMvc.perform(patch(URL_UPDATE)
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildUserUpdateDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value(USER_FULL_NAME))
                .andExpect(jsonPath("$.email").value(USER_UPDATED_EMAIL))
                .andDo(print());
    }

    @Test
    void givenAuthentication_deleteUser_shouldValidateJwtAndDisableStatus() throws Exception {
        jwt = jwtService.generateToken(user);
        mockMvc.perform(delete(URL_DELETE)
                .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent())
                .andDo(print());
    }

    @Test
    void givenAuthenticationAndDisabledStatus_deleteUser_shouldValidateAndReturnBadRequest() throws Exception {
        user.setEnabled(false);
        userRepository.save(user);
        jwt = jwtService.generateToken(user);
        mockMvc.perform(delete(URL_DELETE)
                .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    static class TestResources {
        static final String USER_FULL_NAME = "test_full_name";
        static final String USER_EMAIL = "test@gmail.com";
        static final String USER_UPDATED_EMAIL = "test_updated@gmail.com";
        static final String USER_PASSWORD = "test_password";
        static final String JWT_EXPIRED = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGdtYWlsLmNvbSIsImlhdCI6MTczOTUzM" +
                "TA2NSwiZXhwIjoxNzM5NTMxMDY1fQ.E5VRDloLqp6PKhbpGpLyR54VaY1vJ1vqEh6a9Qwfo9Y";
        static final String JWT_INVALID_SIGNATURE = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGdtYWlsLmNvbSIsImlhdCI6" +
                "MTczOTE4OTM0MywiZXhwIjoxNzM5MTkwMzQzfQ.Rjm_r0QydKS2ShCPAvt6UtF";
        static final String URL_ME = "/users/me";
        static final String URL_UPDATE = "/users/update";
        static final String URL_DELETE = "/users/delete";

        static UserDto buildUserDto() {
            return new UserDto(
                    USER_FULL_NAME,
                    USER_EMAIL,
                    USER_PASSWORD
            );
        }

        static UserUpdateDto buildUserUpdateDto() {
            return new UserUpdateDto(
                    null,
                    USER_UPDATED_EMAIL,
                    null
            );
        }
    }
}
