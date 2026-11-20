package org.bulbaseros.mappers;

import org.bulbaseros.dto.ExternalObservationResponse;
import org.bulbaseros.entity.ObservacionesExternas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para {@link UrbanContextMapper}.
 * <p>
 * Verifica la correcta transformación y aislamiento entre la entidad relacional
 * {@link ObservacionesExternas} y el DTO {@link ExternalObservationResponse}.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class UrbanContextMapperTest {

    private final UrbanContextMapper mapper = new UrbanContextMapper();

    @Test
    @DisplayName("Debe retornar null cuando la entidad ObservacionesExternas es nula")
    void shouldReturnNullWhenEntityIsNull() {
        assertNull(mapper.toDto(null));
    }

    @Test
    @DisplayName("Debe mapear correctamente todos los campos desde ObservacionesExternas a ExternalObservationResponse")
    void shouldMapEntityToDtoCorrectly() {
        OffsetDateTime now = OffsetDateTime.now();
        ObservacionesExternas entity = new ObservacionesExternas();
        entity.setId(15L);
        entity.setSource("OPEN_METEO");
        entity.setMetricType("TEMPERATURE_C");
        entity.setValueNum(23.5);
        entity.setValueText("WARM");
        entity.setObservedAt(now);

        ExternalObservationResponse dto = mapper.toDto(entity);

        assertNotNull(dto);
        assertEquals(15L, dto.getId());
        assertEquals("OPEN_METEO", dto.getSource());
        assertEquals("TEMPERATURE_C", dto.getMetricType());
        assertEquals(23.5, dto.getValueNum());
        assertEquals("WARM", dto.getValueText());
        assertEquals(now, dto.getObservedAt());
    }

    @Test
    @DisplayName("Debe mapear una entidad con valores nulos opcionales sin errores")
    void shouldMapEntityWithPartialDataCorrectly() {
        OffsetDateTime now = OffsetDateTime.now();
        ObservacionesExternas entity = new ObservacionesExternas();
        entity.setId(20L);
        entity.setSource("DGT_TRAFFIC");
        entity.setMetricType("TRAFFIC_LEVEL");
        entity.setValueNum(null);
        entity.setValueText("CONGESTED");
        entity.setObservedAt(now);

        ExternalObservationResponse dto = mapper.toDto(entity);

        assertNotNull(dto);
        assertEquals(20L, dto.getId());
        assertEquals("DGT_TRAFFIC", dto.getSource());
        assertEquals("TRAFFIC_LEVEL", dto.getMetricType());
        assertNull(dto.getValueNum());
        assertEquals("CONGESTED", dto.getValueText());
        assertEquals(now, dto.getObservedAt());
    }

    @Test
    @DisplayName("Debe mapear una lista de observaciones externas a lista de DTOs correctamente")
    void shouldMapListCorrectly() {
        OffsetDateTime now = OffsetDateTime.now();

        ObservacionesExternas obs1 = new ObservacionesExternas();
        obs1.setId(1L);
        obs1.setSource("OPEN_METEO");
        obs1.setMetricType("TEMPERATURE_C");
        obs1.setValueNum(20.0);
        obs1.setObservedAt(now);

        ObservacionesExternas obs2 = new ObservacionesExternas();
        obs2.setId(2L);
        obs2.setSource("OPEN_METEO");
        obs2.setMetricType("PRECIPITATION_MM");
        obs2.setValueNum(0.0);
        obs2.setObservedAt(now);

        List<ExternalObservationResponse> dtos = mapper.toDtoList(List.of(obs1, obs2));

        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals(1L, dtos.get(0).getId());
        assertEquals("TEMPERATURE_C", dtos.get(0).getMetricType());
        assertEquals(2L, dtos.get(1).getId());
        assertEquals("PRECIPITATION_MM", dtos.get(1).getMetricType());
    }

    @Test
    @DisplayName("Debe retornar lista vacia cuando la lista es nula o vacia")
    void shouldReturnEmptyListWhenListIsNullOrEmpty() {
        assertTrue(mapper.toDtoList(null).isEmpty());
        assertTrue(mapper.toDtoList(Collections.emptyList()).isEmpty());
    }
}
