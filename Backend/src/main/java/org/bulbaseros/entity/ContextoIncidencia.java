package org.bulbaseros.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "incident_contexts")
public class ContextoIncidencia {

    @EmbeddedId
    private ContextoIncidenciaId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("incidentId")
    @JoinColumn(name = "incident_id")
    private Incidencia incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("observationId")
    @JoinColumn(name = "observation_id")
    private ObservacionesExternas observation;
}
