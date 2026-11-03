package org.bulbaseros.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bulbaseros.dto.ErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Punto de entrada para el manejo de excepciones de autenticación en la infraestructura de seguridad.
 * <p>
 * Se activa automáticamente cuando un cliente no autenticado o con credenciales inválidas (o ausentes)
 * intenta acceder a un recurso protegido, emitiendo una respuesta HTTP 401 Unauthorized estructurada
 * en formato JSON conforme al DTO {@link ErrorResponse}.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * Constructor por defecto que inicializa un {@link ObjectMapper} configurado con {@link JavaTimeModule}.
     */
    public JwtAuthenticationEntryPoint() {
        this(new ObjectMapper());
    }

    /**
     * Constructor que inyecta o recibe una instancia de {@link ObjectMapper} y asegura el registro de {@link JavaTimeModule}.
     *
     * @param objectMapper instancia de {@link ObjectMapper} a utilizar o {@code null} para instanciar una nueva.
     */
    @Autowired
    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        ObjectMapper mapper = (objectMapper != null) ? objectMapper.copy() : new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper = mapper;
    }

    /**
     * Intercepta las solicitudes no autorizadas y escribe en la respuesta HTTP un error 401 estructurado en JSON.
     *
     * @param request       la petición HTTP entrante.
     * @param response      la respuesta HTTP saliente.
     * @param authException la excepción de autenticación que motivó la invocación.
     * @throws IOException      en caso de error de entrada/salida al escribir la respuesta.
     * @throws ServletException en caso de error del servlet container.
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String message = "Token de autenticación ausente o inválido";
        Object customMessage = request.getAttribute("auth_error_message");
        if (customMessage instanceof String str && !str.isBlank()) {
            message = str;
        } else if (authException != null && authException.getMessage() != null && !authException.getMessage().isBlank()) {
            if ("Token de autenticación expirado o inválido".equals(authException.getMessage()) ||
                "Token de autenticación ausente o inválido".equals(authException.getMessage())) {
                message = authException.getMessage();
            }
        }

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                message,
                request.getRequestURI()
        );

        String json = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(json);
        response.getWriter().flush();
    }
}
