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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Entidad de dominio JPA que representa una notificación in-app dirigida a un usuario de UrbanPulse.
 * <p>
 * Mapea la tabla {@code notifications} de la base de datos PostgreSQL, registrando avisos
 * generados ante eventos relevantes del ciclo de vida de las incidencias (por ejemplo, cambios
 * de estado o sugerencias de duplicados detectados por el sistema de triaje operativo).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {

    /**
     * Identificador único secuencial de la notificación (clave primaria).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuario destinatario de la notificación (ciudadano, operador, técnico o administrador).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Usuario user;

    /**
     * Incidencia urbana a la que hace referencia o sobre la que se originó el aviso.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incidencia incident;

    /**
     * Tipo funcional o categoría del evento que disparó la notificación (ej. STATUS_CHANGE, DUPLICATE_SUGGESTION).
     */
    @Column(nullable = false, length = 50)
    private String type;

    /**
     * Título conciso del aviso informativo.
     */
    @Column(nullable = false, length = 150)
    private String title;

    /**
     * Cuerpo textual explicativo con el detalle o contextualización del aviso.
     */
    @Column(nullable = false, columnDefinition = "text")
    private String message;

    /**
     * Indicador de lectura de la notificación por parte del usuario destinatario.
     * {@code false} por defecto hasta que el usuario la marque como leída.
     */
    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    /**
     * Marca temporal en la que se generó y persistió la notificación.
     */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /**
     * Constructor para la creación programática de una notificación con valores predeterminados.
     *
     * @param user     Usuario destinatario.
     * @param incident Incidencia urbana asociada.
     * @param type     Tipo funcional de aviso.
     * @param title    Título del aviso.
     * @param message  Detalle o contenido del aviso.
     */
    public Notification(Usuario user, Incidencia incident, String type, String title, String message) {
        this.user = user;
        this.incident = incident;
        this.type = type;
        this.title = title;
        this.message = message;
        this.isRead = false;
        this.createdAt = OffsetDateTime.now();
    }

    /**
     * Método callback de ciclo de vida JPA ejecutado antes de persistir para asegurar
     * la marca temporal de creación y el estado no leído por defecto si no han sido inicializados.
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        if (this.isRead == null) {
            this.isRead = false;
        }
    }
}
