package org.morago;

import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureMockMvc

class SecurityIntegrationTests {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername );
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousCannotCreateLanguage() throws Exception {
        mockMvc.perform(post("/languages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"French\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousCannotDeleteLanguage() throws Exception{
        mockMvc.perform(delete("/languages/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousCannotCreateTopics() throws Exception{
        mockMvc.perform(post("/topics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousCannotDeleteTopics() throws Exception{
        mockMvc.perform(delete("/topics/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"French\"}"))
                .andExpect(status().isUnauthorized());
    }
}
