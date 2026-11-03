package org.bulbaseros.dto;

import org.bulbaseros.entity.EstadoIncidencia;

import java.time.OffsetDateTime;

/**
 * Objeto de transferencia de datos (DTO) para la confirmación de cambio de estado en una incidencia.
 * <p>
 * Representa la respuesta emitida tras una operación exitosa de triaje sobre el endpoint
 * {@code PATCH /api/incidents/{id}/status}, proporcionando el identificador de la incidencia,
 * su nuevo estado consolidado, un mensaje de confirmación y la marca temporal de actualización.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
public class IncidentStatusResponse {

    /**
     * Identificador único de la incidencia cuyo estado fue actualizado.
     */
    private Long id;

    /**
     * Estado consolidado de la incidencia tras la operación de triaje.
     */
    private EstadoIncidencia status;

    /**
     * Mensaje descriptivo de confirmación del resultado de la operación.
     */
    private String message;

    /**
     * Marca temporal en zona horaria UTC/Offset indicando cuándo se aplicó el cambio de estado.
     */
    private OffsetDateTime updatedAt;

    /**
     * Constructor por defecto requerido para deserialización y serialización JSON.
     * Inicializa {@code updatedAt} con la marca temporal actual.
     */
    public IncidentStatusResponse() {
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * Constructor parametrizado para generar la confirmación de estado con marca temporal automática.
     *
     * @param id      Identificador numérico de la incidencia.
     * @param status  Estado actualizado de la incidencia.
     * @param message Mensaje descriptivo de confirmación.
     */
    public IncidentStatusResponse(Long id, EstadoIncidencia status, String message) {
        this(id, status, message, OffsetDateTime.now());
    }

    /**
     * Constructor completo con todos los atributos de confirmación de estado.
     *
     * @param id        Identificador numérico de la incidencia.
     * @param status    Estado actualizado de la incidencia.
     * @param message   Mensaje descriptivo de confirmación.
     * @param updatedAt Marca temporal precisa de la actualización.
     */
    public IncidentStatusResponse(Long id, EstadoIncidencia status, String message, OffsetDateTime updatedAt) {
        this.id = id;
        this.status = status;
        this.message = message;
        this.updatedAt = (updatedAt != null) ? updatedAt : OffsetDateTime.now();
    }

    /**
     * Obtiene el identificador de la incidencia.
     *
     * @return Identificador como {@link Long}.
     */
    public Long getId() {
        return id;
    }

    /**
     * Establece el identificador de la incidencia.
     *
     * @param id Identificador como {@link Long}.
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Obtiene el estado actualizado de la incidencia.
     *
     * @return Estado como {@link EstadoIncidencia}.
     */
    public EstadoIncidencia getStatus() {
        return status;
    }

    /**
     * Establece el estado actualizado de la incidencia.
     *
     * @param status Estado como {@link EstadoIncidencia}.
     */
    public void setStatus(EstadoIncidencia status) {
        this.status = status;
    }

    /**
     * Obtiene el mensaje descriptivo de confirmación.
     *
     * @return Mensaje de confirmación como {@link String}.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Establece el mensaje descriptivo de confirmación.
     *
     * @param message Mensaje de confirmación como {@link String}.
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Obtiene la marca temporal de actualización.
     *
     * @return Marca temporal como {@link OffsetDateTime}.
     */
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Establece la marca temporal de actualización.
     *
     * @param updatedAt Marca temporal como {@link OffsetDateTime}.
     */
    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
