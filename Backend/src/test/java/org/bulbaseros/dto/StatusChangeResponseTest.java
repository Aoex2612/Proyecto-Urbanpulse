package org.bulbaseros.dto;

import org.bulbaseros.entity.CambiosEstado;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para el DTO {@link StatusChangeResponse}.
 * <p>
 * Verifica la inicializacion mediante constructores, mutacion con setters y mapeo desde la entidad {@link CambiosEstado}.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class StatusChangeResponseTest {

    @Test
    @DisplayName("Deberia inicializar DTO correctamente mediante constructor completo y getters")
    void shouldInitializeWithFullConstructorAndGetters() {
        OffsetDateTime now = OffsetDateTime.now();
        StatusChangeResponse response = new StatusChangeResponse(
                1L,
                10L,
                "REPORTED",
                "VALIDATED",
                2L,
                "Operador Triaje",
                "operador@urbanpulse.es",
                "Verificado en calle",
                now
        );

        assertEquals(1L, response.getId());
        assertEquals(10L, response.getIncidentId());
        assertEquals("REPORTED", response.getFromStatus());
        assertEquals("VALIDATED", response.getToStatus());
        assertEquals(2L, response.getChangedByUserId());
        assertEquals("Operador Triaje", response.getChangedByUserName());
        assertEquals("operador@urbanpulse.es", response.getChangedByUserEmail());
        assertEquals("Verificado en calle", response.getReason());
        assertEquals(now, response.getCreatedAt());
    }

    @Test
    @DisplayName("Deberia permitir modificar propiedades mediante setters")
    void shouldAllowModifyingPropertiesViaSetters() {
        StatusChangeResponse response = new StatusChangeResponse();
        OffsetDateTime now = OffsetDateTime.now();

        response.setId(5L);
        response.setIncidentId(15L);
        response.setFromStatus("VALIDATED");
        response.setToStatus("RESOLVED");
        response.setChangedByUserId(3L);
        response.setChangedByUserName("Admin General");
        response.setChangedByUserEmail("admin@urbanpulse.es");
        response.setReason("Reparacion concluida satisfactoriamente");
        response.setCreatedAt(now);

        assertEquals(5L, response.getId());
        assertEquals(15L, response.getIncidentId());
        assertEquals("VALIDATED", response.getFromStatus());
        assertEquals("RESOLVED", response.getToStatus());
        assertEquals(3L, response.getChangedByUserId());
        assertEquals("Admin General", response.getChangedByUserName());
        assertEquals("admin@urbanpulse.es", response.getChangedByUserEmail());
        assertEquals("Reparacion concluida satisfactoriamente", response.getReason());
        assertEquals(now, response.getCreatedAt());
    }

    @Test
    @DisplayName("Deberia transformar correctamente desde entidad CambiosEstado mediante fromEntity")
    void shouldTransformFromEntityCorrectly() {
        Usuario user = new Usuario("operador1@urbanpulse.es", "hash123", "Juan Perez");
        user.setId(7L);

        Incidencia incidencia = new Incidencia();
        incidencia.setId(42L);

        CambiosEstado cambio = new CambiosEstado(incidencia, "REPORTED", "IN_PROGRESS", user, "En curso por brigada");
        cambio.setId(101L);
        OffsetDateTime created = OffsetDateTime.now().minusMinutes(30);
        cambio.setCreatedAt(created);

        StatusChangeResponse response = StatusChangeResponse.fromEntity(cambio);

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals(42L, response.getIncidentId());
        assertEquals("REPORTED", response.getFromStatus());
        assertEquals("IN_PROGRESS", response.getToStatus());
        assertEquals(7L, response.getChangedByUserId());
        assertEquals("Juan Perez", response.getChangedByUserName());
        assertEquals("operador1@urbanpulse.es", response.getChangedByUserEmail());
        assertEquals("En curso por brigada", response.getReason());
        assertEquals(created, response.getCreatedAt());
    }

    @Test
    @DisplayName("Deberia retornar null cuando fromEntity recibe null")
    void shouldReturnNullWhenFromEntityReceivesNull() {
        assertNull(StatusChangeResponse.fromEntity(null));
    }
}
