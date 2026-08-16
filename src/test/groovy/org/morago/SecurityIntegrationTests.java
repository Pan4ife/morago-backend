package org.morago;

import org.junit.jupiter.api.BeforeEach;
import org.morago.model.Role;
import org.morago.model.RoleName;
import org.morago.model.User;
import org.morago.repository.RoleRepository;
import org.morago.repository.UserRepository;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureMockMvc

class SecurityIntegrationTests {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;

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
    @Test
    void clientCannotCreateLanguage() throws Exception{
        String token = obtainAccessToken("test@morago.com", "password123");

        mockMvc.perform(post("/languages")
                .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                .content("{\"name\": \"French\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateLanguage() throws Exception{
        String token = obtainAccessTokenWithRole("admin@morago.com", "password123", RoleName.ADMIN);

        mockMvc.perform(post("/languages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("{\"name\": \"French\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void interpreterCannotCreateLanguage() throws Exception{
        String token = obtainAccessTokenWithRole("translator@morago.com", "password123", RoleName.TRANSLATOR);

        mockMvc.perform(post("/languages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("{\"name\": \"French\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() throws Exception{
        String token = obtainRefreshToken("refresh@morago.com", "password123");

        mockMvc.perform(get("/translator-profile/me")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private String obtainAccessToken(String email, String password) throws Exception {

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}"));


        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = new ObjectMapper().readTree(responseBody);
        return json.get("accessToken").asText();
    }

    private String obtainRefreshToken(String email, String password) throws Exception {

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}"));


        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = new ObjectMapper().readTree(responseBody);
        return json.get("refreshToken").asText();
    }

    private String obtainAccessTokenWithRole(String email, String password, RoleName roleName) throws Exception{
        String token = obtainAccessToken(email, password);
        User user = userRepository.findByEmail(email).orElseThrow();
        Role role = roleRepository.findByName(roleName).orElseThrow();
        user.getRoles().add(role);
        userRepository.save(user);

        return token;
    }

    @BeforeEach
    void setUpRoles() {
        if (roleRepository.findByName(RoleName.USER).isEmpty()) {
            Role userRole = new Role();
            userRole.setName(RoleName.USER);
            roleRepository.save(userRole);
        }
        if (roleRepository.findByName(RoleName.TRANSLATOR).isEmpty()) {
            Role translatorRole = new Role();
            translatorRole.setName(RoleName.TRANSLATOR);
            roleRepository.save(translatorRole);
        }
        if (roleRepository.findByName(RoleName.ADMIN).isEmpty()) {
            Role adminRole = new Role();
            adminRole.setName(RoleName.ADMIN);
            roleRepository.save(adminRole);
        }
    }
}
