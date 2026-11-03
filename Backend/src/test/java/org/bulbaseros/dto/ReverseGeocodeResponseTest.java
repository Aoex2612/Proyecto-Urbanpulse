package org.bulbaseros.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pruebas unitarias para el DTO {@link ReverseGeocodeResponse}.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class ReverseGeocodeResponseTest {

    @Test
    @DisplayName("Deberia inicializar DTO correctamente con constructor y getters")
    void shouldInitializeWithConstructorAndGetters() {
        ReverseGeocodeResponse response = new ReverseGeocodeResponse(
                "Calle Marques de Larios, 5",
                "Centro",
                "Centro Historico"
        );

        assertNotNull(response);
        assertEquals("Calle Marques de Larios, 5", response.getAddress());
        assertEquals("Centro", response.getDistrict());
        assertEquals("Centro Historico", response.getNeighborhood());
    }

    @Test
    @DisplayName("Deberia permitir modificar propiedades via setters y soportar constructor vacio")
    void shouldAllowSettersAndEmptyConstructor() {
        ReverseGeocodeResponse response = new ReverseGeocodeResponse();
        assertNull(response.getAddress());
        assertNull(response.getDistrict());
        assertNull(response.getNeighborhood());

        response.setAddress("Paseo del Parque, 1");
        response.setDistrict("Centro");
        response.setNeighborhood("La Malagueta");

        assertEquals("Paseo del Parque, 1", response.getAddress());
        assertEquals("Centro", response.getDistrict());
        assertEquals("La Malagueta", response.getNeighborhood());
    }
}
