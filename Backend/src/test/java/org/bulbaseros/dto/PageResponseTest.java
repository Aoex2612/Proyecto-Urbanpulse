package org.bulbaseros.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para el DTO generico {@link PageResponse}.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class PageResponseTest {

    @Test
    @DisplayName("Deberia inicializar DTO correctamente mediante constructor completo")
    void shouldInitializeWithFullConstructor() {
        List<String> items = List.of("item1", "item2");
        PageResponse<String> response = new PageResponse<>(items, 25L, 3, 0, 10);

        assertNotNull(response);
        assertEquals(items, response.getContent());
        assertEquals(25L, response.getTotalElements());
        assertEquals(3, response.getTotalPages());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
    }

    @Test
    @DisplayName("Deberia crear PageResponse a partir de Page de Spring Data")
    void shouldCreateFromSpringPage() {
        List<String> items = List.of("A", "B", "C");
        Page<String> springPage = new PageImpl<>(items, PageRequest.of(1, 3), 10);

        PageResponse<String> response = PageResponse.from(springPage);

        assertNotNull(response);
        assertEquals(items, response.getContent());
        assertEquals(10L, response.getTotalElements());
        assertEquals(4, response.getTotalPages());
        assertEquals(1, response.getPage());
        assertEquals(3, response.getSize());
    }

    @Test
    @DisplayName("Deberia manejar Page nulo de forma segura devolviendo lista vacia")
    void shouldHandleNullPageSafely() {
        PageResponse<String> response = PageResponse.from(null);

        assertNotNull(response);
        assertTrue(response.getContent().isEmpty());
        assertEquals(0L, response.getTotalElements());
        assertEquals(0, response.getTotalPages());
        assertEquals(0, response.getPage());
        assertEquals(0, response.getSize());
    }
}
