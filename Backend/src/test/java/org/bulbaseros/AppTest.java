package org.bulbaseros;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Pruebas unitarias básicas para verificar la carga inicial de componentes de la aplicación.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class AppTest {

    /**
     * Verifica que la instancia de la aplicación principal pueda ser instanciada correctamente.
     */
    @Test
    @DisplayName("Debería instanciar la clase principal App correctamente")
    void shouldInstantiateApp() {
        App app = new App();
        assertNotNull(app);
    }
}
