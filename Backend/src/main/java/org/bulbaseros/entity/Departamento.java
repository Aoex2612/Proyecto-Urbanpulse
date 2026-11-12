package org.bulbaseros.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Entidad de dominio JPA que representa un departamento o área técnica municipal.
 * <p>
 * Mapea la tabla {@code departments} de la base de datos PostgreSQL, agrupando a los operarios
 * y técnicos municipales según su especialidad técnica (por ejemplo: Vías Públicas, Alumbrado, Limpieza).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "departments")
public class Departamento {

    /**
     * Identificador único secuencial del departamento (clave primaria).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identificador publico universal (UUID v4) del departamento para exposicion externa y API v2.
     */
    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;

    /**
     * Denominación oficial del departamento municipal (único, máximo 100 caracteres).
     */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * Descripción detallada del alcance y competencias del departamento.
     */
    @Column(columnDefinition = "text")
    private String description;

    /**
     * Constructor para la creación básica de un departamento municipal.
     *
     * @param name        Nombre o denominación del departamento.
     * @param description Descripción de funciones o alcance.
     */
    public Departamento(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Constructor para compatibilidad con identificador numerico legacy y tests.
     *
     * @param id          Identificador numerico interno.
     * @param name        Nombre o denominacion del departamento.
     * @param description Descripcion de funciones o alcance.
     */
    public Departamento(Long id, String name, String description) {
        this.id = id;
        this.publicId = UUID.randomUUID();
        this.name = name;
        this.description = description;
    }

    @PrePersist
    protected void onCreate() {
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID();
        }
    }
}
