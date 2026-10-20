package org.bulbaseros.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "external_observations")
public class Observaciones_Externas {

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
