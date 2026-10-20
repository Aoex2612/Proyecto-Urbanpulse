package org.bulbaseros.exception;

import org.bulbaseros.dto.ErrorResponse;
import org.bulbaseros.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para el manejador global de excepciones {@link GlobalExceptionHandler}.
 * <p>
 * Valida la captura de excepciones, asignación correcta de códigos de estado HTTP
 * (400, 401, 409, 500) y estructuración del objeto de respuesta {@link ErrorResponse}.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/register");
    }

    /**
     * Verifica que los fallos de validación MethodArgumentNotValidException retornen HTTP 400
     * con los campos infractores mapeados en el cuerpo.
     *
     * @throws Exception en caso de error de reflexión.
     */
    @Test
    @DisplayName("Debería retornar HTTP 400 Bad Request con mapa de errores ante fallo de validación")
    void shouldHandleMethodArgumentNotValidException() throws Exception {
        RegisterRequest target = new RegisterRequest();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "registerRequest");
        bindingResult.addError(new FieldError("registerRequest", "email", "El correo electrónico es obligatorio"));
        bindingResult.addError(new FieldError("registerRequest", "password", "La contraseña es obligatoria"));

        Method dummyMethod = getClass().getDeclaredMethod("dummyMethodForReflection", RegisterRequest.class);
        MethodParameter parameter = new MethodParameter(dummyMethod, 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationExceptions(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("/api/auth/register", response.getBody().getPath());
        assertNotNull(response.getBody().getErrors());
        assertEquals(2, response.getBody().getErrors().size());
        assertEquals("El correo electrónico es obligatorio", response.getBody().getErrors().get("email"));
        assertEquals("La contraseña es obligatoria", response.getBody().getErrors().get("password"));
    }

    /**
     * Verifica que EmailAlreadyExistsException retorne HTTP 409 Conflict.
     */
    @Test
    @DisplayName("Debería retornar HTTP 409 Conflict ante EmailAlreadyExistsException")
    void shouldHandleEmailAlreadyExistsException() {
        EmailAlreadyExistsException ex = new EmailAlreadyExistsException("El correo test@urbanpulse.es ya está en uso");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleEmailAlreadyExists(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Conflict", response.getBody().getError());
        assertEquals("El correo test@urbanpulse.es ya está en uso", response.getBody().getMessage());
        assertEquals("/api/auth/register", response.getBody().getPath());
    }

    /**
     * Verifica que InvalidCredentialsException retorne HTTP 401 Unauthorized con mensaje genérico.
     */
    @Test
    @DisplayName("Debería retornar HTTP 401 Unauthorized ante InvalidCredentialsException")
    void shouldHandleInvalidCredentialsException() {
        request.setRequestURI("/api/auth/login");
        InvalidCredentialsException ex = new InvalidCredentialsException();

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAuthenticationException(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().getStatus());
        assertEquals("Unauthorized", response.getBody().getError());
        assertEquals("Credenciales inválidas", response.getBody().getMessage());
        assertEquals("/api/auth/login", response.getBody().getPath());
    }

    /**
     * Verifica que BadCredentialsException de Spring Security retorne HTTP 401 Unauthorized.
     */
    @Test
    @DisplayName("Debería retornar HTTP 401 Unauthorized ante BadCredentialsException de Spring Security")
    void shouldHandleBadCredentialsException() {
        request.setRequestURI("/api/auth/login");
        BadCredentialsException ex = new BadCredentialsException("Bad credentials internally");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAuthenticationException(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().getStatus());
        assertEquals("Unauthorized", response.getBody().getError());
        assertEquals("Credenciales inválidas", response.getBody().getMessage());
        assertEquals("/api/auth/login", response.getBody().getPath());
    }

    /**
     * Verifica que cualquier otra excepción no controlada retorne HTTP 500 Internal Server Error.
     */
    @Test
    @DisplayName("Debería retornar HTTP 500 Internal Server Error ante excepciones genéricas")
    void shouldHandleGenericException() {
        RuntimeException ex = new RuntimeException("Error inesperado en base de datos");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Internal Server Error", response.getBody().getError());
        assertEquals("Ha ocurrido un error interno en el servidor", response.getBody().getMessage());
        assertEquals("/api/auth/register", response.getBody().getPath());
    }

    /**
     * Método auxiliar utilizado para suministrar un {@link MethodParameter} al constructor de {@link MethodArgumentNotValidException}.
     *
     * @param request Parámetro simulado.
     */
    @SuppressWarnings("unused")
    private void dummyMethodForReflection(RegisterRequest request) {
        // Método de reflexión auxiliar para tests
    }
}
