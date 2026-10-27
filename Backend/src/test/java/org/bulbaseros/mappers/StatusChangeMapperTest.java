package org.bulbaseros.mappers;

import org.bulbaseros.dto.StatusChangeResponse;
import org.bulbaseros.entity.CambiosEstado;
import org.bulbaseros.entity.Incidencia;
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
 * Pruebas unitarias para {@link StatusChangeMapper}.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class StatusChangeMapperTest {

    private final StatusChangeMapper mapper = new StatusChangeMapper();

    @Test
    @DisplayName("Debe retornar null cuando la entidad CambiosEstado es nula")
    void shouldReturnNullWhenEntityIsNull() {
        assertNull(mapper.toDto(null));
    }

    @Test
    @DisplayName("Debe mapear correctamente todos los campos de CambiosEstado a StatusChangeResponse")
    void shouldMapEntityToDtoCorrectly() {
        Incidencia incident = new Incidencia();
        incident.setId(5L);

        Usuario user = new Usuario();
        user.setId(20L);
        user.setFullName("Operador Gomez");
        user.setEmail("operador@urbanpulse.es");

        OffsetDateTime now = OffsetDateTime.now();
        CambiosEstado cambio = new CambiosEstado();
        cambio.setId(100L);
        cambio.setIncident(incident);
        cambio.setFromStatus("REPORTED");
        cambio.setToStatus("VALIDATED");
        cambio.setChangedByUser(user);
        cambio.setReason("Incidencia verificada correctamente");
        cambio.setCreatedAt(now);

        StatusChangeResponse dto = mapper.toDto(cambio);

        assertNotNull(dto);
        assertEquals(100L, dto.getId());
        assertEquals(5L, dto.getIncidentId());
        assertEquals("REPORTED", dto.getFromStatus());
        assertEquals("VALIDATED", dto.getToStatus());
        assertEquals(20L, dto.getChangedByUserId());
        assertEquals("Operador Gomez", dto.getChangedByUserName());
        assertEquals("operador@urbanpulse.es", dto.getChangedByUserEmail());
        assertEquals("Incidencia verificada correctamente", dto.getReason());
        assertEquals(now, dto.getCreatedAt());
    }

    @Test
    @DisplayName("Debe manejar relaciones nulas (incidencia o usuario) de forma segura")
    void shouldHandleNullRelationsGracefully() {
        CambiosEstado cambio = new CambiosEstado();
        cambio.setId(101L);
        cambio.setIncident(null);
        cambio.setChangedByUser(null);

        StatusChangeResponse dto = mapper.toDto(cambio);

        assertNotNull(dto);
        assertNull(dto.getIncidentId());
        assertNull(dto.getChangedByUserId());
        assertNull(dto.getChangedByUserName());
        assertNull(dto.getChangedByUserEmail());
    }

    @Test
    @DisplayName("Debe convertir listas de transiciones de forma segura")
    void shouldConvertEntityListCorrectly() {
        assertTrue(mapper.toDtoList(null).isEmpty());
        assertTrue(mapper.toDtoList(Collections.emptyList()).isEmpty());

        CambiosEstado c1 = new CambiosEstado();
        c1.setId(1L);
        CambiosEstado c2 = new CambiosEstado();
        c2.setId(2L);

        List<StatusChangeResponse> dtoList = mapper.toDtoList(List.of(c1, c2));
        assertEquals(2, dtoList.size());
        assertEquals(1L, dtoList.get(0).getId());
        assertEquals(2L, dtoList.get(1).getId());
    }
}
