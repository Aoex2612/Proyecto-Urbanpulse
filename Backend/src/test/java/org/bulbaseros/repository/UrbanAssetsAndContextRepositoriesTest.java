package org.bulbaseros.repository;

import org.bulbaseros.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración y persistencia para los repositorios de la Feature 007:
 * {@link ActivoUrbanoRepository}, {@link AsociacionIncidenciaActivoRepository},
 * {@link ObservacionesExternasRepository} y {@link ContextoIncidenciaRepository}.
 * <p>
 * Verifica la creación, recuperación, borrado y búsquedas espaciales en la base de datos de test (H2).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@DataJpaTest
class UrbanAssetsAndContextRepositoriesTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ActivoUrbanoRepository activoUrbanoRepository;

    @Autowired
    private AsociacionIncidenciaActivoRepository asociacionIncidenciaActivoRepository;

    @Autowired
    private ObservacionesExternasRepository observacionesExternasRepository;

    @Autowired
    private ContextoIncidenciaRepository contextoIncidenciaRepository;

    private Usuario citizen;
    private Incidencia incident;
    private ActivoUrbano semaforo;
    private ActivoUrbano fuente;
    private ActivoUrbano contenedorLejano;

    @BeforeEach
    void setUp() {
        citizen = new Usuario("ciudadano.test@urbanpulse.es", "$2a$10$hashedPass", "Ciudadano Test");
        citizen = entityManager.persistAndFlush(citizen);

        incident = new Incidencia(
                "Avería semáforo Calle Larios",
                "Semáforo parpadea continuamente",
                "Vías Públicas",
                36.7212,
                -4.4214,
                citizen
        );
        incident = entityManager.persistAndFlush(incident);

        // Semáforo en Calle Larios (muy cerca de la incidencia)
        semaforo = new ActivoUrbano();
        semaforo.setAssetCode("SEM-001");
        semaforo.setType("TRAFFIC_LIGHT");
        semaforo.setName("Semáforo Larios / Plaza Constitución");
        semaforo.setLatitude(36.72125);
        semaforo.setLongitude(-4.42142);
        semaforo = entityManager.persistAndFlush(semaforo);

        // Fuente en Plaza de las Flores (próxima)
        fuente = new ActivoUrbano();
        fuente.setAssetCode("FNT-002");
        fuente.setType("DRINKING_FOUNTAIN");
        fuente.setName("Fuente Plaza de las Flores");
        fuente.setLatitude(36.72110);
        fuente.setLongitude(-4.42200);
        fuente = entityManager.persistAndFlush(fuente);

        // Contenedor en Teatinos (lejos, fuera del bounding box cercano)
        contenedorLejano = new ActivoUrbano();
        contenedorLejano.setAssetCode("CNT-999");
        contenedorLejano.setType("WASTE_CONTAINER");
        contenedorLejano.setName("Contenedor RSU Teatinos");
        contenedorLejano.setLatitude(36.71600);
        contenedorLejano.setLongitude(-4.47500);
        contenedorLejano = entityManager.persistAndFlush(contenedorLejano);
    }

    @Test
    @DisplayName("ActivoUrbanoRepository: Debería encontrar activo por assetCode")
    void shouldFindActivoUrbanoByAssetCode() {
        Optional<ActivoUrbano> found = activoUrbanoRepository.findByAssetCode("SEM-001");
        assertTrue(found.isPresent());
        assertEquals("Semáforo Larios / Plaza Constitución", found.get().getName());
        assertEquals("TRAFFIC_LIGHT", found.get().getType());

        Optional<ActivoUrbano> notFound = activoUrbanoRepository.findByAssetCode("INEXISTENTE");
        assertTrue(notFound.isEmpty());
    }

    @Test
    @DisplayName("ActivoUrbanoRepository: Debería filtrar por bounding box (latitud y longitud between)")
    void shouldFindActivosInBoundingBox() {
        // Bounding box acotado al centro histórico de Málaga
        Double minLat = 36.7200;
        Double maxLat = 36.7230;
        Double minLon = -4.4230;
        Double maxLon = -4.4200;

        List<ActivoUrbano> activos = activoUrbanoRepository.findByLatitudeBetweenAndLongitudeBetween(minLat, maxLat, minLon, maxLon);

        assertEquals(2, activos.size());
        assertTrue(activos.stream().anyMatch(a -> a.getAssetCode().equals("SEM-001")));
        assertTrue(activos.stream().anyMatch(a -> a.getAssetCode().equals("FNT-002")));
        assertFalse(activos.stream().anyMatch(a -> a.getAssetCode().equals("CNT-999")));
    }

    @Test
    @DisplayName("AsociacionIncidenciaActivoRepository: Debería persistir, buscar por incidentId, por composite key y borrar")
    void shouldManageAsociacionIncidenciaActivo() {
        AsociacionIncidenciaActivoId id = new AsociacionIncidenciaActivoId();
        id.setIncidentId(incident.getId());
        id.setAssetId(semaforo.getId());

        AsociacionIncidenciaActivo asociacion = new AsociacionIncidenciaActivo();
        asociacion.setId(id);
        asociacion.setIncident(incident);
        asociacion.setAsset(semaforo);
        asociacion.setDistanceMeters(6.5);
        asociacion.setConfidenceScore(0.87);
        asociacion.setIsConfirmed(false);

        asociacionIncidenciaActivoRepository.saveAndFlush(asociacion);

        // 1. Buscar por incidentId
        List<AsociacionIncidenciaActivo> list = asociacionIncidenciaActivoRepository.findByIncidentId(incident.getId());
        assertEquals(1, list.size());
        assertEquals(semaforo.getId(), list.get(0).getAsset().getId());
        assertEquals(6.5, list.get(0).getDistanceMeters());

        // 2. Buscar por incidentId y assetId
        Optional<AsociacionIncidenciaActivo> opt = asociacionIncidenciaActivoRepository.findByIncidentIdAndAssetId(
                incident.getId(), semaforo.getId());
        assertTrue(opt.isPresent());
        assertEquals(false, opt.get().getIsConfirmed());

        // 3. Confirmar asociación
        opt.get().setIsConfirmed(true);
        asociacionIncidenciaActivoRepository.saveAndFlush(opt.get());

        Optional<AsociacionIncidenciaActivo> confirmedOpt = asociacionIncidenciaActivoRepository.findByIncidentIdAndAssetId(
                incident.getId(), semaforo.getId());
        assertTrue(confirmedOpt.isPresent());
        assertTrue(confirmedOpt.get().getIsConfirmed());

        // 4. Borrar asociación
        asociacionIncidenciaActivoRepository.deleteByIncidentIdAndAssetId(incident.getId(), semaforo.getId());
        entityManager.flush();
        entityManager.clear();

        Optional<AsociacionIncidenciaActivo> deletedOpt = asociacionIncidenciaActivoRepository.findByIncidentIdAndAssetId(
                incident.getId(), semaforo.getId());
        assertTrue(deletedOpt.isEmpty());
    }

    @Test
    @DisplayName("ObservacionesExternas y ContextoIncidencia: Debería persistir observaciones y consultar contexto por incidentId")
    void shouldManageObservacionesExternasAndContextoIncidencia() {
        // 1. Persistir observaciones externas (meteorológica y tráfico)
        ObservacionesExternas meteoObs = new ObservacionesExternas();
        meteoObs.setSource("AEMET");
        meteoObs.setMetricType("TEMPERATURE");
        meteoObs.setValueNum(23.5);
        meteoObs.setValueText("23.5 C");
        meteoObs.setObservedAt(OffsetDateTime.now());

        meteoObs = observacionesExternasRepository.saveAndFlush(meteoObs);
        assertNotNull(meteoObs.getId());

        ObservacionesExternas traficoObs = new ObservacionesExternas();
        traficoObs.setSource("DGT");
        traficoObs.setMetricType("TRAFFIC_LEVEL");
        traficoObs.setValueNum(2.0);
        traficoObs.setValueText("MODERATE");
        traficoObs.setObservedAt(OffsetDateTime.now());

        traficoObs = observacionesExternasRepository.saveAndFlush(traficoObs);
        assertNotNull(traficoObs.getId());

        // 2. Vincular contexto a la incidencia
        ContextoIncidenciaId id1 = new ContextoIncidenciaId();
        id1.setIncidentId(incident.getId());
        id1.setObservationId(meteoObs.getId());

        ContextoIncidencia contexto1 = new ContextoIncidencia();
        contexto1.setId(id1);
        contexto1.setIncident(incident);
        contexto1.setObservation(meteoObs);
        contextoIncidenciaRepository.saveAndFlush(contexto1);

        ContextoIncidenciaId id2 = new ContextoIncidenciaId();
        id2.setIncidentId(incident.getId());
        id2.setObservationId(traficoObs.getId());

        ContextoIncidencia contexto2 = new ContextoIncidencia();
        contexto2.setId(id2);
        contexto2.setIncident(incident);
        contexto2.setObservation(traficoObs);
        contextoIncidenciaRepository.saveAndFlush(contexto2);

        // 3. Consultar por incidentId
        List<ContextoIncidencia> contextos = contextoIncidenciaRepository.findByIncidentId(incident.getId());
        assertEquals(2, contextos.size());
        assertTrue(contextos.stream().anyMatch(c -> c.getObservation().getMetricType().equals("TEMPERATURE")));
        assertTrue(contextos.stream().anyMatch(c -> c.getObservation().getMetricType().equals("TRAFFIC_LEVEL")));
    }
}
