package org.bulbaseros.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "incident_comments")
public class ComentariosIncidencias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incidencia incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Usuario author;

    @Column(nullable = false, columnDefinition = "text")
    private String comment;

    @Column(name = "is_internal")
    private Boolean isInternal;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
