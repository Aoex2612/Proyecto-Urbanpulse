package org.bulbaseros.dto;

import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.PrioridadIncidencia;
import org.bulbaseros.entity.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para el DTO {@link IncidentResponse}.
 * <p>
 * Verifica la correcta inicializacion de constructores, asignacion de propiedades y transformacion
 * desde la entidad de dominio {@link Incidencia} mediante el metodo {@code fromEntity}.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class IncidentResponseTest {

    @Test
    @DisplayName("Deberia inicializar DTO correctamente mediante constructor completo y getters")
    void shouldInitializeWithFullConstructorAndGetters() {
        OffsetDateTime now = OffsetDateTime.now();
        IncidentResponse response = new IncidentResponse(
                1L,
                "Rotura de tuberia",
                "Fuga de agua en acera",
                "Vias Publicas",
                36.7213,
                -4.4214,
                EstadoIncidencia.REPORTED,
                PrioridadIncidencia.HIGH,
                10L,
                "Carlos Perez",
                now,
                now
        );

        assertEquals(1L, response.getId());
        assertEquals("Rotura de tuberia", response.getTitle());
        assertEquals("Fuga de agua en acera", response.getDescription());
        assertEquals("Vias Publicas", response.getCategory());
        assertEquals(36.7213, response.getLatitude());
        assertEquals(-4.4214, response.getLongitude());
        assertEquals(EstadoIncidencia.REPORTED, response.getStatus());
        assertEquals(PrioridadIncidencia.HIGH, response.getPriority());
        assertEquals(10L, response.getCitizenId());
        assertEquals("Carlos Perez", response.getCitizenName());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());
    }

    @Test
    @DisplayName("Deberia permitir modificar propiedades mediante setters")
    void shouldAllowModifyingPropertiesViaSetters() {
        IncidentResponse response = new IncidentResponse();
        OffsetDateTime now = OffsetDateTime.now();

        response.setId(2L);
        response.setTitle("Farola rota");
        response.setDescription("Lampara caida");
        response.setCategory("Alumbrado");
        response.setLatitude(36.7196);
        response.setLongitude(-4.4200);
        response.setStatus(EstadoIncidencia.IN_PROGRESS);
        response.setPriority(PrioridadIncidencia.MEDIUM);
        response.setCitizenId(20L);
        response.setCitizenName("Marta Gomez");
        response.setCreatedAt(now);
        response.setUpdatedAt(now);

        assertEquals(2L, response.getId());
        assertEquals("Farola rota", response.getTitle());
        assertEquals("Lampara caida", response.getDescription());
        assertEquals("Alumbrado", response.getCategory());
        assertEquals(36.7196, response.getLatitude());
        assertEquals(-4.4200, response.getLongitude());
        assertEquals(EstadoIncidencia.IN_PROGRESS, response.getStatus());
        assertEquals(PrioridadIncidencia.MEDIUM, response.getPriority());
        assertEquals(20L, response.getCitizenId());
        assertEquals("Marta Gomez", response.getCitizenName());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());
    }

    @Test
    @DisplayName("Deberia transformar correctamente desde entidad Incidencia mediante fromEntity")
    void shouldTransformFromEntityCorrectly() {
        Usuario citizen = new Usuario("usuario@urbanpulse.es", "hash123", "Ana Morales");
        citizen.setId(5L);

        Incidencia incidencia = new Incidencia(
                "Banco roto",
                "Banco de madera partido",
                "Mobiliario Urbano",
                36.7200,
                -4.4220,
                citizen
        );
        incidencia.setId(100L);
        incidencia.setStatus(EstadoIncidencia.VALIDATED);
        incidencia.setPriority(PrioridadIncidencia.LOW);
        OffsetDateTime created = OffsetDateTime.now().minusHours(2);
        OffsetDateTime updated = OffsetDateTime.now();
        incidencia.setCreatedAt(created);
        incidencia.setUpdatedAt(updated);

        IncidentResponse response = IncidentResponse.fromEntity(incidencia);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Banco roto", response.getTitle());
        assertEquals("Banco de madera partido", response.getDescription());
        assertEquals("Mobiliario Urbano", response.getCategory());
        assertEquals(36.7200, response.getLatitude());
        assertEquals(-4.4220, response.getLongitude());
        assertEquals(EstadoIncidencia.VALIDATED, response.getStatus());
        assertEquals(PrioridadIncidencia.LOW, response.getPriority());
        assertEquals(5L, response.getCitizenId());
        assertEquals("Ana Morales", response.getCitizenName());
        assertEquals(created, response.getCreatedAt());
        assertEquals(updated, response.getUpdatedAt());
    }

    @Test
    @DisplayName("Deberia retornar null cuando fromEntity recibe null")
    void shouldReturnNullWhenFromEntityReceivesNull() {
        assertNull(IncidentResponse.fromEntity(null));
    }
}
