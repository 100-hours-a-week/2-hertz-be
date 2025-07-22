package com.hertz.hertz_be.global.config;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;

@Testcontainers
public abstract class TestContainersConfig {

    @Container
    static final MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0.32")
            .withDatabaseName("testdb")
            .withUsername("testUser")
            .withPassword("testPW");

    @Container
    static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:6.2")
            .withExposedPorts(6379)
            .withCommand("redis-server --requirepass testpass")
            .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        // MySQL
        registry.add("spring.datasource.url",    mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);

        // Redis
        registry.add("spring.redis.host",     redisContainer::getHost);
        registry.add("spring.redis.port",     () -> redisContainer.getMappedPort(6379));
        registry.add("spring.redis.password", () -> "testpass");
    }
}
