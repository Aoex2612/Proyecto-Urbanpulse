package org.bulbaseros.mappers;

import org.bulbaseros.dto.AssignmentResponse;
import org.bulbaseros.entity.Departamento;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.Tareas;
import org.bulbaseros.entity.Usuario;
import org.springframework.stereotype.Component;

/**
 * Mapeador desacoplado de componentes Spring para la conversión entre la entidad de persistencia
 * {@link Tareas} y el objeto de transferencia de datos {@link AssignmentResponse}.
 * <p>
 * Facilita la transformación segura de asignaciones operativas, protegiendo las referencias cíclicas
 * y garantizando la adecuada extracción de identificadores y nombres descriptivos de las entidades asociadas.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Component
public class AssignmentMapper extends BaseMapper<Tareas, AssignmentResponse> {

    /**
     * Transforma una entidad de persistencia {@link Tareas} a su respectivo DTO {@link AssignmentResponse}.
     *
     * @param entity Entidad de tarea/asignación origen, o {@code null}.
     * @return DTO {@link AssignmentResponse} con los datos correspondientes, o {@code null} si la entidad es nula.
     */
    @Override
    public AssignmentResponse toDto(Tareas entity) {
        if (entity == null) {
            return null;
        }

        Long departmentId = (entity.getDepartment() != null) ? entity.getDepartment().getId() : null;
        String departmentName = (entity.getDepartment() != null) ? entity.getDepartment().getName() : null;

        Long technicianId = (entity.getTechnician() != null) ? entity.getTechnician().getId() : null;
        String technicianName = (entity.getTechnician() != null) ? entity.getTechnician().getFullName() : null;

        Long assignedById = (entity.getAssignedBy() != null) ? entity.getAssignedBy().getId() : null;
        String assignedByName = (entity.getAssignedBy() != null) ? entity.getAssignedBy().getFullName() : null;

        return AssignmentResponse.builder()
                .id(entity.getId())
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

    /**
     * Construye una nueva entidad {@link Tareas} a partir de las entidades de dominio involucradas.
     *
     * @param incident   Incidencia urbana asociada.
     * @param department Departamento municipal asignado.
     * @param technician Técnico específico asignado (opcional).
     * @param assignedBy Usuario operador o administrador que autoriza la asignación.
     * @return Nueva instancia de la entidad {@link Tareas} lista para ser persistida.
     */
    public Tareas toEntity(Incidencia incident, Departamento department, Usuario technician, Usuario assignedBy) {
        return new Tareas(incident, department, technician, assignedBy);
    }
}
