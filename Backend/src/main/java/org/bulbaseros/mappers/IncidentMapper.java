package org.bulbaseros.mappers;

import org.bulbaseros.dto.AssignmentResponse;
import org.bulbaseros.dto.IncidentResponse;
import org.bulbaseros.dto.PageResponse;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.Tareas;
import org.bulbaseros.repository.TareasRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Mapeador de componentes Spring para la conversion entre entidades {@link Incidencia}
 * y DTOs {@link IncidentResponse}, asi como envoltorios paginados {@link PageResponse}.
 *
 * @author UrbanPulse Team
 * @version 1.1
 */
@Component
public class IncidentMapper extends BaseMapper<Incidencia, IncidentResponse> {

    private final TareasRepository tareasRepository;
    private final AssignmentMapper assignmentMapper;

    /**
     * Constructor por defecto para pruebas unitarias desacopladas sin dependencias de persistencia.
     */
    public IncidentMapper() {
        this(null, null);
    }

    /**
     * Constructor principal con inyeccion de dependencias para mapeo de asignaciones activas.
     *
     * @param tareasRepository repositorio de tareas y asignaciones operativas.
     * @param assignmentMapper mapeador de tareas a {@link AssignmentResponse}.
     */
    @Autowired
    public IncidentMapper(@Autowired(required = false) TareasRepository tareasRepository,
                          @Autowired(required = false) AssignmentMapper assignmentMapper) {
        this.tareasRepository = tareasRepository;
        this.assignmentMapper = assignmentMapper;
    }

    /**
     * Convierte una entidad {@link Incidencia} en su correspondiente DTO {@link IncidentResponse}.
     *
     * @param entity Entidad de incidencia origen, o {@code null}.
     * @return DTO {@link IncidentResponse} mapeado, o {@code null} si la entidad es nula.
     */
    @Override
    public IncidentResponse toDto(Incidencia entity) {
        if (entity == null) {
            return null;
        }

        Long citizenId = (entity.getCitizen() != null) ? entity.getCitizen().getId() : null;
        String citizenName = (entity.getCitizen() != null) ? entity.getCitizen().getFullName() : null;

        IncidentResponse dto = new IncidentResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getStatus(),
                entity.getPriority(),
                citizenId,
                citizenName,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getAccuracyMeters(),
                entity.getAddress(),
                entity.getDistrict(),
                entity.getNeighborhood()
        );

        if (tareasRepository != null && assignmentMapper != null && entity.getId() != null) {
            List<Tareas> activeTasks = tareasRepository.findByIncidentIdAndIsActiveTrue(entity.getId());
            dto.setAssignments(assignmentMapper.toDtoList(activeTasks));
        }

        return dto;
    }

    /**
     * Convierte un objeto {@link Page} de entidades {@link Incidencia} en un {@link PageResponse} estructurado de {@link IncidentResponse}.
     *
     * @param page Pagina de entidades Spring Data origen, o {@code null}.
     * @return DTO {@link PageResponse} con los datos de paginacion y lista de elementos transformados.
     */
    public PageResponse<IncidentResponse> toPageResponse(Page<Incidencia> page) {
        if (page == null) {
            return new PageResponse<>(Collections.emptyList(), 0L, 0, 0, 0);
        }
        List<IncidentResponse> dtoList = toDtoList(page.getContent());
        return new PageResponse<>(
                dtoList,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }
}