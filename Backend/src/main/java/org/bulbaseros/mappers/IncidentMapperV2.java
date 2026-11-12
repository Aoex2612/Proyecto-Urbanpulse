package org.bulbaseros.mappers;

import org.bulbaseros.dto.PageResponse;
import org.bulbaseros.dto.v2.IncidentResponseV2;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.Tareas;
import org.bulbaseros.repository.TareasRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapeador de componentes Spring para la conversion entre entidades {@link Incidencia}
 * y DTOs {@link IncidentResponseV2} en la API v2.
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@Component
public class IncidentMapperV2 {

    private final TareasRepository tareasRepository;
    private final AssignmentMapperV2 assignmentMapperV2;

    public IncidentMapperV2() {
        this(null, null);
    }

    @Autowired
    public IncidentMapperV2(@Autowired(required = false) TareasRepository tareasRepository,
                            @Autowired(required = false) AssignmentMapperV2 assignmentMapperV2) {
        this.tareasRepository = tareasRepository;
        this.assignmentMapperV2 = assignmentMapperV2;
    }

    public IncidentResponseV2 toDto(Incidencia entity) {
        if (entity == null) {
            return null;
        }

        String id = entity.getPublicId() != null ? entity.getPublicId().toString() : null;
        String citizenId = (entity.getCitizen() != null && entity.getCitizen().getPublicId() != null)
                ? entity.getCitizen().getPublicId().toString() : null;
        String citizenName = (entity.getCitizen() != null) ? entity.getCitizen().getFullName() : null;
        String citizenEmail = (entity.getCitizen() != null) ? entity.getCitizen().getEmail() : null;

        IncidentResponseV2 dto = IncidentResponseV2.builder()
                .id(id)
                .title(entity.getTitle())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .accuracyMeters(entity.getAccuracyMeters())
                .address(entity.getAddress())
                .district(entity.getDistrict())
                .neighborhood(entity.getNeighborhood())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .priorityReason(entity.getPriorityReason())
                .citizenId(citizenId)
                .citizenName(citizenName)
                .citizenEmail(citizenEmail)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .urbanAssets(Collections.emptyList())
                .urbanContext(Collections.emptyList())
                .activeAssignments(Collections.emptyList())
                .build();

        if (tareasRepository != null && assignmentMapperV2 != null && entity.getPublicId() != null) {
            List<Tareas> activeTasks = tareasRepository.findByIncidentPublicIdAndIsActiveTrue(entity.getPublicId());
            dto.setActiveAssignments(assignmentMapperV2.toDtoList(activeTasks));
        }

        return dto;
    }

    public List<IncidentResponseV2> toDtoList(List<Incidencia> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    public PageResponse<IncidentResponseV2> toPageResponse(Page<Incidencia> page) {
        if (page == null) {
            return new PageResponse<>(Collections.emptyList(), 0, 0, 0, 0);
        }
        List<IncidentResponseV2> content = toDtoList(page.getContent());
        return new PageResponse<>(content, page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize());
    }
}
