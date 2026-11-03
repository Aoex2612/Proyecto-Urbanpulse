package org.bulbaseros.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Entidad de dominio JPA que representa una incidencia urbana reportada en la plataforma UrbanPulse.
 * <p>
 * Mapea la tabla {@code incidents} de la base de datos PostgreSQL, almacenando informacion
 * georreferenciada (latitud y longitud), clasificacion tematica por categoria, datos descriptivos,
 * estado dentro del ciclo de vida de triaje, nivel de prioridad y el ciudadano emisor del reporte.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@Entity
@Table(name = "incidents")
public class Incidencia {

    /**
     * Identificador unico secuencial de la incidencia (clave primaria).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Titulo sintetico y descriptivo de la incidencia urbana.
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * Descripcion detallada del problema o suceso reportado por el ciudadano.
     */
    @Column(nullable = false, columnDefinition = "text")
    private String description;

    /**
     * Categoria tematica de clasificacion del incidente (ej. Alumbrado, Vias Publicas, Limpieza).
     */
    @Column(nullable = false, length = 50)
    private String category;

    /**
     * Coordenada geografica de latitud donde se ubica el suceso reportado.
     */
    @Column(nullable = false)
    private Double latitude;

    /**
     * Coordenada geografica de longitud donde se ubica el suceso reportado.
     */
    @Column(nullable = false)
    private Double longitude;

    /**
     * Precision estimada de la captura geografica expresada en metros (opcional).
     */
    @Column(name = "accuracy_meters")
    private Double accuracyMeters;

    /**
     * Direccion postal o referencia urbana aproximada del incidente.
     */
    @Column(length = 255)
    private String address;

    /**
     * Distrito municipal administrativo en el que se ubica la incidencia.
     */
    @Column(length = 100)
    private String district;

    /**
     * Barrio o vecindario especifico del entorno del incidente.
     */
    @Column(length = 100)
    private String neighborhood;

    /**
     * Estado consolidado actual de la incidencia en su ciclo de vida y flujo de triaje.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoIncidencia status;

    /**
     * Nivel de urgencia o prioridad asignada para la resolucion del incidente.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrioridadIncidencia priority;

    /**
     * Justificacion tecnica o administrativa del nivel de prioridad asignado.
     */
    @Column(name = "priority_reason", columnDefinition = "text")
    private String priorityReason;

    /**
     * Ciudadano o usuario del sistema que emitio el reporte de la incidencia.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false)
    private Usuario citizen;

    /**
     * Marca temporal en que fue dada de alta la incidencia en la plataforma.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Marca temporal correspondiente a la ultima modificacion de datos de la incidencia.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    /**
     * Constructor por defecto requerido por la especificacion JPA.
     */
    public Incidencia() {
    }

    /**
     * Constructor basico para la creacion de nuevas incidencias con valores principales requeridos.
     *
     * @param title       Titulo sintetico de la incidencia.
     * @param description Descripcion detallada del problema.
     * @param category    Categoria tematica de clasificacion.
     * @param latitude    Latitud geografica del suceso.
     * @param longitude   Longitud geografica del suceso.
     * @param citizen     Usuario emisor del reporte.
     */
    public Incidencia(String title, String description, String category, Double latitude, Double longitude, Usuario citizen) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.citizen = citizen;
        this.status = EstadoIncidencia.REPORTED;
        this.priority = PrioridadIncidencia.MEDIUM;
    }

    /**
     * Metodo callback ejecutado antes de persistir la entidad, asignando valores por defecto y marcas de tiempo.
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = OffsetDateTime.now();
        }
        if (this.status == null) {
            this.status = EstadoIncidencia.REPORTED;
        }
        if (this.priority == null) {
            this.priority = PrioridadIncidencia.MEDIUM;
        }
    }

    /**
     * Metodo callback ejecutado antes de actualizar la entidad, refrescando la marca temporal de actualizacion.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
