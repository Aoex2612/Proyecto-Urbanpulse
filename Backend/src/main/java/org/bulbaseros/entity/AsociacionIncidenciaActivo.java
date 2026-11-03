package org.bulbaseros.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "incident_asset_associations")
public class AsociacionIncidenciaActivo {

    @EmbeddedId
    private AsociacionIncidenciaActivoId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("incidentId")
    @JoinColumn(name = "incident_id")
    private Incidencia incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("assetId")
    @JoinColumn(name = "asset_id")
    private ActivoUrbano asset;

    @Column(name = "distance_meters", nullable = false)
    private Double distanceMeters;

    @Column(name = "confidence_score", nullable = false)
    private Double confidenceScore;

    @Column(name = "is_confirmed")
    private Boolean isConfirmed;
}
