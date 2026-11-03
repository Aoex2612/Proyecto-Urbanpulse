package org.bulbaseros.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "urban_assets")
public class ActivoUrbano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_code", nullable = false, unique = true, length = 100)
    private String assetCode;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(length = 200)
    private String name;
}
