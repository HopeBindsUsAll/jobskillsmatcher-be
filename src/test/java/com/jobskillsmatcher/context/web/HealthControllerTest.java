package com.jobskillsmatcher.context.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobskillsmatcher.auditlog.AuditLogService;
import com.jobskillsmatcher.auth.AuthRepository;
import com.jobskillsmatcher.auth.AuthService;
import com.jobskillsmatcher.user.AdminProfileRepository;
import com.jobskillsmatcher.user.StudentProfileRepository;
import com.jobskillsmatcher.user.UserRepository;
import com.jobskillsmatcher.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@Slf4j
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(
        controllers = HealthController.class,
        includeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.jobskillsmatcher.context.web.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.jobskillsmatcher.context.security.*")
        }
)
class HealthControllerTest {

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    StudentProfileRepository studentProfileRepository;

    @MockitoBean
    AdminProfileRepository adminProfileRepository;

    @MockitoBean
    PasswordEncoder passwordEncoder;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    AuthRepository authRepository;

    @MockitoBean
    UserService userService;

    @MockitoBean
    AuditLogService auditLogService;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void successHealthCheck() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/health")
        ).andDo(print()).andExpectAll(
                MockMvcResultMatchers.status().is2xxSuccessful(),
                MockMvcResultMatchers.jsonPath("$.status").value("ok"),
                MockMvcResultMatchers.jsonPath("$.service").value("jobskillsmatcher"),
                MockMvcResultMatchers.jsonPath("$.timestamp").isNotEmpty()
        );
    }
}
