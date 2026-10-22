package org.bulbaseros.dto;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Objeto de transferencia de datos (DTO) para la representación estandarizada de errores HTTP en la API REST.
 * <p>
 * Proporciona información contextual consistente que incluye marca de tiempo, código de estado HTTP,
 * descripción del error, mensaje informativo, ruta invocada y opcionalmente detalles de validación de campos.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
public class ErrorResponse {

    /**
     * Marca de tiempo en que ocurrió el error.
     */
    private OffsetDateTime timestamp;

    /**
     * Código de estado HTTP numérico (ej. 400, 401, 409, 500).
     */
    private int status;

    /**
     * Razón o título canónico del estado HTTP (ej. "Bad Request", "Conflict").
     */
    private String error;

    /**
     * Mensaje descriptivo del error para el consumidor de la API.
     */
    private String message;

    /**
     * Ruta relativa del recurso solicitado que originó la excepción.
     */
    private String path;

    /**
     * Mapa opcional de errores específicos por campo (utilizado principalmente en validaciones 400).
     */
    private Map<String, String> errors;

    /**
     * Constructor por defecto.
     */
    public ErrorResponse() {
        this.timestamp = OffsetDateTime.now();
    }

    /**
     * Constructor para errores generales sin detalle por campos.
     *
     * @param status  Código de estado HTTP.
     * @param error   Título del error HTTP.
     * @param message Mensaje descriptivo.
     * @param path    Ruta del recurso invocado.
     */
    public ErrorResponse(int status, String error, String message, String path) {
        this(status, error, message, path, null);
    }

    /**
     * Constructor completo que incluye mapa detallado de errores por campo.
     *
     * @param status  Código de estado HTTP.
     * @param error   Título del error HTTP.
     * @param message Mensaje descriptivo.
     * @param path    Ruta del recurso invocado.
     * @param errors  Mapa con pares campo-mensaje de error.
     */
    public ErrorResponse(int status, String error, String message, String path, Map<String, String> errors) {
        this.timestamp = OffsetDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.errors = errors;
    }

    /**
     * Obtiene la marca de tiempo del error.
     *
     * @return {@link OffsetDateTime} del momento del fallo.
     */
    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Establece la marca de tiempo del error.
     *
     * @param timestamp {@link OffsetDateTime} del momento del fallo.
     */
    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Obtiene el código de estado HTTP.
     *
     * @return Código numérico HTTP.
     */
    public int getStatus() {
        return status;
    }

    /**
     * Establece el código de estado HTTP.
     *
     * @param status Código numérico HTTP.
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * Obtiene el título descriptivo del error HTTP.
     *
     * @return Razón canónica HTTP.
     */
    public String getError() {
        return error;
    }

    /**
     * Establece el título descriptivo del error HTTP.
     *
     * @param error Razón canónica HTTP.
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * Obtiene el mensaje descriptivo del fallo.
     *
     * @return Mensaje como {@link String}.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Establece el mensaje descriptivo del fallo.
     *
     * @param message Mensaje como {@link String}.
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Obtiene la ruta del endpoint que generó el error.
     *
     * @return URI o ruta relativa solicitada.
     */
    public String getPath() {
        return path;
    }

    /**
     * Establece la ruta del endpoint que generó el error.
     *
     * @param path URI o ruta relativa solicitada.
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Obtiene el mapa detallado de errores de validación de campos.
     *
     * @return {@link Map} con nombres de campo y mensajes, o {@code null} si no aplica.
     */
    public Map<String, String> getErrors() {
        return errors;
    }

    /**
     * Establece el mapa detallado de errores de validación de campos.
     *
     * @param errors {@link Map} con nombres de campo y mensajes.
     */
    public void setErrors(Map<String, String> errors) {
        this.errors = errors;
    }
}
