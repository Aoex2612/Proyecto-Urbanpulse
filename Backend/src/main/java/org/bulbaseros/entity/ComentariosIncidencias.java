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
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Entidad de dominio JPA que representa un comentario o nota de seguimiento asociada a una incidencia urbana.
 * <p>
 * Mapea la tabla {@code incident_comments} de la base de datos PostgreSQL, almacenando observaciones
 * y comunicaciones relativas a una incidencia. Puede tratarse de una nota técnica interna (visible exclusivamente
 * para operadores, técnicos y administradores) o de un comentario público accesible para el ciudadano autor.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "incident_comments")
public class ComentariosIncidencias {

    /**
     * Identificador único secuencial del comentario (clave primaria).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Incidencia urbana a la que se asocia el comentario.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incidencia incident;

    /**
     * Usuario autor del comentario (ciudadano, operador, técnico o administrador).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Usuario author;

    /**
     * Contenido textual del comentario u observación.
     */
    @Column(nullable = false, columnDefinition = "text")
    private String comment;

    /**
     * Indicador de visibilidad interna. Si es {@code true}, solo es accesible por personal municipal
     * (operadores, técnicos y administradores). Si es {@code false}, es público y visible para el ciudadano autor.
     */
    @Column(name = "is_internal")
    private Boolean isInternal = true;

    /**
     * Marca temporal en que fue redactado y persistido originalmente el comentario.
     */
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    /**
     * Marca temporal de la última modificación efectuada sobre el comentario.
     * Permanece nula hasta que el autor o un administrador edite el contenido.
     */
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    /**
     * Constructor para la creación básica de un comentario sobre una incidencia.
     *
     * @param incident   Incidencia urbana asociada.
     * @param author     Usuario autor del comentario.
     * @param comment    Texto del comentario.
     * @param isInternal Indicador de nota interna o comentario público.
     */
    public ComentariosIncidencias(Incidencia incident, Usuario author, String comment, Boolean isInternal) {
        this.incident = incident;
        this.author = author;
        this.comment = comment;
        this.isInternal = isInternal != null ? isInternal : true;
        this.createdAt = OffsetDateTime.now();
    }

    /**
     * Método callback de ciclo de vida JPA ejecutado antes de la persistencia para inicializar valores por defecto.
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        if (this.isInternal == null) {
            this.isInternal = true;
        }
    }
}
