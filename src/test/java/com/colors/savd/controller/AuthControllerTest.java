package com.colors.savd.controller;

import com.colors.savd.dto.auth.PasswordResetConfirmDTO;
import com.colors.savd.dto.auth.PasswordResetRequestDTO;
import com.colors.savd.exception.BusinessException;
import com.colors.savd.exception.GlobalExceptionHandler;
import com.colors.savd.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false) // desactiva filtros de Spring Security en tests MVC
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/auth/forgot-password -> 200 y llama a issueResetToken")
    void forgotPassword_ok() throws Exception {
        PasswordResetRequestDTO req = new PasswordResetRequestDTO();
        req.setEmail("user@example.com");

        doNothing().when(authService).issueResetToken(req.getEmail());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isOk())
               .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$.status").value("ok"));

        verify(authService).issueResetToken("user@example.com");
    }

    @Test
    @DisplayName("GET /api/auth/verify-reset?token=... -> 200 valid=true")
    void verifyReset_valid_true() throws Exception {
        String token = "tok-123";
        given(authService.verifyResetToken(token)).willReturn(true);

        mockMvc.perform(get("/api/auth/verify-reset").param("token", token))
               .andExpect(status().isOk())
               .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    @DisplayName("GET /api/auth/verify-reset?token=... -> 200 valid=false")
    void verifyReset_valid_false() throws Exception {
        String token = "bad-token";
        given(authService.verifyResetToken(token)).willReturn(false);

        mockMvc.perform(get("/api/auth/verify-reset").param("token", token))
               .andExpect(status().isOk())
               .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    @DisplayName("POST /api/auth/reset-password -> 200 y llama a resetPassword")
    void resetPassword_ok() throws Exception {
        PasswordResetConfirmDTO req = new PasswordResetConfirmDTO();
        req.setToken("tok-456");
        req.setNewPassword("Abcdef1!");

        doNothing().when(authService).resetPassword(req.getToken(), req.getNewPassword());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isOk())
               .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$.status").value("ok"));

        verify(authService).resetPassword("tok-456", "Abcdef1!");
    }

    @Test
    @DisplayName("POST /api/auth/reset-password -> 400 cuando el servicio lanza BusinessException")
    void resetPassword_businessError() throws Exception {
        PasswordResetConfirmDTO req = new PasswordResetConfirmDTO();
        req.setToken("expired-token");
        req.setNewPassword("Abcdef1!");

        Mockito.doThrow(new BusinessException("El token ha expirado"))
               .when(authService).resetPassword(req.getToken(), req.getNewPassword());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isBadRequest())
               .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$.mensaje").value("El token ha expirado"))
               .andExpect(jsonPath("$.code").value("BUSINESS"));
    }
}
