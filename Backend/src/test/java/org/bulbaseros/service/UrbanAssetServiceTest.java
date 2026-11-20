package org.bulbaseros.service;

import org.bulbaseros.dto.IncidentAssetResponse;
import org.bulbaseros.entity.ActivoUrbano;
import org.bulbaseros.entity.AsociacionIncidenciaActivo;
import org.bulbaseros.entity.AsociacionIncidenciaActivoId;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.mappers.UrbanAssetMapper;
import org.bulbaseros.repository.ActivoUrbanoRepository;
import org.bulbaseros.repository.AsociacionIncidenciaActivoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link UrbanAssetService}.
 * <p>
 * Verifica de forma rigurosa la lógica de cálculo geodésico de distancias con la fórmula de Haversine,
 * la normalización matemática del índice de confianza probabilístico, el algoritmo de prefiltrado y
 * vinculación de activos contiguos (<= 50 metros), y las operaciones de confirmación y desvinculación
 * con control de excepciones HTTP 404 (RF-1, RF-2, RF-3, RF-4, RF-6, RF-7).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class UrbanAssetServiceTest {

    @Mock
    private ActivoUrbanoRepository activoUrbanoRepository;

    @Mock
    private AsociacionIncidenciaActivoRepository asociacionRepository;

    @Mock
    private UrbanAssetMapper urbanAssetMapper;

    @InjectMocks
    private UrbanAssetService urbanAssetService;

    private Incidencia incident;
    private ActivoUrbano asset1;
    private ActivoUrbano asset2;
    private ActivoUrbano assetFar;

    @BeforeEach
    void setUp() {
        // Coordenadas base en Málaga: Alameda Principal (aprox. 36.7196, -4.4200)
        incident = new Incidencia();
        incident.setId(10L);
        incident.setLatitude(36.719600);
        incident.setLongitude(-4.420000);

        // Activo 1: A escasos metros (~11 metros al norte)
        // 0.0001 grados latitud ~ 11.13 metros
        asset1 = new ActivoUrbano();
        asset1.setId(101L);
        asset1.setAssetCode("SEM-MAL-001");
        asset1.setType("SEMAFORO");
        asset1.setName("Semáforo Alameda Norte");
        asset1.setLatitude(36.719700);
        asset1.setLongitude(-4.420000);

        // Activo 2: A ~35 metros al este
        // deltaLon = 35 / (111320 * cos(36.7196)) ~ 35 / 89230 ~ 0.000392 grados
        asset2 = new ActivoUrbano();
        asset2.setId(102L);
        asset2.setAssetCode("FUE-MAL-002");
        asset2.setType("FUENTE");
        asset2.setName("Fuente Alameda Este");
        asset2.setLatitude(36.719600);
        asset2.setLongitude(-4.419608);

        // Activo Lejano: A ~150 metros (fuera del radio de 50m)
        assetFar = new ActivoUrbano();
        assetFar.setId(103L);
        assetFar.setAssetCode("CON-MAL-003");
        assetFar.setType("CONTENEDOR");
        assetFar.setName("Contenedor Lejano");
        assetFar.setLatitude(36.721000);
        assetFar.setLongitude(-4.420000);
    }

    // ---------------------------------------------------------------------------------------------
    // PRUEBAS DE CÁLCULO DE DISTANCIA (HAVERSINE)
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("Debe calcular distancia cero cuando ambos puntos tienen coordenadas idénticas")
    void shouldReturnZeroDistanceForIdenticalCoordinates() {
        double distance = urbanAssetService.calculateDistanceMeters(36.7196, -4.4200, 36.7196, -4.4200);
        assertEquals(0.0, distance, 0.01);
    }

    @Test
    @DisplayName("Debe calcular la distancia geodésica correcta entre coordenadas conocidas de Málaga")
    void shouldCalculateAccurateDistanceBetweenKnownPoints() {
        // Distancia aproximada conocida entre Alameda Principal (36.7196, -4.4200) y Plaza de la Marina (36.7180, -4.4180)
        // Delta aprox: ~250 metros
        double distance = urbanAssetService.calculateDistanceMeters(36.7196, -4.4200, 36.7180, -4.4180);

        assertTrue(distance > 240.0 && distance < 265.0,
                "La distancia calculada debe estar entre 240m y 265m, resultado obtenido: " + distance);
    }

    @Test
    @DisplayName("Debe ser conmutativa la función de cálculo de distancia de Haversine")
    void shouldBeCommutativeDistanceCalculation() {
        double distAB = urbanAssetService.calculateDistanceMeters(36.7196, -4.4200, 36.7250, -4.4300);
        double distBA = urbanAssetService.calculateDistanceMeters(36.7250, -4.4300, 36.7196, -4.4200);

        assertEquals(distAB, distBA, 0.001);
    }

    // ---------------------------------------------------------------------------------------------
    // PRUEBAS DE SCORE DE CONFIANZA
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("Debe calcular score 1.0 para distancia 0 metros")
    void shouldReturnMaxConfidenceForZeroDistance() {
        double score = urbanAssetService.calculateConfidenceScore(0.0);
        assertEquals(1.0, score, 0.0001);
    }

    @Test
    @DisplayName("Debe calcular score 0.5 para distancia media de 25 metros")
    void shouldReturnHalfConfidenceForHalfMaxDistance() {
        double score = urbanAssetService.calculateConfidenceScore(25.0);
        assertEquals(0.5, score, 0.0001);
    }

    @Test
    @DisplayName("Debe calcular score 0.0 para distancia límite de 50 metros")
    void shouldReturnZeroConfidenceForMaxRadiusDistance() {
        double score = urbanAssetService.calculateConfidenceScore(50.0);
        assertEquals(0.0, score, 0.0001);
    }

    @Test
    @DisplayName("Debe acotar el score a 0.0 para distancias superiores al radio máximo de 50 metros")
    void shouldClampConfidenceToZeroForDistancesExceedingLimit() {
        double score = urbanAssetService.calculateConfidenceScore(75.5);
        assertEquals(0.0, score, 0.0001);

        double scoreWayFar = urbanAssetService.calculateConfidenceScore(1000.0);
        assertEquals(0.0, scoreWayFar, 0.0001);
    }

    @Test
    @DisplayName("Debe manejar distancias negativas acotándolas con seguridad a 1.0")
    void shouldHandleNegativeDistancesGracefully() {
        double score = urbanAssetService.calculateConfidenceScore(-5.0);
        assertEquals(1.0, score, 0.0001);
    }

    // ---------------------------------------------------------------------------------------------
    // PRUEBAS DE ASOCIACIÓN DE ACTIVOS PRÓXIMOS (associateNearbyAssets)
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("RF-1 y RF-2: Debe asociar activos dentro del radio de 50m y descartar los lejanos, ordenando por distancia")
    void shouldAssociateNearbyAssetsAndExcludeFarOnes() {
        // Configuración de candidatos devueltos por el bounding box
        when(activoUrbanoRepository.findByLatitudeBetweenAndLongitudeBetween(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(assetFar, asset2, asset1)); // En orden desordenado

        when(asociacionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        IncidentAssetResponse dto1 = IncidentAssetResponse.builder().assetId(101L).distanceMeters(11.12).confidenceScore(0.78).build();
        IncidentAssetResponse dto2 = IncidentAssetResponse.builder().assetId(102L).distanceMeters(34.98).confidenceScore(0.30).build();
        when(urbanAssetMapper.toDtoList(anyList())).thenReturn(List.of(dto1, dto2));

        List<IncidentAssetResponse> result = urbanAssetService.associateNearbyAssets(incident);

        assertNotNull(result);
        assertEquals(2, result.size());

        // Verificar que saveAll fue invocado con 2 asociaciones válidas (asset1 y asset2) y sin assetFar
        ArgumentCaptor<List<AsociacionIncidenciaActivo>> captor = ArgumentCaptor.forClass(List.class);
        verify(asociacionRepository).saveAll(captor.capture());
        List<AsociacionIncidenciaActivo> savedList = captor.getValue();

        assertEquals(2, savedList.size());

        // Debe estar ordenado de menor a mayor distancia: asset1 (~11m) antes que asset2 (~35m)
        assertEquals(101L, savedList.get(0).getAsset().getId());
        assertEquals(102L, savedList.get(1).getAsset().getId());
        assertTrue(savedList.get(0).getDistanceMeters() < savedList.get(1).getDistanceMeters());

        // Todas las asociaciones iniciales deben tener isConfirmed = false
        assertFalse(savedList.get(0).getIsConfirmed());
        assertFalse(savedList.get(1).getIsConfirmed());

        // Comprobar claves compuestas y relación bidireccional
        assertEquals(incident.getId(), savedList.get(0).getId().getIncidentId());
        assertEquals(101L, savedList.get(0).getId().getAssetId());
    }

    @Test
    @DisplayName("RF-6: Si no hay activos en el radio, debe retornar lista vacía sin fallar ni persistir")
    void shouldReturnEmptyListWhenNoAssetsWithinRadius() {
        // Caso 1: Repositorio no encuentra nada en el bounding box
        when(activoUrbanoRepository.findByLatitudeBetweenAndLongitudeBetween(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Collections.emptyList());

        List<IncidentAssetResponse> result = urbanAssetService.associateNearbyAssets(incident);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(asociacionRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("RF-6: Si los activos devueltos por el bounding box están a más de 50 metros, debe retornar lista vacía")
    void shouldReturnEmptyListWhenBoundingBoxAssetsAreBeyondFiftyMeters() {
        when(activoUrbanoRepository.findByLatitudeBetweenAndLongitudeBetween(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(assetFar));

        when(asociacionRepository.saveAll(Collections.emptyList())).thenReturn(Collections.emptyList());
        when(urbanAssetMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

        List<IncidentAssetResponse> result = urbanAssetService.associateNearbyAssets(incident);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Debe retornar lista vacía si la incidencia es nula o carece de coordenadas")
    void shouldReturnEmptyListWhenIncidentOrCoordinatesAreNull() {
        assertTrue(urbanAssetService.associateNearbyAssets(null).isEmpty());

        Incidencia incomplete = new Incidencia();
        incomplete.setId(1L);
        assertTrue(urbanAssetService.associateNearbyAssets(incomplete).isEmpty());

        verifyNoInteractions(activoUrbanoRepository);
        verifyNoInteractions(asociacionRepository);
    }

    // ---------------------------------------------------------------------------------------------
    // PRUEBAS DE CONFIRMACIÓN DE ASOCIACIÓN (confirmAssetAssociation)
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("RF-3: Debe confirmar con éxito una asociación existente y retornar el DTO actualizado")
    void shouldConfirmAssetAssociationSuccessfully() {
        Long incidentId = 10L;
        Long assetId = 101L;

        AsociacionIncidenciaActivo association = new AsociacionIncidenciaActivo();
        association.setIsConfirmed(false);
        association.setDistanceMeters(11.12);
        association.setConfidenceScore(0.78);

        when(asociacionRepository.findByIncidentIdAndAssetId(incidentId, assetId))
                .thenReturn(Optional.of(association));
        when(asociacionRepository.save(any(AsociacionIncidenciaActivo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IncidentAssetResponse expectedDto = IncidentAssetResponse.builder()
                .assetId(assetId)
                .isConfirmed(true)
                .build();
        when(urbanAssetMapper.toDto(any(AsociacionIncidenciaActivo.class))).thenReturn(expectedDto);

        IncidentAssetResponse result = urbanAssetService.confirmAssetAssociation(incidentId, assetId);

        assertNotNull(result);
        assertTrue(result.getIsConfirmed());
        assertTrue(association.getIsConfirmed());
        verify(asociacionRepository).save(association);
    }

    @Test
    @DisplayName("RF-7: Debe lanzar ResponseStatusException (404 NOT FOUND) al confirmar activo inexistente")
    void shouldThrowNotFoundWhenConfirmingNonExistentAssociation() {
        Long incidentId = 10L;
        Long assetId = 999L;

        when(asociacionRepository.findByIncidentIdAndAssetId(incidentId, assetId))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                urbanAssetService.confirmAssetAssociation(incidentId, assetId)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("No se encontró la asociación"));
        verify(asociacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar HTTP 400 Bad Request si los IDs de confirmación son nulos")
    void shouldThrowBadRequestWhenConfirmIdsAreNull() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                urbanAssetService.confirmAssetAssociation(null, 101L)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());

        ResponseStatusException ex2 = assertThrows(ResponseStatusException.class, () ->
                urbanAssetService.confirmAssetAssociation(10L, null)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex2.getStatusCode());
    }

    // ---------------------------------------------------------------------------------------------
    // PRUEBAS DE DESVINCULACIÓN DE ASOCIACIÓN (removeAssetAssociation)
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("RF-4: Debe eliminar la asociación existente correctamente")
    void shouldRemoveAssetAssociationSuccessfully() {
        Long incidentId = 10L;
        Long assetId = 101L;

        AsociacionIncidenciaActivo association = new AsociacionIncidenciaActivo();
        when(asociacionRepository.findByIncidentIdAndAssetId(incidentId, assetId))
                .thenReturn(Optional.of(association));

        urbanAssetService.removeAssetAssociation(incidentId, assetId);

        verify(asociacionRepository).deleteByIncidentIdAndAssetId(incidentId, assetId);
    }

    @Test
    @DisplayName("RF-4 y RF-7: Debe lanzar ResponseStatusException (404 NOT FOUND) al eliminar asociación inexistente")
    void shouldThrowNotFoundWhenRemovingNonExistentAssociation() {
        Long incidentId = 10L;
        Long assetId = 999L;

        when(asociacionRepository.findByIncidentIdAndAssetId(incidentId, assetId))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                urbanAssetService.removeAssetAssociation(incidentId, assetId)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(asociacionRepository, never()).deleteByIncidentIdAndAssetId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Debe lanzar HTTP 400 Bad Request si los IDs de desvinculación son nulos")
    void shouldThrowBadRequestWhenRemoveIdsAreNull() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                urbanAssetService.removeAssetAssociation(null, 101L)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    // ---------------------------------------------------------------------------------------------
    // PRUEBAS DE CONSULTA POR INCIDENCIA (getAssetsByIncidentId)
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("RF-5: Debe recuperar y ordenar los activos vinculados a una incidencia")
    void shouldRetrieveAndSortAssetsByIncidentId() {
        Long incidentId = 10L;

        AsociacionIncidenciaActivo assoc1 = new AsociacionIncidenciaActivo();
        assoc1.setDistanceMeters(30.0);

        AsociacionIncidenciaActivo assoc2 = new AsociacionIncidenciaActivo();
        assoc2.setDistanceMeters(10.0);

        when(asociacionRepository.findByIncidentId(incidentId)).thenReturn(new java.util.ArrayList<>(List.of(assoc1, assoc2)));

        IncidentAssetResponse dto1 = IncidentAssetResponse.builder().distanceMeters(10.0).build();
        IncidentAssetResponse dto2 = IncidentAssetResponse.builder().distanceMeters(30.0).build();
        when(urbanAssetMapper.toDtoList(anyList())).thenReturn(List.of(dto1, dto2));

        List<IncidentAssetResponse> result = urbanAssetService.getAssetsByIncidentId(incidentId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(10.0, result.get(0).getDistanceMeters());
    }

    @Test
    @DisplayName("RF-5: Debe retornar lista vacía si no hay asociaciones para la incidencia o el id es nulo")
    void shouldReturnEmptyListWhenNoAssociationsOrNullId() {
        assertTrue(urbanAssetService.getAssetsByIncidentId(null).isEmpty());

        when(asociacionRepository.findByIncidentId(999L)).thenReturn(Collections.emptyList());
        assertTrue(urbanAssetService.getAssetsByIncidentId(999L).isEmpty());
    }
}
