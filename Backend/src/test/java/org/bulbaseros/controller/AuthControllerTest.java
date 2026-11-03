package org.bulbaseros.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bulbaseros.config.SecurityConfig;
import org.bulbaseros.dto.AuthResponse;
import org.bulbaseros.dto.LoginRequest;
import org.bulbaseros.dto.RegisterRequest;
import org.bulbaseros.dto.RegisterResponse;
import org.bulbaseros.exception.EmailAlreadyExistsException;
import org.bulbaseros.exception.GlobalExceptionHandler;
import org.bulbaseros.exception.InvalidCredentialsException;
import org.bulbaseros.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración web para el controlador REST {@link AuthController}.
 * <p>
 * Valida los contratos HTTP, serialización de payloads y respuestas de código de estado (201, 200, 400, 401, 409).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    /**
     * Valida que el registro exitoso retorne HTTP 201 Created con el payload esperado.
     */
    @Test
    @DisplayName("POST /api/auth/register con datos válidos retorna HTTP 201 Created")
    void shouldReturn201WhenRegistrationSuccessful() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Juan Perez");
        request.setEmail("juan@urbanpulse.es");
        request.setPassword("Password123");

        RegisterResponse response = new RegisterResponse("Usuario registrado exitosamente", "juan@urbanpulse.es");
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Usuario registrado exitosamente"))
                .andExpect(jsonPath("$.email").value("juan@urbanpulse.es"));
    }

    /**
     * Valida que el registro con correo ya existente retorne HTTP 409 Conflict.
     */
    @Test
    @DisplayName("POST /api/auth/register con email existente retorna HTTP 409 Conflict")
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Juan Perez");
        request.setEmail("duplicado@urbanpulse.es");
        request.setPassword("Password123");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("El correo electrónico ya está registrado."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("El correo electrónico ya está registrado."));
    }

    /**
     * Valida que datos de registro inválidos retornen HTTP 400 Bad Request.
     */
    @Test
    @DisplayName("POST /api/auth/register con campos inválidos retorna HTTP 400 Bad Request")
    void shouldReturn400WhenRegisterPayloadInvalid() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName(""); // Vacío
        request.setEmail("email-invalido");
        request.setPassword("corta");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.fullName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    /**
     * Valida que el registro retorne HTTP 400 si el nombre completo supera los 150 caracteres.
     */
    @Test
    @DisplayName("POST /api/auth/register con fullName superior a 150 caracteres retorna HTTP 400 Bad Request")
    void shouldReturn400WhenFullNameExceeds150Characters() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("A".repeat(151));
        request.setEmail("usuario.valido@urbanpulse.es");
        request.setPassword("Password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.fullName").exists());
    }

    /**
     * Valida que el registro retorne HTTP 400 si el correo electrónico supera los 150 caracteres.
     */
    @Test
    @DisplayName("POST /api/auth/register con email superior a 150 caracteres retorna HTTP 400 Bad Request")
    void shouldReturn400WhenEmailExceeds150Characters() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Nombre Valido");
        request.setEmail("a".repeat(140) + "@urbanpulse.es"); // 154 caracteres
        request.setPassword("Password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.email").exists());
    }

    /**
     * Valida que el inicio de sesión con campos en blanco retorne HTTP 400 Bad Request.
     */
    @Test
    @DisplayName("POST /api/auth/login con campos en blanco retorna HTTP 400 Bad Request")
    void shouldReturn400WhenLoginPayloadInvalid() throws Exception {
        LoginRequest request = new LoginRequest("", "");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    /**
     * Valida que el inicio de sesión exitoso retorne HTTP 200 OK con el token y metadatos.
     */
    @Test
    @DisplayName("POST /api/auth/login con credenciales válidas retorna HTTP 200 OK")
    void shouldReturn200WhenLoginSuccessful() throws Exception {
        LoginRequest request = new LoginRequest("juan@urbanpulse.es", "Password123");
        AuthResponse response = new AuthResponse("mock.jwt.token", "Bearer", 86400L, "CITIZEN", "Juan Perez");

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400))
                .andExpect(jsonPath("$.role").value("CITIZEN"))
                .andExpect(jsonPath("$.fullName").value("Juan Perez"));
    }

    /**
     * Valida que credenciales inválidas retornen HTTP 401 Unauthorized.
     */
    @Test
    @DisplayName("POST /api/auth/login con credenciales inválidas retorna HTTP 401 Unauthorized")
    void shouldReturn401WhenInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("juan@urbanpulse.es", "PasswordErronea");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Credenciales inválidas"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }
}
