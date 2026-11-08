package org.bulbaseros.mappers;

import org.bulbaseros.dto.IncidentAssetResponse;
import org.bulbaseros.entity.ActivoUrbano;
import org.bulbaseros.entity.AsociacionIncidenciaActivo;
import org.bulbaseros.entity.AsociacionIncidenciaActivoId;
import org.bulbaseros.entity.Incidencia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para {@link UrbanAssetMapper}.
 * <p>
 * Verifica la correcta transformación y aislamiento entre las entidades relacionales
 * {@link AsociacionIncidenciaActivo} y {@link ActivoUrbano} con respecto al DTO {@link IncidentAssetResponse}.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class UrbanAssetMapperTest {

    private final UrbanAssetMapper mapper = new UrbanAssetMapper();

    @Test
    @DisplayName("Debe retornar null cuando la entidad AsociacionIncidenciaActivo es nula")
    void shouldReturnNullWhenEntityIsNull() {
        assertNull(mapper.toDto((AsociacionIncidenciaActivo) null));
    }

    @Test
    @DisplayName("Debe mapear correctamente todos los campos desde AsociacionIncidenciaActivo a IncidentAssetResponse")
    void shouldMapAsociacionToDtoCorrectly() {
        Incidencia incident = new Incidencia();
        incident.setId(100L);

        ActivoUrbano asset = new ActivoUrbano();
        asset.setId(5L);
        asset.setAssetCode("SEM-MAL-001");
        asset.setType("SEMAFORO");
        asset.setName("Semáforo Alameda Principal");
        asset.setLatitude(36.7196);
        asset.setLongitude(-4.4200);

        AsociacionIncidenciaActivo association = new AsociacionIncidenciaActivo();
        association.setId(new AsociacionIncidenciaActivoId(100L, 5L));
        association.setIncident(incident);
        association.setAsset(asset);
        association.setDistanceMeters(18.5);
        association.setConfidenceScore(0.92);
        association.setIsConfirmed(true);

        IncidentAssetResponse dto = mapper.toDto(association);

        assertNotNull(dto);
        assertEquals(5L, dto.getAssetId());
        assertEquals("SEM-MAL-001", dto.getAssetCode());
        assertEquals("SEMAFORO", dto.getType());
        assertEquals("Semáforo Alameda Principal", dto.getName());
        assertEquals(36.7196, dto.getLatitude());
        assertEquals(-4.4200, dto.getLongitude());
        assertEquals(18.5, dto.getDistanceMeters());
        assertEquals(0.92, dto.getConfidenceScore());
        assertTrue(dto.getIsConfirmed());
    }

    @Test
    @DisplayName("Debe mapear correctamente cuando el activo de la asociacion es nulo pero existe clave compuesta")
    void shouldMapAsociacionWithNullAssetCorrectly() {
        AsociacionIncidenciaActivo association = new AsociacionIncidenciaActivo();
        association.setId(new AsociacionIncidenciaActivoId(200L, 12L));
        association.setAsset(null);
        association.setDistanceMeters(30.0);
        association.setConfidenceScore(0.65);
        association.setIsConfirmed(false);

        IncidentAssetResponse dto = mapper.toDto(association);

        assertNotNull(dto);
        assertEquals(12L, dto.getAssetId());
        assertNull(dto.getAssetCode());
        assertNull(dto.getType());
        assertNull(dto.getName());
        assertNull(dto.getLatitude());
        assertNull(dto.getLongitude());
        assertEquals(30.0, dto.getDistanceMeters());
        assertEquals(0.65, dto.getConfidenceScore());
        assertFalse(dto.getIsConfirmed());
    }

    @Test
    @DisplayName("Debe mapear directamente desde ActivoUrbano con parametros calculados")
    void shouldMapFromActivoUrbanoWithCalculatedMetrics() {
        ActivoUrbano asset = new ActivoUrbano();
        asset.setId(8L);
        asset.setAssetCode("FUE-MAL-025");
        asset.setType("FUENTE");
        asset.setName("Fuente de las Tres Gracias");
        asset.setLatitude(36.7180);
        asset.setLongitude(-4.4150);

        IncidentAssetResponse dto = mapper.toDto(asset, 12.3, 0.88, false);

        assertNotNull(dto);
        assertEquals(8L, dto.getAssetId());
        assertEquals("FUE-MAL-025", dto.getAssetCode());
        assertEquals("FUENTE", dto.getType());
        assertEquals("Fuente de las Tres Gracias", dto.getName());
        assertEquals(36.7180, dto.getLatitude());
        assertEquals(-4.4150, dto.getLongitude());
        assertEquals(12.3, dto.getDistanceMeters());
        assertEquals(0.88, dto.getConfidenceScore());
        assertFalse(dto.getIsConfirmed());
    }

    @Test
    @DisplayName("Debe retornar null al mapear desde ActivoUrbano si la entidad es nula")
    void shouldReturnNullWhenActivoUrbanoIsNull() {
        assertNull(mapper.toDto(null, 10.0, 0.5, false));
    }

    @Test
    @DisplayName("Debe mapear una lista de asociaciones a lista de DTOs correctamente")
    void shouldMapListCorrectly() {
        ActivoUrbano asset1 = new ActivoUrbano();
        asset1.setId(1L);
        asset1.setAssetCode("A1");
        asset1.setType("T1");

        AsociacionIncidenciaActivo assoc1 = new AsociacionIncidenciaActivo();
        assoc1.setAsset(asset1);
        assoc1.setDistanceMeters(10.0);
        assoc1.setConfidenceScore(0.9);
        assoc1.setIsConfirmed(true);

        ActivoUrbano asset2 = new ActivoUrbano();
        asset2.setId(2L);
        asset2.setAssetCode("A2");
        asset2.setType("T2");

        AsociacionIncidenciaActivo assoc2 = new AsociacionIncidenciaActivo();
        assoc2.setAsset(asset2);
        assoc2.setDistanceMeters(25.0);
        assoc2.setConfidenceScore(0.7);
        assoc2.setIsConfirmed(false);

        List<IncidentAssetResponse> dtos = mapper.toDtoList(List.of(assoc1, assoc2));

        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals(1L, dtos.get(0).getAssetId());
        assertEquals("A1", dtos.get(0).getAssetCode());
        assertEquals(2L, dtos.get(1).getAssetId());
        assertEquals("A2", dtos.get(1).getAssetCode());
    }

    @Test
    @DisplayName("Debe retornar lista vacia cuando la lista es nula o vacia")
    void shouldReturnEmptyListWhenListIsNullOrEmpty() {
        assertTrue(mapper.toDtoList(null).isEmpty());
        assertTrue(mapper.toDtoList(Collections.emptyList()).isEmpty());
    }
}
