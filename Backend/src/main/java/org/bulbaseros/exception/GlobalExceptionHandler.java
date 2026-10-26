package org.bulbaseros.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.bulbaseros.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador global de excepciones para la API REST de UrbanPulse.
 * <p>
 * Centraliza la captura y serialización de excepciones en respuestas HTTP consistentes
 * utilizando {@link ErrorResponse}, garantizando códigos de respuesta estandarizados y
 * protegiendo la seguridad del sistema al evitar filtrar detalles sensibles.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja excepciones de validación en argumentos de controladores REST anotados con {@code @Valid}.
     * <p>
     * Retorna HTTP 400 Bad Request acompañado del mapa de errores campo a campo.
     * </p>
     *
     * @param ex      Excepción de validación capturada.
     * @param request Petición HTTP recibida.
     * @return {@link ResponseEntity} con estado 400 y el DTO {@link ErrorResponse}.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Error de validación en los datos de la solicitud",
                request.getRequestURI(),
                errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Maneja la excepción de conflicto cuando un correo electrónico ya está registrado.
     * <p>
     * Retorna HTTP 409 Conflict con el mensaje descriptivo del fallo.
     * </p>
     *
     * @param ex      Excepción {@link EmailAlreadyExistsException} capturada.
     * @param request Petición HTTP recibida.
     * @return {@link ResponseEntity} con estado 409 y el DTO {@link ErrorResponse}.
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(
            EmailAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Maneja fallos de credenciales incorrectas en los procesos de inicio de sesión.
     * <p>
     * Retorna HTTP 401 Unauthorized con un mensaje uniforme y genérico para prevenir
     * ataques de enumeración de usuarios.
     * </p>
     *
     * @param ex      Excepción de autenticación capturada ({@link InvalidCredentialsException} o {@link BadCredentialsException}).
     * @param request Petición HTTP recibida.
     * @return {@link ResponseEntity} con estado 401 y el DTO {@link ErrorResponse}.
     */
    @ExceptionHandler({InvalidCredentialsException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            Exception ex,
            HttpServletRequest request
    ) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Credenciales inválidas",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Maneja excepciones de entidad de incidencia no encontrada en el sistema.
     * <p>
     * Retorna HTTP 404 Not Found acompañado del mensaje explicativo de la excepción.
     * </p>
     *
     * @param ex      Excepción {@link IncidentNotFoundException} capturada.
     * @param request Petición HTTP recibida.
     * @return {@link ResponseEntity} con estado 404 y el DTO {@link ErrorResponse}.
     */
    @ExceptionHandler(IncidentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleIncidentNotFound(
            IncidentNotFoundException ex,
            HttpServletRequest request
    ) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Maneja excepciones del tipo {@link ResponseStatusException}.
     * <p>
     * Extrae el código de estado HTTP y la justificación asociada para retornar una respuesta estructurada.
     * </p>
     *
     * @param ex      Excepción {@link ResponseStatusException} capturada.
     * @param request Petición HTTP recibida.
     * @return {@link ResponseEntity} con el estado HTTP de la excepción y el DTO {@link ErrorResponse}.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {
        String reason = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getStatusCode().value(),
                ex.getStatusCode().toString(),
                reason,
                request.getRequestURI()
        );

        return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
    }

    /**
     * Manejador por defecto para cualquier otra excepción no controlada en el servidor.
     * <p>
     * Retorna HTTP 500 Internal Server Error con un mensaje genérico para salvaguardar
     * la integridad y los detalles internos del sistema.
     * </p>
     *
     * @param ex      Excepción no controlada capturada.
     * @param request Petición HTTP recibida.
     * @return {@link ResponseEntity} con estado 500 y el DTO {@link ErrorResponse}.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Ha ocurrido un error interno en el servidor",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
