package com.wasel.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiSmokeTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerEndpointIssuesTokens() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "citizen@example.com",
                                  "password": "ChangeMe123!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("citizen@example.com"));
    }

    @Test
    void publicReportSubmissionIsAccepted() throws Exception {
        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Client-Fingerprint", "smoke-suite")
                        .content("""
                                {
                                  "latitude": 31.7683,
                                  "longitude": 35.2137,
                                  "category": "DELAY",
                                  "description": "Traffic backup observed near the main road junction."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").isNotEmpty())
                .andExpect(jsonPath("$.category").value("DELAY"));
    }
}
