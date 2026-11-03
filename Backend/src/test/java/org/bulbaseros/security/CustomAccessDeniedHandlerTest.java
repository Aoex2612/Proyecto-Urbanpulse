package org.bulbaseros.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.bulbaseros.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para {@link CustomAccessDeniedHandler}.
 * <p>
 * Verifica la captura de excepciones por permisos insuficientes (403 Forbidden), la serialización
 * del cuerpo JSON según {@link ErrorResponse}, y el registro adecuado de {@link JavaTimeModule}.
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

    @BeforeEach
    void setUp() {
        accessDeniedHandler = new CustomAccessDeniedHandler();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
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
}
