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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Manejador personalizado para la denegación de acceso y control de permisos (RBAC).
 * <p>
 * Se activa automáticamente cuando un usuario autenticado intenta realizar una acción o invocar
 * un endpoint para el cual no posee los privilegios o roles necesarios, retornando una respuesta
 * HTTP 403 Forbidden serializada en JSON mediante el DTO {@link ErrorResponse}.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /**
     * Constructor por defecto que inicializa un {@link ObjectMapper} configurado con {@link JavaTimeModule}.
     */
    public CustomAccessDeniedHandler() {
        this(new ObjectMapper());
    }

    /**
     * Constructor que inyecta o recibe una instancia de {@link ObjectMapper} y asegura el registro de {@link JavaTimeModule}.
     *
     * @param objectMapper instancia de {@link ObjectMapper} a utilizar o {@code null} para instanciar una nueva.
     */
    @Autowired
    public CustomAccessDeniedHandler(ObjectMapper objectMapper) {
        ObjectMapper mapper = (objectMapper != null) ? objectMapper.copy() : new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper = mapper;
    }

    /**
     * Intercepta las solicitudes con permisos insuficientes y escribe en la respuesta HTTP un error 403 estructurado en JSON.
     *
     * @param request               la petición HTTP entrante.
     * @param response              la respuesta HTTP saliente.
     * @param accessDeniedException la excepción de acceso denegado que originó la invocación.
     * @throws IOException      en caso de error de entrada/salida al escribir la respuesta.
     * @throws ServletException en caso de error del servlet container.
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String message = "Acceso denegado: permisos insuficientes";
        Object customMessage = request.getAttribute("access_denied_message");
        if (customMessage instanceof String str && !str.isBlank()) {
            message = str;
        }

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                message,
                request.getRequestURI()
        );

        String json = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(json);
        response.getWriter().flush();
    }
}
