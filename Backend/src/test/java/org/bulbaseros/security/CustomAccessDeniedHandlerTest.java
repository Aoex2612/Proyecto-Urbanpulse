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
import org.springframework.security.access.AccessDeniedException;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para {@link CustomAccessDeniedHandler}.
 * <p>
 * Verifica la captura de excepciones por permisos insuficientes (403 Forbidden), la serialización
 * del cuerpo JSON según {@link ErrorResponse}, el registro del evento de seguridad con nivel WARN
 * e incluyendo la URI de la petición, y el registro adecuado de {@link JavaTimeModule}.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class CustomAccessDeniedHandlerTest {

    private CustomAccessDeniedHandler accessDeniedHandler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private ObjectMapper objectMapper;
    private ListAppender<ILoggingEvent> logbackAppender;

    @BeforeEach
    void setUp() {
        accessDeniedHandler = new CustomAccessDeniedHandler();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        Logger logger = (Logger) LoggerFactory.getLogger(CustomAccessDeniedHandler.class);
        logbackAppender = new ListAppender<>();
        logbackAppender.start();
        logger.addAppender(logbackAppender);
        logger.setLevel(Level.DEBUG);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(CustomAccessDeniedHandler.class);
        logger.detachAppender(logbackAppender);
        logbackAppender.stop();
    }

    @Test
    @DisplayName("Debería responder HTTP 403 con JSON normalizado ErrorResponse ante acceso denegado por rol")
    void shouldReturn403WithStandardErrorResponseWhenHandleIsInvoked() throws Exception {
        request.setRequestURI("/api/incidents/1/status");
        AccessDeniedException accessDeniedException = new AccessDeniedException("Access is denied");

        accessDeniedHandler.handle(request, response, accessDeniedException);

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
        assertTrue(response.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE));
        assertEquals("UTF-8", response.getCharacterEncoding());

        String responseBody = response.getContentAsString();
        assertNotNull(responseBody);
        assertFalse(responseBody.isBlank());

        ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);
        assertEquals(403, errorResponse.getStatus());
        assertEquals("Forbidden", errorResponse.getError());
        assertEquals("Acceso denegado: permisos insuficientes", errorResponse.getMessage());
        assertEquals("/api/incidents/1/status", errorResponse.getPath());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    @DisplayName("Debería incluir mensaje personalizado si se especifica en el atributo del request")
    void shouldIncludeCustomMessageFromRequestAttribute() throws Exception {
        request.setRequestURI("/api/operator-only");
        request.setAttribute("access_denied_message", "Acceso denegado: se requiere rol de operador");

        accessDeniedHandler.handle(request, response, new AccessDeniedException("Denied"));

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());

        String responseBody = response.getContentAsString();
        ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);
        assertEquals(403, errorResponse.getStatus());
        assertEquals("Forbidden", errorResponse.getError());
        assertEquals("Acceso denegado: se requiere rol de operador", errorResponse.getMessage());
        assertEquals("/api/operator-only", errorResponse.getPath());
    }

    @Test
    @DisplayName("Debería serializar OffsetDateTime como texto ISO-8601 en el cuerpo JSON")
    void shouldSerializeOffsetDateTimeAsIsoString() throws Exception {
        request.setRequestURI("/api/test-403");

        accessDeniedHandler.handle(request, response, new AccessDeniedException("Forbidden"));

        String responseBody = response.getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);

        assertTrue(jsonNode.has("timestamp"));
        assertTrue(jsonNode.get("timestamp").isTextual(), "El campo timestamp debe ser una cadena ISO-8601");
        assertEquals(403, jsonNode.get("status").asInt());
        assertEquals("Forbidden", jsonNode.get("error").asText());
    }

    @Test
    @DisplayName("Debería soportar inicialización con constructor parametrizado y ObjectMapper nulo")
    void shouldSupportParameterizedConstructorAndNullObjectMapper() throws Exception {
        CustomAccessDeniedHandler handlerWithNull = new CustomAccessDeniedHandler(null);
        request.setRequestURI("/api/null-test");

        handlerWithNull.handle(request, response, new AccessDeniedException("Denied"));

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
        ErrorResponse errorResponse = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertEquals(403, errorResponse.getStatus());
        assertEquals("/api/null-test", errorResponse.getPath());
    }

    @Test
    @DisplayName("Debería soportar inicialización con ObjectMapper preconfigurado")
    void shouldSupportPreconfiguredObjectMapper() throws Exception {
        ObjectMapper customMapper = new ObjectMapper();
        CustomAccessDeniedHandler customHandler = new CustomAccessDeniedHandler(customMapper);
        request.setRequestURI("/api/custom-test");

        customHandler.handle(request, response, new AccessDeniedException("Denied"));

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
        ErrorResponse errorResponse = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertEquals(403, errorResponse.getStatus());
        assertEquals("/api/custom-test", errorResponse.getPath());
    }

    @Test
    @DisplayName("Debería registrar evento de acceso denegado con nivel WARN e incluir la URI de la peticion")
    void shouldLogAccessDeniedWithWarnLevelAndRequestUri() throws Exception {
        request.setRequestURI("/api/incidents/1/status");
        AccessDeniedException accessDeniedException = new AccessDeniedException("Access is denied");

        accessDeniedHandler.handle(request, response, accessDeniedException);

        List<ILoggingEvent> events = logbackAppender.list;
        assertEquals(1, events.size());

        ILoggingEvent event = events.get(0);
        assertEquals(Level.WARN, event.getLevel());
        String formattedMessage = event.getFormattedMessage();
        assertNotNull(formattedMessage);
        assertTrue(formattedMessage.contains("/api/incidents/1/status"));
        assertTrue(formattedMessage.contains("Access is denied"));
        assertFalse(formattedMessage.contains("Bearer"));
    }

    @Test
    @DisplayName("Debería redactar tokens completos del motivo de acceso denegado")
    void shouldRedactCredentialsFromAccessDeniedReason() throws Exception {
        request.setRequestURI("/api/admin");
        String token = "eyJhbGciOiJIUzI1NiJ9.payload.signature";

        accessDeniedHandler.handle(request, response,
                new AccessDeniedException("token=" + token));

        assertEquals(1, logbackAppender.list.size());
        String formattedMessage = logbackAppender.list.get(0).getFormattedMessage();
        assertFalse(formattedMessage.contains(token));
        assertTrue(formattedMessage.contains("[REDACTED]"));
    }
}
