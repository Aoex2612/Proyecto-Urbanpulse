package org.bulbaseros.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias de Bean Validation para los DTOs de autenticación y transferencia de datos.
 * <p>
 * Verifica restricciones de validación sobre {@link RegisterRequest} y {@link LoginRequest},
 * así como la correcta integridad estructural de {@link RegisterResponse}, {@link AuthResponse}
 * y {@link ErrorResponse}.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class ValidationDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    /**
     * Verifica que un RegisterRequest válido no produzca violaciones de restricciones.
     */
    @Test
    @DisplayName("Debería validar exitosamente un RegisterRequest válido")
    void shouldValidateValidRegisterRequest() {
        RegisterRequest request = new RegisterRequest("Carlos Gómez", "carlos@urbanpulse.es", "Pass1234");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "No deben existir violaciones para datos válidos");
    }

    /**
     * Verifica que RegisterRequest falle si el nombre es nulo o vacío.
     */
    @Test
    @DisplayName("Debería fallar validación si el nombre completo es nulo o está en blanco")
    void shouldFailWhenFullNameIsBlank() {
        RegisterRequest request = new RegisterRequest("   ", "carlos@urbanpulse.es", "Pass1234");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("fullName")));
    }

    /**
     * Verifica que RegisterRequest falle si el nombre completo supera 150 caracteres.
     */
    @Test
    @DisplayName("Debería fallar validación si el nombre completo supera 150 caracteres")
    void shouldFailWhenFullNameExceeds150Chars() {
        String longName = "A".repeat(151);
        RegisterRequest request = new RegisterRequest(longName, "carlos@urbanpulse.es", "Pass1234");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("fullName")));
    }

    /**
     * Verifica que RegisterRequest falle si el correo es nulo, en blanco o con formato inválido.
     */
    @Test
    @DisplayName("Debería fallar validación si el correo no tiene formato válido")
    void shouldFailWhenEmailIsInvalid() {
        RegisterRequest request = new RegisterRequest("Carlos Gómez", "formato-invalido", "Pass1234");
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    /**
     * Verifica que RegisterRequest falle si la contraseña no cumple la política de complejidad (mínimo 8 caracteres, letra y número).
     */
    @Test
    @DisplayName("Debería fallar validación si la contraseña no cumple longitud o complejidad")
    void shouldFailWhenPasswordDoesNotMeetRequirements() {
        // Menos de 8 caracteres
        RegisterRequest shortPass = new RegisterRequest("Carlos", "carlos@urbanpulse.es", "Pass1");
        assertFalse(validator.validate(shortPass).isEmpty());

        // Sin números
        RegisterRequest noDigit = new RegisterRequest("Carlos", "carlos@urbanpulse.es", "PasswordSolamente");
        assertFalse(validator.validate(noDigit).isEmpty());

        // Sin letras
        RegisterRequest noLetter = new RegisterRequest("Carlos", "carlos@urbanpulse.es", "1234567890");
        assertFalse(validator.validate(noLetter).isEmpty());
    }

    /**
     * Verifica que LoginRequest valide exitosamente credenciales con formato adecuado.
     */
    @Test
    @DisplayName("Debería validar exitosamente un LoginRequest válido")
    void shouldValidateValidLoginRequest() {
        LoginRequest request = new LoginRequest("usuario@urbanpulse.es", "MiClave123");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    /**
     * Verifica que LoginRequest falle si faltan correo o contraseña.
     */
    @Test
    @DisplayName("Debería fallar LoginRequest cuando los campos están en blanco")
    void shouldFailLoginRequestWhenFieldsAreBlank() {
        LoginRequest request = new LoginRequest("", "");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    /**
     * Verifica que LoginRequest falle si el correo tiene un formato inválido.
     */
    @Test
    @DisplayName("Debería fallar LoginRequest cuando el correo tiene formato inválido")
    void shouldFailLoginRequestWhenEmailIsInvalid() {
        LoginRequest request = new LoginRequest("correo-invalido", "Password123");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    /**
     * Verifica la correcta inicialización y getters/setters de RegisterResponse.
     */
    @Test
    @DisplayName("Debería instanciar y acceder a los campos de RegisterResponse")
    void shouldHandleRegisterResponse() {
        RegisterResponse response = new RegisterResponse("Usuario registrado exitosamente", "test@urbanpulse.es");

        assertEquals("Usuario registrado exitosamente", response.getMessage());
        assertEquals("test@urbanpulse.es", response.getEmail());

        response.setMessage("Nuevo mensaje");
        response.setEmail("nuevo@urbanpulse.es");

        assertEquals("Nuevo mensaje", response.getMessage());
        assertEquals("nuevo@urbanpulse.es", response.getEmail());
    }

    /**
     * Verifica la correcta inicialización y getters/setters de AuthResponse.
     */
    @Test
    @DisplayName("Debería instanciar y acceder a los campos de AuthResponse")
    void shouldHandleAuthResponse() {
        AuthResponse response = new AuthResponse("tokenJwtValido", 86400L, "CITIZEN", "Ana Ruiz");

        assertEquals("tokenJwtValido", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86400L, response.getExpiresIn());
        assertEquals("CITIZEN", response.getRole());
        assertEquals("Ana Ruiz", response.getFullName());

        response.setToken("otroToken");
        response.setTokenType("Custom");
        response.setExpiresIn(3600L);
        response.setRole("ADMIN");
        response.setFullName("Admin Root");

        assertEquals("otroToken", response.getToken());
        assertEquals("Custom", response.getTokenType());
        assertEquals(3600L, response.getExpiresIn());
        assertEquals("ADMIN", response.getRole());
        assertEquals("Admin Root", response.getFullName());
    }

    /**
     * Verifica la correcta inicialización y getters/setters de ErrorResponse.
     */
    @Test
    @DisplayName("Debería instanciar y acceder a los campos de ErrorResponse")
    void shouldHandleErrorResponse() {
        OffsetDateTime now = OffsetDateTime.now();
        Map<String, String> fieldErrors = Map.of("email", "Formato inválido");
        ErrorResponse errorResponse = new ErrorResponse(400, "Bad Request", "Error de validación", "/api/auth/register", fieldErrors);

        assertEquals(400, errorResponse.getStatus());
        assertEquals("Bad Request", errorResponse.getError());
        assertEquals("Error de validación", errorResponse.getMessage());
        assertEquals("/api/auth/register", errorResponse.getPath());
        assertEquals(fieldErrors, errorResponse.getErrors());
        assertNotNull(errorResponse.getTimestamp());

        errorResponse.setStatus(500);
        errorResponse.setError("Internal Server Error");
        errorResponse.setMessage("Fallo interno");
        errorResponse.setPath("/api/test");
        errorResponse.setTimestamp(now);
        errorResponse.setErrors(null);

        assertEquals(500, errorResponse.getStatus());
        assertEquals("Internal Server Error", errorResponse.getError());
        assertEquals("Fallo interno", errorResponse.getMessage());
        assertEquals("/api/test", errorResponse.getPath());
        assertEquals(now, errorResponse.getTimestamp());
        assertEquals(null, errorResponse.getErrors());
    }
}
