package org.bulbaseros.dto;

import org.bulbaseros.entity.CambiosEstado;

import java.time.OffsetDateTime;

/**
 * Objeto de transferencia de datos (DTO) que representa una transicion historica de estado para auditoria.
 * <p>
 * Contiene el registro inmutable del cambio de ciclo de vida de una incidencia, reflejando el estado previo,
 * el nuevo estado asignado, los datos identificativos del usuario que realizo la accion (id, nombre y correo),
 * la justificacion o motivo del cambio y la marca temporal de ocurrencia.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
public class StatusChangeResponse {

    /**
     * Identificador unico del registro de auditoria.
     */
    private Long id;

    /**
     * Identificador de la incidencia urbana vinculada.
     */
    private Long incidentId;

    /**
     * Estado original del que partia la incidencia antes del cambio.
     */
    private String fromStatus;

    /**
     * Nuevo estado consolidado tras la aplicacion del cambio.
     */
    private String toStatus;

    /**
     * Identificador unico del usuario que aplico la modificacion de estado.
     */
    private Long changedByUserId;

    /**
     * Nombre completo del usuario que aplico la modificacion de estado.
     */
    private String changedByUserName;

    /**
     * Correo electronico del usuario que aplico la modificacion de estado.
     */
    private String changedByUserEmail;

    /**
     * Motivo explicativo o justificacion registrada para la transicion.
     */
    private String reason;

    /**
     * Marca temporal en que se produjo la transicion de estado.
     */
    private OffsetDateTime createdAt;

    /**
     * Constructor por defecto requerido para deserializacion JSON.
     */
    public StatusChangeResponse() {
    }

    /**
     * Constructor completo con todos los atributos de auditoria de transicion.
     *
     * @param id                 Identificador del registro de auditoria.
     * @param incidentId         Identificador de la incidencia afectada.
     * @param fromStatus         Estado original previo.
     * @param toStatus           Nuevo estado asignado.
     * @param changedByUserId    Identificador del usuario responsable.
     * @param changedByUserName  Nombre completo del usuario responsable.
     * @param changedByUserEmail Correo electronico del usuario responsable.
     * @param reason             Justificacion o motivo del cambio.
     * @param createdAt          Marca temporal de la transicion.
     */
    public StatusChangeResponse(Long id, Long incidentId, String fromStatus, String toStatus,
                                Long changedByUserId, String changedByUserName, String changedByUserEmail,
                                String reason, OffsetDateTime createdAt) {
        this.id = id;
        this.incidentId = incidentId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedByUserId = changedByUserId;
        this.changedByUserName = changedByUserName;
        this.changedByUserEmail = changedByUserEmail;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    /**
     * Metodo factoria estatico para convertir una entidad de dominio {@link CambiosEstado} en su correspondiente DTO.
     *
     * @param cambio Entidad de dominio origen, o {@code null}.
     * @return DTO {@link StatusChangeResponse} inicializado, o {@code null} si la entidad recibida es nula.
     */
    public static StatusChangeResponse fromEntity(CambiosEstado cambio) {
        if (cambio == null) {
            return null;
        }
        Long incidentId = (cambio.getIncident() != null) ? cambio.getIncident().getId() : null;
        Long changedByUserId = (cambio.getChangedByUser() != null) ? cambio.getChangedByUser().getId() : null;
        String changedByUserName = (cambio.getChangedByUser() != null) ? cambio.getChangedByUser().getFullName() : null;
        String changedByUserEmail = (cambio.getChangedByUser() != null) ? cambio.getChangedByUser().getEmail() : null;

        return new StatusChangeResponse(
                cambio.getId(),
                incidentId,
                cambio.getFromStatus(),
                cambio.getToStatus(),
                changedByUserId,
                changedByUserName,
                changedByUserEmail,
                cambio.getReason(),
                cambio.getCreatedAt()
        );
    }

    /**
     * Obtiene el identificador del registro de cambio de estado.
     *
     * @return Identificador como {@link Long}.
     */
    public Long getId() {
        return id;
    }

    /**
     * Establece el identificador del registro de cambio de estado.
     *
     * @param id Identificador como {@link Long}.
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Obtiene el identificador de la incidencia asociada.
     *
     * @return Identificador de la incidencia como {@link Long}.
     */
    public Long getIncidentId() {
        return incidentId;
    }

    /**
     * Establece el identificador de la incidencia asociada.
     *
     * @param incidentId Identificador de la incidencia como {@link Long}.
     */
    public void setIncidentId(Long incidentId) {
        this.incidentId = incidentId;
    }

    /**
     * Obtiene el estado original previo a la transicion.
     *
     * @return Nombre del estado anterior como {@link String}.
     */
    public String getFromStatus() {
        return fromStatus;
    }

    /**
     * Establece el estado original previo a la transicion.
     *
     * @param fromStatus Nombre del estado anterior como {@link String}.
     */
    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    /**
     * Obtiene el nuevo estado resultante tras la transicion.
     *
     * @return Nombre del nuevo estado como {@link String}.
     */
    public String getToStatus() {
        return toStatus;
    }

    /**
     * Establece el nuevo estado resultante tras la transicion.
     *
     * @param toStatus Nombre del nuevo estado como {@link String}.
     */
    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    /**
     * Obtiene el identificador del usuario que ejecuto la transicion.
     *
     * @return Identificador de usuario como {@link Long}.
     */
    public Long getChangedByUserId() {
        return changedByUserId;
    }

    /**
     * Establece el identificador del usuario que ejecuto la transicion.
     *
     * @param changedByUserId Identificador de usuario como {@link Long}.
     */
    public void setChangedByUserId(Long changedByUserId) {
        this.changedByUserId = changedByUserId;
    }

    /**
     * Obtiene el nombre completo del usuario que ejecuto la transicion.
     *
     * @return Nombre completo como {@link String}.
     */
    public String getChangedByUserName() {
        return changedByUserName;
    }

    /**
     * Establece el nombre completo del usuario que ejecuto la transicion.
     *
     * @param changedByUserName Nombre completo como {@link String}.
     */
    public void setChangedByUserName(String changedByUserName) {
        this.changedByUserName = changedByUserName;
    }

    /**
     * Obtiene el correo electronico del usuario que ejecuto la transicion.
     *
     * @return Correo electronico como {@link String}.
     */
    public String getChangedByUserEmail() {
        return changedByUserEmail;
    }

    /**
     * Establece el correo electronico del usuario que ejecuto la transicion.
     *
     * @param changedByUserEmail Correo electronico como {@link String}.
     */
    public void setChangedByUserEmail(String changedByUserEmail) {
        this.changedByUserEmail = changedByUserEmail;
    }

    /**
     * Obtiene la justificacion o motivo del cambio de estado.
     *
     * @return Motivo como {@link String}.
     */
    public String getReason() {
        return reason;
    }

    /**
     * Establece la justificacion o motivo del cambio de estado.
     *
     * @param reason Motivo como {@link String}.
     */
    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * Obtiene la marca temporal en que se produjo la transicion.
     *
     * @return Fecha y hora como {@link OffsetDateTime}.
     */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Establece la marca temporal en que se produjo la transicion.
     *
     * @param createdAt Fecha y hora como {@link OffsetDateTime}.
     */
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
