package org.bulbaseros.mappers;

import org.bulbaseros.dto.IncidentResponse;
import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.PrioridadIncidencia;
import org.bulbaseros.entity.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para {@link IncidentMapper}.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class IncidentMapperTest {

    private final IncidentMapper mapper = new IncidentMapper();

    @Test
    @DisplayName("Debe retornar null cuando la entidad Incidencia es nula")
    void shouldReturnNullWhenEntityIsNull() {
        assertNull(mapper.toDto(null));
    }

    @Test
    @DisplayName("Debe mapear correctamente todos los campos de Incidencia a IncidentResponse")
    void shouldMapEntityToDtoCorrectly() {
        Usuario citizen = new Usuario();
        citizen.setId(10L);
        citizen.setFullName("Juan Perez");

        OffsetDateTime now = OffsetDateTime.now();
        Incidencia entity = new Incidencia();
        entity.setId(1L);
        entity.setTitle("Bache peligroso");
        entity.setDescription("Bache de gran profundidad");
        entity.setCategory("VIAS_PUBLICAS");
        entity.setLatitude(37.3891);
        entity.setLongitude(-5.9845);
        entity.setStatus(EstadoIncidencia.REPORTED);
        entity.setPriority(PrioridadIncidencia.HIGH);
        entity.setCitizen(citizen);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setAccuracyMeters(5.5);
        entity.setAddress("Calle Larios, 1");
        entity.setDistrict("Centro");
        entity.setNeighborhood("Centro Historico");

        IncidentResponse dto = mapper.toDto(entity);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Bache peligroso", dto.getTitle());
        assertEquals("Bache de gran profundidad", dto.getDescription());
        assertEquals("VIAS_PUBLICAS", dto.getCategory());
        assertEquals(37.3891, dto.getLatitude());
        assertEquals(-5.9845, dto.getLongitude());
        assertEquals(EstadoIncidencia.REPORTED, dto.getStatus());
        assertEquals(PrioridadIncidencia.HIGH, dto.getPriority());
        assertEquals(10L, dto.getCitizenId());
        assertEquals("Juan Perez", dto.getCitizenName());
        assertEquals(now, dto.getCreatedAt());
        assertEquals(now, dto.getUpdatedAt());
        assertEquals(5.5, dto.getAccuracyMeters());
        assertEquals("Calle Larios, 1", dto.getAddress());
        assertEquals("Centro", dto.getDistrict());
        assertEquals("Centro Historico", dto.getNeighborhood());
    }

    @Test
    @DisplayName("Debe convertir Page de entidades a PageResponse de DTOs correctamente")
    void shouldMapPageToPageResponseCorrectly() {
        assertNotNull(mapper.toPageResponse(null));
        assertTrue(mapper.toPageResponse(null).getContent().isEmpty());

        Incidencia inc1 = new Incidencia();
        inc1.setId(101L);
        inc1.setTitle("Incidencia 1");

        Incidencia inc2 = new Incidencia();
        inc2.setId(102L);
        inc2.setTitle("Incidencia 2");

        org.springframework.data.domain.Page<Incidencia> page = new org.springframework.data.domain.PageImpl<>(
                List.of(inc1, inc2),
                org.springframework.data.domain.PageRequest.of(0, 10),
                2
        );

        org.bulbaseros.dto.PageResponse<IncidentResponse> pageResponse = mapper.toPageResponse(page);

        assertNotNull(pageResponse);
        assertEquals(2, pageResponse.getTotalElements());
        assertEquals(1, pageResponse.getTotalPages());
        assertEquals(0, pageResponse.getPage());
        assertEquals(10, pageResponse.getSize());
        assertEquals(2, pageResponse.getContent().size());
        assertEquals(101L, pageResponse.getContent().get(0).getId());
        assertEquals(102L, pageResponse.getContent().get(1).getId());
    }

    @Test
    @DisplayName("Debe manejar ciudadano nulo de forma segura")
    void shouldHandleNullCitizenGracefully() {
        Incidencia entity = new Incidencia();
        entity.setId(2L);
        entity.setTitle("Alumbrado apagado");
        entity.setCitizen(null);

        IncidentResponse dto = mapper.toDto(entity);

        assertNotNull(dto);
        assertNull(dto.getCitizenId());
        assertNull(dto.getCitizenName());
    }

    @Test
    @DisplayName("Debe convertir listas de entidades de forma segura")
    void shouldConvertEntityListCorrectly() {
        assertTrue(mapper.toDtoList(null).isEmpty());
        assertTrue(mapper.toDtoList(Collections.emptyList()).isEmpty());

        Incidencia inc1 = new Incidencia();
        inc1.setId(1L);
        Incidencia inc2 = new Incidencia();
        inc2.setId(2L);

        List<IncidentResponse> dtoList = mapper.toDtoList(List.of(inc1, inc2));
        assertEquals(2, dtoList.size());
        assertEquals(1L, dtoList.get(0).getId());
        assertEquals(2L, dtoList.get(1).getId());
    }
}
