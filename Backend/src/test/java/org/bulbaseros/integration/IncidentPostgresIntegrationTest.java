package org.bulbaseros.integration;

import org.bulbaseros.dto.CreateIncidentRequest;
import org.bulbaseros.dto.IncidentResponse;
import org.bulbaseros.dto.IncidentStatusResponse;
import org.bulbaseros.dto.StatusChangeResponse;
import org.bulbaseros.dto.UpdateIncidentStatusRequest;
import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.service.IncidentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test de integracion extremo a extremo del flujo de incidencias y auditoria
 * ejecutado directamente contra la base de datos PostgreSQL real (Docker Compose).
 * <p>
 * Anotado con {@link EnabledIfPostgresAvailable}: si el contenedor de PostgreSQL no esta encendido,
 * el test se omite (skipped) de forma limpia sin romper la construccion ni penalizar la validacion.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@SpringBootTest
@ActiveProfiles("postgres")
@TestPropertySource(locations = "classpath:application-postgres.properties")
@EnabledIfPostgresAvailable
@Transactional
class IncidentPostgresIntegrationTest {

    @Autowired
    private IncidentService incidentService;

    @Test
    @DisplayName("Debe persistir incidencia en PostgreSQL y registrar cambio de estado con auditoria real")
    void shouldCreateIncidentAndAuditStatusChangeInPostgres() {
        CreateIncidentRequest createReq = new CreateIncidentRequest(
                "Farola fundida en Calle Larios",
                "Fallo intermitente en luminaria LED",
                "ALUMBRADO",
                36.7213,
                -4.4214,
                5.0,
                "Calle Marqués de Larios, 12",
                "Centro",
                "Centro Histórico"
        );

        IncidentResponse created = incidentService.createIncident(createReq, "citizen@urbanpulse.es");
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals(EstadoIncidencia.REPORTED, created.getStatus());

        List<IncidentResponse> actives = incidentService.getActiveIncidents();
        assertFalse(actives.isEmpty());
        assertTrue(actives.stream().anyMatch(i -> i.getId().equals(created.getId())));

        UpdateIncidentStatusRequest updateReq = new UpdateIncidentStatusRequest(
                EstadoIncidencia.VALIDATED,
                "Verificado por inspector municipal en base de datos PostgreSQL"
        );
        IncidentStatusResponse statusResp = incidentService.updateIncidentStatus(
                created.getId(),
                updateReq,
                "operator@urbanpulse.es"
        );
        assertNotNull(statusResp);
        assertEquals(EstadoIncidencia.VALIDATED, statusResp.getStatus());

        List<StatusChangeResponse> history = incidentService.getIncidentHistory(created.getId());
        assertFalse(history.isEmpty());
        StatusChangeResponse auditEntry = history.get(history.size() - 1);
        assertEquals(created.getId(), auditEntry.getIncidentId());
        assertEquals("REPORTED", auditEntry.getFromStatus());
        assertEquals("VALIDATED", auditEntry.getToStatus());
        assertEquals("operator@urbanpulse.es", auditEntry.getChangedByUserEmail());
    }
}
