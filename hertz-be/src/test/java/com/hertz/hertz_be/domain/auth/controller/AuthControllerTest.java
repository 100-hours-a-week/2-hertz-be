package com.hertz.hertz_be.domain.auth.controller;

import com.hertz.hertz_be.domain.auth.repository.RefreshTokenRepository;
import com.hertz.hertz_be.domain.auth.service.AuthService;
import com.hertz.hertz_be.domain.user.service.UserService;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@RequiredArgsConstructor
@Testcontainers
@ActiveProfiles("test")
public class AuthControllerTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.32")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    private final MockMvc mockMvc;

    private final AuthService authTokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenService;
    private final UserService userService;
    private final AuthService authService;
}
