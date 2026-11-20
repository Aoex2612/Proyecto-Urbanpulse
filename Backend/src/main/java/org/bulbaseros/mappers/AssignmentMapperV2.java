package org.bulbaseros.mappers;

import org.bulbaseros.dto.v2.AssignmentResponseV2;
import org.bulbaseros.entity.Tareas;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapeador de componentes Spring para transformar {@link Tareas} a {@link AssignmentResponseV2}.
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@Component
public class AssignmentMapperV2 {

    public AssignmentResponseV2 toDto(Tareas entity) {
        if (entity == null) {
            return null;
        }

        String id = entity.getPublicId() != null ? entity.getPublicId().toString() : null;
        String incidentId = (entity.getIncident() != null && entity.getIncident().getPublicId() != null)
                ? entity.getIncident().getPublicId().toString() : null;
        String departmentId = (entity.getDepartment() != null && entity.getDepartment().getPublicId() != null)
                ? entity.getDepartment().getPublicId().toString() : null;
        String departmentName = (entity.getDepartment() != null) ? entity.getDepartment().getName() : null;
        String technicianId = (entity.getTechnician() != null && entity.getTechnician().getPublicId() != null)
                ? entity.getTechnician().getPublicId().toString() : null;
        String technicianName = (entity.getTechnician() != null) ? entity.getTechnician().getFullName() : null;
        String assignedById = (entity.getAssignedBy() != null && entity.getAssignedBy().getPublicId() != null)
                ? entity.getAssignedBy().getPublicId().toString() : null;
        String assignedByName = (entity.getAssignedBy() != null) ? entity.getAssignedBy().getFullName() : null;

        return AssignmentResponseV2.builder()
                .id(id)
                .incidentId(incidentId)
                .departmentId(departmentId)
                .departmentName(departmentName)
                .technicianId(technicianId)
                .technicianName(technicianName)
                .assignedById(assignedById)
                .assignedByName(assignedByName)
                .isActive(entity.getIsActive())
                .assignedAt(entity.getAssignedAt())
                .build();
    }

    public List<AssignmentResponseV2> toDtoList(List<Tareas> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }
}
