package org.bulbaseros.service;

import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.RolUsuario;
import org.bulbaseros.entity.Usuario;
import org.bulbaseros.repository.IncidenciaRepository;
import org.bulbaseros.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link DuplicateDetectionService}.
 * <p>
 * Valida los criterios de proximidad espacial Haversine (&le; 60 m), ventana temporal (&le; 48 h),
 * coincidencia léxica / semántica normalizada (Jaccard &ge; 0.3) y la emisión exclusiva de alertas
 * de tipo DUPLICATE_SUGGESTION a operadores y técnicos.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class DuplicateDetectionServiceTest {

    @Mock
    private IncidenciaRepository incidenciaRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DuplicateDetectionService duplicateDetectionService;

    private Incidencia baseIncident;
    private Usuario operator;
    private Usuario technician;

    @BeforeEach
    void setUp() {
        Usuario citizen = new Usuario();
        citizen.setId(1L);
        citizen.setEmail("ciudadano@urbanpulse.es");
        citizen.setRole(RolUsuario.CITIZEN);

        operator = new Usuario();
        operator.setId(2L);
        operator.setEmail("operador@urbanpulse.es");
        operator.setRole(RolUsuario.OPERATOR);

        technician = new Usuario();
        technician.setId(3L);
        technician.setEmail("tecnico@urbanpulse.es");
        technician.setRole(RolUsuario.TECHNICIAN);

        baseIncident = new Incidencia();
        baseIncident.setId(10L);
        baseIncident.setTitle("Farola rota calle Larios");
        baseIncident.setDescription("La farola frente al portal 3 no enciende desde anoche");
        baseIncident.setCategory("Alumbrado");
        baseIncident.setLatitude(36.7213);
        baseIncident.setLongitude(-4.4214);
        baseIncident.setCitizen(citizen);
        baseIncident.setStatus(EstadoIncidencia.REPORTED);
        baseIncident.setCreatedAt(OffsetDateTime.now().minusHours(2));
    }

    @Test
    @DisplayName("haversineDistance: calcula distancia precisa menor de 60m para coordenadas contiguas")
    void shouldCalculateAccurateHaversineDistanceClose() {
        // Desplazamiento aproximado de ~20 metros en latitud (~0.00018 grados)
        double distance = duplicateDetectionService.haversineDistance(36.72130, -4.42140, 36.72145, -4.42140);
        assertTrue(distance < 60.0, "La distancia debe ser menor de 60 metros");
        assertTrue(distance > 5.0, "La distancia debe ser mayor de 5 metros");
    }

    @Test
    @DisplayName("haversineDistance: calcula distancia mayor a 60m para puntos alejados")
    void shouldCalculateAccurateHaversineDistanceFar() {
        // Desplazamiento de ~1 km
        double distance = duplicateDetectionService.haversineDistance(36.72130, -4.42140, 36.73000, -4.42140);
        assertTrue(distance > 60.0, "La distancia debe superar los 60 metros");
    }

    @Test
    @DisplayName("jaccardSimilarity: devuelve valor alto cuando comparten palabras clave")
    void shouldCalculateHighSimilarityForSharedWords() {
        double sim = duplicateDetectionService.jaccardSimilarity(
                "Farola rota calle Larios", "No enciende la luz frente al portal",
                "Farola rota en Larios", "Luz apagada en la farola del portal"
        );
        assertTrue(sim >= 0.3, "La similitud de Jaccard debe ser mayor o igual a 0.3");
    }

    @Test
    @DisplayName("jaccardSimilarity: devuelve valor bajo inferior al umbral cuando no comparten vocabulario clave")
    void shouldCalculateZeroSimilarityForDisjointText() {
        double sim = duplicateDetectionService.jaccardSimilarity(
                "Semáforo averiado", "Luces parpadeando intermitente",
                "Banco roto parque", "Madera astillada columpio infantil"
        );
        assertEquals(0.0, sim, 0.001);
    }

    @Test
    @DisplayName("findPotentialDuplicates: detecta candidato que cumple distancia, tiempo, categoría y semántica")
    void shouldDetectCandidateWhenAllCriteriaMet() {
        Incidencia candidate = new Incidencia();
        candidate.setId(20L);
        candidate.setTitle("Farola apagada calle Larios");
        candidate.setDescription("Frente al portal 3 no funciona la farola");
        candidate.setCategory("Alumbrado");
        candidate.setLatitude(36.72132); // ~2m de distancia
        candidate.setLongitude(-4.42141);
        candidate.setCreatedAt(OffsetDateTime.now().minusHours(3));

        when(incidenciaRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of(candidate));

        List<Incidencia> duplicates = duplicateDetectionService.findPotentialDuplicates(
                36.72130, -4.42140, "Alumbrado",
                "Farola rota calle Larios", "La farola frente al portal no enciende", 10L
        );

        assertEquals(1, duplicates.size());
        assertEquals(20L, duplicates.get(0).getId());
    }

    @Test
    @DisplayName("findPotentialDuplicates: descarta candidato con distancia > 60m")
    void shouldDiscardCandidateWhenDistanceExceeds60Meters() {
        Incidencia candidate = new Incidencia();
        candidate.setId(21L);
        candidate.setTitle("Farola rota calle Larios");
        candidate.setDescription("La farola frente al portal 3 no enciende");
        candidate.setCategory("Alumbrado");
        candidate.setLatitude(36.73000); // Lejos
        candidate.setLongitude(-4.42140);
        candidate.setCreatedAt(OffsetDateTime.now().minusHours(1));

        when(incidenciaRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of(candidate));

        List<Incidencia> duplicates = duplicateDetectionService.findPotentialDuplicates(
                36.72130, -4.42140, "Alumbrado",
                "Farola rota calle Larios", "Frente al portal 3 no enciende", 10L
        );

        assertTrue(duplicates.isEmpty());
    }

    @Test
    @DisplayName("findPotentialDuplicates: descarta candidato con categoría diferente")
    void shouldDiscardCandidateWhenCategoryDiffers() {
        Incidencia candidate = new Incidencia();
        candidate.setId(22L);
        candidate.setTitle("Farola rota calle Larios");
        candidate.setDescription("Frente al portal 3");
        candidate.setCategory("Limpieza"); // Categoría distinta
        candidate.setLatitude(36.72131);
        candidate.setLongitude(-4.42140);
        candidate.setCreatedAt(OffsetDateTime.now().minusHours(1));

        when(incidenciaRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of(candidate));

        List<Incidencia> duplicates = duplicateDetectionService.findPotentialDuplicates(
                36.72130, -4.42140, "Alumbrado",
                "Farola rota calle Larios", "Frente al portal 3", 10L
        );

        assertTrue(duplicates.isEmpty());
    }

    @Test
    @DisplayName("notifyPotentialDuplicates: envía alertas DUPLICATE_SUGGESTION exclusivamente a operadores y técnicos")
    void shouldNotifyOperatorsAndTechniciansWhenDuplicatesDetected() {
        when(userRepository.findByRole(RolUsuario.OPERATOR)).thenReturn(List.of(operator));
        when(userRepository.findByRole(RolUsuario.TECHNICIAN)).thenReturn(List.of(technician));

        duplicateDetectionService.notifyPotentialDuplicates(baseIncident, List.of(baseIncident));

        verify(notificationService).createNotification(
                eq(operator), eq(baseIncident), eq("DUPLICATE_SUGGESTION"),
                contains("Posible incidencia duplicada detectada"),
                contains("puede ser duplicada de 1 incidencia(s)")
        );

        verify(notificationService).createNotification(
                eq(technician), eq(baseIncident), eq("DUPLICATE_SUGGESTION"),
                contains("Posible incidencia duplicada detectada"),
                contains("puede ser duplicada de 1 incidencia(s)")
        );
    }

    @Test
    @DisplayName("notifyPotentialDuplicates: no envía notificaciones si la lista de candidatos está vacía")
    void shouldNotNotifyWhenCandidatesListIsEmpty() {
        duplicateDetectionService.notifyPotentialDuplicates(baseIncident, List.of());
        verifyNoInteractions(notificationService);
    }
}
