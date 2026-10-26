package org.bulbaseros.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * Entidad de dominio JPA que representa un registro inmutable de auditoria sobre la transicion de estado de una incidencia.
 * <p>
 * Mapea la tabla {@code status_changes} de la base de datos PostgreSQL, registrando cada hito
 * en el ciclo de vida de una incidencia urbana, incluyendo el estado previo, el nuevo estado alcanzado,
 * el usuario responsable de autorizar o ejecutar el cambio, la justificacion o motivo, y la marca temporal de ocurrencia.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@Entity
@Table(name = "status_changes")
public class CambiosEstado {

    /**
     * Identificador unico secuencial del registro de auditoria (clave primaria).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Incidencia urbana asociada a la transicion de estado auditada.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incidencia incident;

    /**
     * Nombre textual del estado previo del que procedia la incidencia antes del cambio.
     */
    @Column(name = "from_status", nullable = false, length = 30)
    private String fromStatus;

    /**
     * Nombre textual del nuevo estado resultante tras la transicion.
     */
    @Column(name = "to_status", nullable = false, length = 30)
    private String toStatus;

    /**
     * Usuario que realizo o autorizo el cambio de estado (identificado mediante su token JWT).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id", nullable = false)
    private Usuario changedByUser;

    /**
     * Motivo explicativo, razon o justificacion administrativa del cambio de estado.
     */
    @Column(columnDefinition = "text")
    private String reason;

    /**
     * Marca temporal en que se produjo y persistio la transicion de estado.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Constructor por defecto requerido por la especificacion JPA.
     */
    public CambiosEstado() {
    }

    /**
     * Constructor parametrizado para instanciar una nueva transicion de estado de auditoria.
     *
     * @param incident      Incidencia afectada por la transicion.
     * @param fromStatus    Estado original previo al cambio.
     * @param toStatus      Nuevo estado asignado a la incidencia.
     * @param changedByUser Usuario responsable de la transicion.
     * @param reason        Motivo o justificacion del cambio de estado.
     */
    public CambiosEstado(Incidencia incident, String fromStatus, String toStatus, Usuario changedByUser, String reason) {
        this.incident = incident;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedByUser = changedByUser;
        this.reason = reason;
        this.createdAt = OffsetDateTime.now();
    }

    /**
     * Metodo callback ejecutado antes de la persistencia para garantizar la fijacion de la fecha de auditoria.
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }
}
