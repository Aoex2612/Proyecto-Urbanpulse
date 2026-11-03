package org.bulbaseros.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Data
@Embeddable
public class ContextoIncidenciaId implements Serializable {

    @Column(name = "incident_id")
    private Long incidentId;

    @Column(name = "observation_id")
    private Long observationId;
}
