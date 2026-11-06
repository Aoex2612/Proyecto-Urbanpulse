package org.bulbaseros.mappers;

import org.bulbaseros.dto.AssignmentResponse;
import org.bulbaseros.entity.Departamento;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.RolUsuario;
import org.bulbaseros.entity.Tareas;
import org.bulbaseros.entity.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para {@link AssignmentMapper}.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class AssignmentMapperTest {

    private final AssignmentMapper mapper = new AssignmentMapper();

    @Test
    @DisplayName("Debe retornar null cuando la entidad Tareas es nula")
    void shouldReturnNullWhenEntityIsNull() {
        assertNull(mapper.toDto(null));
    }

    @Test
    @DisplayName("Debe mapear correctamente todos los campos de Tareas a AssignmentResponse")
    void shouldMapEntityToDtoCorrectly() {
        Incidencia incident = new Incidencia();
        incident.setId(10L);

        Departamento department = new Departamento("Vías Públicas", "Reparación de calzadas");
        department.setId(20L);

        Usuario technician = new Usuario("tech@urbanpulse.es", "hash", "Técnico Juan");
        technician.setId(30L);
        technician.setRole(RolUsuario.TECHNICIAN);

        Usuario assignedBy = new Usuario("op@urbanpulse.es", "hash", "Operador Ana");
        assignedBy.setId(40L);
        assignedBy.setRole(RolUsuario.OPERATOR);

        OffsetDateTime now = OffsetDateTime.now();
        Tareas tarea = new Tareas();
        tarea.setId(1L);
        tarea.setIncident(incident);
        tarea.setDepartment(department);
        tarea.setTechnician(technician);
        tarea.setAssignedBy(assignedBy);
        tarea.setIsActive(true);
        tarea.setAssignedAt(now);

        AssignmentResponse dto = mapper.toDto(tarea);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(20L, dto.getDepartmentId());
        assertEquals("Vías Públicas", dto.getDepartmentName());
        assertEquals(30L, dto.getTechnicianId());
        assertEquals("Técnico Juan", dto.getTechnicianName());
        assertEquals(40L, dto.getAssignedById());
        assertEquals("Operador Ana", dto.getAssignedByName());
        assertTrue(dto.getIsActive());
        assertEquals(now, dto.getAssignedAt());
    }

    @Test
    @DisplayName("Debe mapear correctamente Tareas sin técnico asignado y relaciones nulas")
    void shouldMapEntityWithNullRelationsCorrectly() {
        Tareas tarea = new Tareas();
        tarea.setId(2L);
        tarea.setIsActive(false);

        AssignmentResponse dto = mapper.toDto(tarea);

        assertNotNull(dto);
        assertEquals(2L, dto.getId());
        assertNull(dto.getDepartmentId());
        assertNull(dto.getDepartmentName());
        assertNull(dto.getTechnicianId());
        assertNull(dto.getTechnicianName());
        assertNull(dto.getAssignedById());
        assertNull(dto.getAssignedByName());
        assertFalse(dto.getIsActive());
    }

    @Test
    @DisplayName("Debe transformar lista de Tareas a lista de AssignmentResponse")
    void shouldMapEntityListToDtoList() {
        Tareas tarea1 = new Tareas();
        tarea1.setId(1L);
        Tareas tarea2 = new Tareas();
        tarea2.setId(2L);

        List<AssignmentResponse> dtos = mapper.toDtoList(List.of(tarea1, tarea2));

        assertEquals(2, dtos.size());
        assertEquals(1L, dtos.get(0).getId());
        assertEquals(2L, dtos.get(1).getId());
    }

    @Test
    @DisplayName("Debe retornar lista vacía si la lista de entidades es nula o vacía")
    void shouldReturnEmptyListWhenInputIsNullOrEmpty() {
        assertTrue(mapper.toDtoList(null).isEmpty());
        assertTrue(mapper.toDtoList(Collections.emptyList()).isEmpty());
    }

    @Test
    @DisplayName("Debe construir una entidad Tareas mediante toEntity")
    void shouldConstructEntityViaToEntity() {
        Incidencia incident = new Incidencia();
        incident.setId(10L);
        Departamento department = new Departamento("Alumbrado", "Farolas y electricidad");
        Usuario technician = new Usuario("tech@test.es", "hash", "Tech");
        Usuario assignedBy = new Usuario("op@test.es", "hash", "Op");

        Tareas tarea = mapper.toEntity(incident, department, technician, assignedBy);

        assertNotNull(tarea);
        assertEquals(incident, tarea.getIncident());
        assertEquals(department, tarea.getDepartment());
        assertEquals(technician, tarea.getTechnician());
        assertEquals(assignedBy, tarea.getAssignedBy());
        assertTrue(tarea.getIsActive());
        assertNotNull(tarea.getAssignedAt());
    }
}
