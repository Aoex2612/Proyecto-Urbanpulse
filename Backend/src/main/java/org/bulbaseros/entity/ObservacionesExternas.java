package org.bulbaseros.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Entidad JPA que representa una observación contextual externa en la plataforma UrbanPulse.
 * Mapea la tabla {@code external_observations} de la base de datos.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@Entity
@Table(name = "external_observations")
public class ObservacionesExternas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(name = "metric_type", nullable = false, length = 50)
    private String metricType;

    @Column(name = "value_num")
    private Double valueNum;

    @Column(name = "value_text", length = 100)
    private String valueText;

    @Column(name = "observed_at", nullable = false)
    private OffsetDateTime observedAt;
}
