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
 * Entidad de dominio JPA que representa una asignación operativa (tarea) de una incidencia urbana.
 * <p>
 * Mapea la tabla {@code assignments} de la base de datos PostgreSQL. Registra la vinculación de
 * un departamento municipal y, opcionalmente, un técnico responsable con una incidencia, manteniendo
 * el estado de actividad lógica ({@code isActive}) y trazabilidad de quién realizó la asignación.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "assignments")
public class Tareas {

    /**
     * Identificador único secuencial de la asignación (clave primaria).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Incidencia urbana asignada.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incidencia incident;

    /**
     * Departamento municipal responsable de atender la incidencia.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Departamento department;

    /**
     * Técnico específico asignado para ejecutar las labores operativas (opcional).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private Usuario technician;

    /**
     * Usuario (operador o administrador) que autorizó o ejecutó la asignación.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_id", nullable = false)
    private Usuario assignedBy;

    /**
     * Indicador de vigencia operativa de la asignación.
     * Si es {@code false}, la tarea se encuentra desactivada lógicamente.
     */
    @Column(name = "is_active")
    private Boolean isActive = true;

    /**
     * Fecha y hora en la que se efectuó la asignación.
     */
    @Column(name = "assigned_at")
    private OffsetDateTime assignedAt;

    /**
     * Constructor para la creación básica de una asignación departamental u operativa.
     *
     * @param incident   Incidencia urbana a asignar.
     * @param department Departamento municipal asignado.
     * @param technician Técnico asignado (opcional).
     * @param assignedBy Usuario que ejecuta la asignación.
     */
    public Tareas(Incidencia incident, Departamento department, Usuario technician, Usuario assignedBy) {
        this.incident = incident;
        this.department = department;
        this.technician = technician;
        this.assignedBy = assignedBy;
        this.isActive = true;
        this.assignedAt = OffsetDateTime.now();
    }

    /**
     * Método callback ejecutado antes de persistir para asegurar valores predeterminados.
     */
    @PrePersist
    protected void onCreate() {
        if (this.assignedAt == null) {
            this.assignedAt = OffsetDateTime.now();
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
    }
}
