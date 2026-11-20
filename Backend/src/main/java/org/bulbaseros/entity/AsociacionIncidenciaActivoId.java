package org.bulbaseros.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class AsociacionIncidenciaActivoId implements Serializable {

    @Column(name = "incident_id")
    private Long incidentId;

    @Column(name = "asset_id")
    private Long assetId;
}
