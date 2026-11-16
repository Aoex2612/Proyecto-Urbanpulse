package org.bulbaseros.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.bulbaseros.dto.ErrorResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para {@link JwtAuthenticationEntryPoint}.
 * <p>
 * Verifica la captura de fallos de autenticación (401 Unauthorized), la serialización
 * del cuerpo JSON según {@link ErrorResponse}, y el registro adecuado de {@link JavaTimeModule}.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class JwtAuthenticationEntryPointTest {

    private JwtAuthenticationEntryPoint entryPoint;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private ObjectMapper objectMapper;
    private ListAppender<ILoggingEvent> logbackAppender;

    @BeforeEach
    void setUp() {
        entryPoint = new JwtAuthenticationEntryPoint();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        Logger logger = (Logger) LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);
        logbackAppender = new ListAppender<>();
        logbackAppender.start();
        logger.addAppender(logbackAppender);
        logger.setLevel(Level.DEBUG);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);
        logger.detachAppender(logbackAppender);
        logbackAppender.stop();
    }

    @Test
    @DisplayName("Debería responder HTTP 401 con JSON normalizado ErrorResponse ante petición no autenticada")
    void shouldReturn401WithStandardErrorResponseWhenCommenceIsInvoked() throws Exception {
        request.setRequestURI("/api/incidents/1/status");
        InsufficientAuthenticationException authException =
                new InsufficientAuthenticationException("Full authentication is required to access this resource");

        entryPoint.commence(request, response, authException);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertTrue(response.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE));
        assertEquals("UTF-8", response.getCharacterEncoding());

        String responseBody = response.getContentAsString();
        assertNotNull(responseBody);
        assertFalse(responseBody.isBlank());

        ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);
        assertEquals(401, errorResponse.getStatus());
        assertEquals("Unauthorized", errorResponse.getError());
        assertEquals("Token de autenticación ausente o inválido", errorResponse.getMessage());
        assertEquals("/api/incidents/1/status", errorResponse.getPath());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    @DisplayName("Debería incluir mensaje personalizado si se especifica en el atributo del request")
    void shouldIncludeCustomMessageFromRequestAttribute() throws Exception {
        request.setRequestURI("/api/protected");
        request.setAttribute("auth_error_message", "Token de autenticación expirado o inválido");

        entryPoint.commence(request, response, new BadCredentialsException("Bad credentials"));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());

        String responseBody = response.getContentAsString();
        ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);
        assertEquals(401, errorResponse.getStatus());
        assertEquals("Unauthorized", errorResponse.getError());
        assertEquals("Token de autenticación expirado o inválido", errorResponse.getMessage());
        assertEquals("/api/protected", errorResponse.getPath());
    }

    @Test
    @DisplayName("Debería incluir mensaje si la excepción tiene un mensaje de token expirado o inválido")
    void shouldIncludeMessageWhenExceptionHasExpiredTokenMessage() throws Exception {
        request.setRequestURI("/api/protected");
        BadCredentialsException exception = new BadCredentialsException("Token de autenticación expirado o inválido");

        entryPoint.commence(request, response, exception);

        String responseBody = response.getContentAsString();
        ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);
        assertEquals(401, errorResponse.getStatus());
        assertEquals("Token de autenticación expirado o inválido", errorResponse.getMessage());
    }

    @Test
    @DisplayName("Debería serializar OffsetDateTime como texto ISO-8601 en el cuerpo JSON")
    void shouldSerializeOffsetDateTimeAsIsoString() throws Exception {
        request.setRequestURI("/api/test");

        entryPoint.commence(request, response, new BadCredentialsException("Invalid"));

        String responseBody = response.getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);

        assertTrue(jsonNode.has("timestamp"));
        assertTrue(jsonNode.get("timestamp").isTextual(), "El campo timestamp debe ser una cadena ISO-8601");
        assertEquals(401, jsonNode.get("status").asInt());
        assertEquals("Unauthorized", jsonNode.get("error").asText());
    }

    @Test
    @DisplayName("Debería soportar inicialización con constructor parametrizado y ObjectMapper nulo")
    void shouldSupportParameterizedConstructorAndNullObjectMapper() throws Exception {
        JwtAuthenticationEntryPoint entryPointWithNull = new JwtAuthenticationEntryPoint(null);
        request.setRequestURI("/api/null-test");

        entryPointWithNull.commence(request, response, new BadCredentialsException("Test"));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        ErrorResponse errorResponse = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertEquals(401, errorResponse.getStatus());
        assertEquals("/api/null-test", errorResponse.getPath());
    }

    @Test
    @DisplayName("Debería soportar inicialización con ObjectMapper preconfigurado")
    void shouldSupportPreconfiguredObjectMapper() throws Exception {
        ObjectMapper customMapper = new ObjectMapper();
        JwtAuthenticationEntryPoint customEntryPoint = new JwtAuthenticationEntryPoint(customMapper);
        request.setRequestURI("/api/custom-test");

        customEntryPoint.commence(request, response, new BadCredentialsException("Test"));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        ErrorResponse errorResponse = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertEquals(401, errorResponse.getStatus());
        assertEquals("/api/custom-test", errorResponse.getPath());
    }

    @Test
    @DisplayName("Debería registrar evento de autenticacion fallido con nivel WARN e incluir la URI de la peticion")
    void shouldLogAuthenticationFailureWithWarnLevelAndRequestUri() throws Exception {
        request.setRequestURI("/api/incidents/1/status");
        InsufficientAuthenticationException authException =
                new InsufficientAuthenticationException("Full authentication is required to access this resource");

        entryPoint.commence(request, response, authException);

        List<ILoggingEvent> events = logbackAppender.list;
        assertEquals(1, events.size());

        ILoggingEvent event = events.get(0);
        assertEquals(Level.WARN, event.getLevel());
        String formattedMessage = event.getFormattedMessage();
        assertNotNull(formattedMessage);
        assertTrue(formattedMessage.contains("/api/incidents/1/status"));
        assertTrue(formattedMessage.contains("Full authentication is required"));
    }

    @Test
    @DisplayName("Debería redactar tokens completos del motivo de autenticación")
    void shouldRedactCredentialsFromAuthenticationReason() throws Exception {
        request.setRequestURI("/api/protected");
        String token = "eyJhbGciOiJIUzI1NiJ9.payload.signature";

        entryPoint.commence(request, response, new BadCredentialsException("token=" + token));

        assertEquals(1, logbackAppender.list.size());
        String formattedMessage = logbackAppender.list.get(0).getFormattedMessage();
        assertFalse(formattedMessage.contains(token));
        assertTrue(formattedMessage.contains("[REDACTED]"));
    }
}
