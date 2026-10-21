package org.bulbaseros.dto;

import jakarta.validation.constraints.NotNull;
import org.bulbaseros.entity.EstadoIncidencia;

/**
 * Objeto de transferencia de datos (DTO) para la solicitud de actualización del estado de una incidencia.
 * <p>
 * Empleado en el proceso de triaje operativo municipal para modificar la fase del ciclo de vida
 * de una incidencia reportada (por ejemplo, validación, asignación, resolución o cierre).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
public class UpdateIncidentStatusRequest {

    /**
     * Nuevo estado que se asignará a la incidencia en el flujo de triaje.
     */
    @NotNull(message = "El estado de la incidencia es obligatorio")
    private EstadoIncidencia status;

    /**
     * Justificación o comentario explicativo opcional acerca del motivo de la transición de estado.
     */
    private String comment;

    /**
     * Constructor por defecto requerido para la deserialización de cargas útiles JSON.
     */
    public UpdateIncidentStatusRequest() {
    }

    /**
     * Constructor parametrizado para inicializar la solicitud con el nuevo estado requerido.
     *
     * @param status Nuevo estado de la incidencia.
     */
    public UpdateIncidentStatusRequest(EstadoIncidencia status) {
        this.status = status;
    }

    /**
     * Constructor de conveniencia que inicializa la solicitud a partir del nombre textual del estado.
     *
     * @param status Nombre del estado de la incidencia como texto.
     */
    public UpdateIncidentStatusRequest(String status) {
        this.status = (status != null && !status.trim().isEmpty())
                ? EstadoIncidencia.valueOf(status.trim().toUpperCase())
                : null;
    }

    /**
     * Constructor completo con estado y justificación o comentario del cambio.
     *
     * @param status  Nuevo estado de la incidencia.
     * @param comment Comentario o justificación de la decisión operativa.
     */
    public UpdateIncidentStatusRequest(EstadoIncidencia status, String comment) {
        this.status = status;
        this.comment = comment;
    }

    /**
     * Obtiene el nuevo estado solicitado para la incidencia.
     *
     * @return El estado de la incidencia como {@link EstadoIncidencia}.
     */
    public EstadoIncidencia getStatus() {
        return status;
    }

    /**
     * Establece el nuevo estado de la incidencia.
     *
     * @param status El nuevo estado como {@link EstadoIncidencia}.
     */
    public void setStatus(EstadoIncidencia status) {
        this.status = status;
    }

    /**
     * Obtiene el comentario o justificación asociado a la transición de estado.
     *
     * @return Comentario o justificación como {@link String}, o {@code null} si no fue provisto.
     */
    public String getComment() {
        return comment;
    }

    /**
     * Establece el comentario o justificación asociado a la transición de estado.
     *
     * @param comment Comentario o justificación como {@link String}.
     */
    public void setComment(String comment) {
        this.comment = comment;
    }
}
