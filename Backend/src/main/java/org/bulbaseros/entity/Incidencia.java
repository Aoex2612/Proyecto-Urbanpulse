package org.bulbaseros.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "incidents")
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "accuracy_meters")
    private Double accuracyMeters;

    @Column(length = 255)
    private String address;

    @Column(length = 100)
    private String district;

    @Column(length = 100)
    private String neighborhood;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoIncidencia status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrioridadIncidencia priority;

    @Column(name = "priority_reason", columnDefinition = "text")
    private String priorityReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false)
    private Usuario citizen;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
