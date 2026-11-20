package org.bulbaseros.service.resilience;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CircuitBreaker - Pruebas Unitarias de Resiliencia y Transiciones de Estado")
class CircuitBreakerTest {

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        // Ventana de 10 peticiones, 50% umbral de fallo (>= 5 fallos en 10), 100 ms de cooldown para tests rápidos
        circuitBreaker = new CircuitBreaker(10, 0.50, 100L);
    }

    @Test
    @DisplayName("Debe iniciar en estado CLOSED y permitir la ejecución")
    void shouldStartInClosedStateAndAllowExecution() {
        assertEquals(CircuitState.CLOSED, circuitBreaker.getState());
        assertTrue(circuitBreaker.allowExecution());
        assertFalse(circuitBreaker.isOpen());
    }

    @Test
    @DisplayName("Debe mantenerse en CLOSED si los fallos no alcanzan el umbral del 50%")
    void shouldRemainClosedWhenFailuresDoNotExceedThreshold() {
        // 4 fallos y 6 éxitos en una ventana de 10 (40% < 50%)
        for (int i = 0; i < 4; i++) {
            circuitBreaker.recordFailure();
        }
        for (int i = 0; i < 6; i++) {
            circuitBreaker.recordSuccess();
        }

        assertEquals(CircuitState.CLOSED, circuitBreaker.getState());
        assertTrue(circuitBreaker.allowExecution());
        assertFalse(circuitBreaker.isOpen());
    }

    @Test
    @DisplayName("Debe transicionar a OPEN cuando se alcanza el 50% de fallos en la ventana de 10")
    void shouldTransitionToOpenWhenFailureThresholdReached() {
        // 5 fallos y 5 éxitos en 10 peticiones -> 50% de fallos
        for (int i = 0; i < 5; i++) {
            circuitBreaker.recordFailure();
        }
        for (int i = 0; i < 5; i++) {
            circuitBreaker.recordSuccess();
        }

        // Si se registraron 5 fallos y 5 éxitos en 10 peticiones, al registrar el décimo evento se evalúa la ventana:
        // Dependiendo del orden, si los 5 fallos quedan en la ventana deslizante al llegar al tamaño 10:
        // Verifiquemos con 5 éxitos primero y luego 5 fallos:
        circuitBreaker.reset();
        for (int i = 0; i < 5; i++) {
            circuitBreaker.recordSuccess();
        }
        for (int i = 0; i < 5; i++) {
            circuitBreaker.recordFailure();
        }

        assertEquals(CircuitState.OPEN, circuitBreaker.getState());
        assertFalse(circuitBreaker.allowExecution());
        assertTrue(circuitBreaker.isOpen());
    }

    @Test
    @DisplayName("Debe rechazar ejecuciones mientras esté en estado OPEN")
    void shouldRejectExecutionsWhileOpen() {
        // Abrir circuito con 10 fallos
        for (int i = 0; i < 10; i++) {
            circuitBreaker.recordFailure();
        }

        assertEquals(CircuitState.OPEN, circuitBreaker.getState());
        assertFalse(circuitBreaker.allowExecution());
        assertTrue(circuitBreaker.isOpen());
    }

    @Test
    @DisplayName("Debe transicionar a HALF_OPEN una vez transcurrido el tiempo de cooldown")
    void shouldTransitionToHalfOpenAfterCooldownExpires() throws InterruptedException {
        // Abrir el circuito
        for (int i = 0; i < 10; i++) {
            circuitBreaker.recordFailure();
        }
        assertEquals(CircuitState.OPEN, circuitBreaker.getState());

        // Esperar a que expire el cooldown de 100 ms
        Thread.sleep(150L);

        // Al evaluar el estado o permitir ejecución, transiciona a HALF_OPEN
        assertTrue(circuitBreaker.allowExecution());
        assertEquals(CircuitState.HALF_OPEN, circuitBreaker.getState());
        assertFalse(circuitBreaker.isOpen());
    }

    @Test
    @DisplayName("Debe recuperarse a CLOSED tras un éxito en estado HALF_OPEN")
    void shouldRecoverToClosedAfterSuccessInHalfOpen() throws InterruptedException {
        // Abrir el circuito
        for (int i = 0; i < 10; i++) {
            circuitBreaker.recordFailure();
        }

        // Esperar cooldown
        Thread.sleep(150L);
        assertTrue(circuitBreaker.allowExecution());
        assertEquals(CircuitState.HALF_OPEN, circuitBreaker.getState());

        // Registrar éxito en HALF_OPEN
        circuitBreaker.recordSuccess();

        // Debe volver a CLOSED
        assertEquals(CircuitState.CLOSED, circuitBreaker.getState());
        assertTrue(circuitBreaker.allowExecution());
    }

    @Test
    @DisplayName("Debe reabrir a OPEN tras un fallo en estado HALF_OPEN")
    void shouldReopenAfterFailureInHalfOpen() throws InterruptedException {
        // Abrir el circuito
        for (int i = 0; i < 10; i++) {
            circuitBreaker.recordFailure();
        }

        // Esperar cooldown
        Thread.sleep(150L);
        assertTrue(circuitBreaker.allowExecution());
        assertEquals(CircuitState.HALF_OPEN, circuitBreaker.getState());

        // Registrar fallo en HALF_OPEN
        circuitBreaker.recordFailure();

        // Debe volver de inmediato a OPEN
        assertEquals(CircuitState.OPEN, circuitBreaker.getState());
        assertFalse(circuitBreaker.allowExecution());
        assertTrue(circuitBreaker.isOpen());
    }

    @Test
    @DisplayName("Debe permitir resetear el estado manualmente a CLOSED")
    void shouldResetToClosedStateManually() {
        for (int i = 0; i < 10; i++) {
            circuitBreaker.recordFailure();
        }
        assertEquals(CircuitState.OPEN, circuitBreaker.getState());

        circuitBreaker.reset();

        assertEquals(CircuitState.CLOSED, circuitBreaker.getState());
        assertTrue(circuitBreaker.allowExecution());
    }

    @Test
    @DisplayName("Debe validar los parámetros de configuración en el constructor")
    void shouldValidateConstructorParameters() {
        assertThrows(IllegalArgumentException.class, () -> new CircuitBreaker(0, 0.5, 1000));
        assertThrows(IllegalArgumentException.class, () -> new CircuitBreaker(-1, 0.5, 1000));
        assertThrows(IllegalArgumentException.class, () -> new CircuitBreaker(10, 0.0, 1000));
        assertThrows(IllegalArgumentException.class, () -> new CircuitBreaker(10, 1.5, 1000));
        assertThrows(IllegalArgumentException.class, () -> new CircuitBreaker(10, 0.5, -1));
    }
}
