package org.bulbaseros.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Data
@Embeddable
public class AsociacionIncidenciaActivoId implements Serializable {

    @Column(name = "incident_id")
    private Long incidentId;

    @Column(name = "asset_id")
    private Long assetId;
}
