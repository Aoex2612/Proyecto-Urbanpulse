package org.bulbaseros.mappers;

import org.bulbaseros.dto.DepartmentResponse;
import org.bulbaseros.dto.UserSummaryResponse;
import org.bulbaseros.entity.Departamento;
import org.bulbaseros.entity.RolUsuario;
import org.bulbaseros.entity.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para {@link DepartmentMapper}.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class DepartmentMapperTest {

    private final DepartmentMapper mapper = new DepartmentMapper();

    @Test
    @DisplayName("Debe retornar null cuando la entidad Departamento es nula")
    void shouldReturnNullWhenEntityIsNull() {
        assertNull(mapper.toDto(null));
    }

    @Test
    @DisplayName("Debe mapear correctamente los campos de Departamento a DepartmentResponse")
    void shouldMapEntityToDtoCorrectly() {
        Departamento dept = new Departamento("Parques y Jardines", "Mantenimiento de zonas verdes");
        dept.setId(5L);

        DepartmentResponse dto = mapper.toDto(dept);

        assertNotNull(dto);
        assertEquals(5L, dto.getId());
        assertEquals("Parques y Jardines", dto.getName());
        assertEquals("Mantenimiento de zonas verdes", dto.getDescription());
    }

    @Test
    @DisplayName("Debe convertir DepartmentResponse a entidad Departamento mediante toEntity")
    void shouldConvertDtoToEntityCorrectly() {
        DepartmentResponse dto = DepartmentResponse.builder()
                .id(7L)
                .name("Limpieza")
                .description("Servicios de limpieza viaria")
                .build();

        Departamento entity = mapper.toEntity(dto);

        assertNotNull(entity);
        assertEquals(7L, entity.getId());
        assertEquals("Limpieza", entity.getName());
        assertEquals("Servicios de limpieza viaria", entity.getDescription());
    }

    @Test
    @DisplayName("Debe retornar null al invocar toEntity con DTO nulo")
    void shouldReturnNullWhenDtoIsNullInToEntity() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    @DisplayName("Debe mapear lista de departamentos a lista de DepartmentResponse")
    void shouldMapEntityListToDtoList() {
        Departamento d1 = new Departamento("D1", "Desc 1");
        d1.setId(1L);
        Departamento d2 = new Departamento("D2", "Desc 2");
        d2.setId(2L);

        List<DepartmentResponse> dtos = mapper.toDtoList(List.of(d1, d2));

        assertEquals(2, dtos.size());
        assertEquals(1L, dtos.get(0).getId());
        assertEquals("D1", dtos.get(0).getName());
        assertEquals(2L, dtos.get(1).getId());
        assertEquals("D2", dtos.get(1).getName());
    }

    @Test
    @DisplayName("Debe retornar lista vacía si la lista de departamentos es nula o vacía")
    void shouldReturnEmptyListWhenDepartmentListIsNullOrEmpty() {
        assertTrue(mapper.toDtoList(null).isEmpty());
        assertTrue(mapper.toDtoList(Collections.emptyList()).isEmpty());
    }

    @Test
    @DisplayName("Debe mapear correctamente Usuario a UserSummaryResponse")
    void shouldMapUserToUserSummaryDtoCorrectly() {
        Departamento dept = new Departamento("Alumbrado", "Red eléctrica");
        dept.setId(15L);

        Usuario user = new Usuario("lucas@urbanpulse.es", "hash", "Lucas Técnico");
        user.setId(22L);
        user.setRole(RolUsuario.TECHNICIAN);
        user.setDepartment(dept);

        UserSummaryResponse dto = mapper.toUserSummaryDto(user);

        assertNotNull(dto);
        assertEquals(22L, dto.getId());
        assertEquals("Lucas Técnico", dto.getFullName());
        assertEquals("lucas@urbanpulse.es", dto.getEmail());
        assertEquals(RolUsuario.TECHNICIAN, dto.getRole());
        assertEquals(15L, dto.getDepartmentId());
    }

    @Test
    @DisplayName("Debe retornar null cuando el Usuario es nulo en toUserSummaryDto")
    void shouldReturnNullWhenUserIsNullInToUserSummaryDto() {
        assertNull(mapper.toUserSummaryDto(null));
    }

    @Test
    @DisplayName("Debe mapear Usuario sin departamento asignado a UserSummaryResponse")
    void shouldMapUserWithoutDepartmentToUserSummaryDto() {
        Usuario user = new Usuario("admin@urbanpulse.es", "hash", "Admin Central");
        user.setId(1L);
        user.setRole(RolUsuario.ADMIN);

        UserSummaryResponse dto = mapper.toUserSummaryDto(user);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Admin Central", dto.getFullName());
        assertEquals(RolUsuario.ADMIN, dto.getRole());
        assertNull(dto.getDepartmentId());
    }

    @Test
    @DisplayName("Debe transformar lista de usuarios a lista de UserSummaryResponse")
    void shouldMapUserListToUserSummaryDtoList() {
        Usuario u1 = new Usuario("u1@test.es", "h", "U1");
        u1.setId(1L);
        Usuario u2 = new Usuario("u2@test.es", "h", "U2");
        u2.setId(2L);

        List<UserSummaryResponse> dtos = mapper.toUserSummaryDtoList(List.of(u1, u2));

        assertEquals(2, dtos.size());
        assertEquals(1L, dtos.get(0).getId());
        assertEquals(2L, dtos.get(1).getId());
    }

    @Test
    @DisplayName("Debe retornar lista vacía si la lista de usuarios es nula o vacía")
    void shouldReturnEmptyListWhenUserListIsNullOrEmpty() {
        assertTrue(mapper.toUserSummaryDtoList(null).isEmpty());
        assertTrue(mapper.toUserSummaryDtoList(Collections.emptyList()).isEmpty());
    }
}
