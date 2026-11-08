package org.bulbaseros.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bulbaseros.dto.ExternalObservationResponse;
import org.bulbaseros.entity.ContextoIncidencia;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.ObservacionesExternas;
import org.bulbaseros.mappers.UrbanContextMapper;
import org.bulbaseros.repository.ContextoIncidenciaRepository;
import org.bulbaseros.repository.ObservacionesExternasRepository;
import org.bulbaseros.service.resilience.CircuitBreaker;
import org.bulbaseros.service.resilience.CircuitState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExternalContextService - Pruebas Unitarias de Ingesta, Circuit Breaker y Fallback")
class ExternalContextServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CircuitBreaker circuitBreaker;

    @Mock
    private ObservacionesExternasRepository observacionesRepository;

    @Mock
    private ContextoIncidenciaRepository contextoIncidenciaRepository;

    private UrbanContextMapper urbanContextMapper;
    private ObjectMapper objectMapper;
    private ExternalContextService externalContextService;

    private Incidencia testIncident;

    @BeforeEach
    void setUp() {
        urbanContextMapper = new UrbanContextMapper();
        objectMapper = new ObjectMapper();

        externalContextService = new ExternalContextService(
                restTemplate,
                circuitBreaker,
                observacionesRepository,
                contextoIncidenciaRepository,
                urbanContextMapper,
                objectMapper
        );

        testIncident = new Incidencia();
        testIncident.setId(100L);
        testIncident.setTitle("Farola rota en Alameda Principal");
        testIncident.setLatitude(36.7196);
        testIncident.setLongitude(-4.4200);
    }

    @Test
    @DisplayName("Debe capturar clima exitosamente, persistir observaciones y retornar DTOs")
    void shouldCaptureAndSaveWeatherContextSuccessfully() {
        // Dado que el circuit breaker permite ejecución
        when(circuitBreaker.allowExecution()).thenReturn(true);

        String jsonResponse = """
                {
                  "latitude": 36.72,
                  "longitude": -4.42,
                  "current": {
                    "time": "2026-11-08T20:00",
                    "interval": 900,
                    "temperature_2m": 19.5,
                    "precipitation": 0.0,
                    "wind_speed_10m": 12.3
                  }
                }
                """;

        ResponseEntity<String> weatherEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);
        when(restTemplate.getForEntity(contains("open-meteo"), eq(String.class))).thenReturn(weatherEntity);

        // Tráfico falla o no disponible (simulación 404 o timeout suave)
        when(restTemplate.getForEntity(contains("trafico"), eq(String.class)))
                .thenThrow(new ResourceAccessException("Traffic API unavailable"));

        when(observacionesRepository.save(any(ObservacionesExternas.class))).thenAnswer(invocation -> {
            ObservacionesExternas obs = invocation.getArgument(0);
            obs.setId(1L);
            return obs;
        });

        // Cuando
        List<ExternalObservationResponse> result = externalContextService.captureAndSaveContext(testIncident);

        // Entonces
        assertNotNull(result);
        assertEquals(3, result.size()); // temperatura, lluvia, viento
        verify(circuitBreaker).recordSuccess();
        verify(observacionesRepository, times(3)).save(any(ObservacionesExternas.class));
        verify(contextoIncidenciaRepository, times(3)).save(any(ContextoIncidencia.class));

        ExternalObservationResponse tempDto = result.stream()
                .filter(r -> "TEMPERATURE".equals(r.getMetricType()))
                .findFirst()
                .orElse(null);
        assertNotNull(tempDto);
        assertEquals(19.5, tempDto.getValueNum());
        assertEquals("Open-Meteo", tempDto.getSource());
    }

    @Test
    @DisplayName("Debe activar Fallback silencioso y retornar lista vacía ante SocketTimeoutException (RF-12, RF-13)")
    void shouldFallbackSilentlyOnSocketTimeout() {
        when(circuitBreaker.allowExecution()).thenReturn(true);

        // Simular timeout estricto de conexión/lectura (SocketTimeoutException)
        when(restTemplate.getForEntity(contains("open-meteo"), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection timed out", new SocketTimeoutException("Read timed out")));

        when(restTemplate.getForEntity(contains("trafico"), eq(String.class)))
                .thenThrow(new ResourceAccessException("Traffic API timeout"));

        // Cuando
        List<ExternalObservationResponse> result = externalContextService.captureAndSaveContext(testIncident);

        // Entonces: no propaga excepción, registra fallo en Circuit Breaker y retorna lista vacía
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(circuitBreaker).recordFailure();
        verify(observacionesRepository, never()).save(any());
        verify(contextoIncidenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe activar Fallback silencioso ante error HTTP 500 del servicio remoto")
    void shouldFallbackSilentlyOnHttpServerError() {
        when(circuitBreaker.allowExecution()).thenReturn(true);

        when(restTemplate.getForEntity(contains("open-meteo"), eq(String.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error"));

        when(restTemplate.getForEntity(contains("trafico"), eq(String.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY, "Bad Gateway"));

        // Cuando
        List<ExternalObservationResponse> result = externalContextService.captureAndSaveContext(testIncident);

        // Entonces
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(circuitBreaker).recordFailure();
        verify(observacionesRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe degradar de inmediato sin invocar la red si el Circuit Breaker está OPEN (RF-14)")
    void shouldDegradeImmediatelyWithoutNetworkCallWhenCircuitBreakerIsOpen() {
        // Circuito abierto: allowExecution() retorna false
        when(circuitBreaker.allowExecution()).thenReturn(false);

        // Cuando
        List<ExternalObservationResponse> result = externalContextService.captureAndSaveContext(testIncident);

        // Entonces
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(restTemplate, never()).getForEntity(anyString(), eq(String.class));
        verify(circuitBreaker, never()).recordSuccess();
        verify(circuitBreaker, never()).recordFailure();
    }

    @Test
    @DisplayName("Debe retornar lista vacía si la incidencia es nula o carece de ID o coordenadas")
    void shouldReturnEmptyListWhenIncidentOrCoordinatesAreMissing() {
        assertTrue(externalContextService.captureAndSaveContext(null).isEmpty());

        Incidencia noId = new Incidencia();
        noId.setLatitude(36.7);
        noId.setLongitude(-4.4);
        assertTrue(externalContextService.captureAndSaveContext(noId).isEmpty());

        Incidencia noCoords = new Incidencia();
        noCoords.setId(1L);
        assertTrue(externalContextService.captureAndSaveContext(noCoords).isEmpty());
    }

    @Test
    @DisplayName("Debe recuperar observaciones por incidentId")
    void shouldRetrieveObservationsByIncidentId() {
        ObservacionesExternas obs = new ObservacionesExternas();
        obs.setId(10L);
        obs.setSource("Open-Meteo");
        obs.setMetricType("TEMPERATURE");
        obs.setValueNum(21.0);
        obs.setObservedAt(OffsetDateTime.now());

        ContextoIncidencia ci = new ContextoIncidencia();
        ci.setObservation(obs);

        when(contextoIncidenciaRepository.findByIncidentId(100L)).thenReturn(List.of(ci));

        List<ExternalObservationResponse> dtos = externalContextService.getContextByIncidentId(100L);

        assertNotNull(dtos);
        assertEquals(1, dtos.size());
        assertEquals("TEMPERATURE", dtos.get(0).getMetricType());
        assertEquals(21.0, dtos.get(0).getValueNum());
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando no hay contexto para la incidencia o ID es nulo")
    void shouldReturnEmptyListWhenNoContextOrNullIncidentId() {
        assertTrue(externalContextService.getContextByIncidentId(null).isEmpty());

        when(contextoIncidenciaRepository.findByIncidentId(999L)).thenReturn(Collections.emptyList());
        assertTrue(externalContextService.getContextByIncidentId(999L).isEmpty());
    }
}
