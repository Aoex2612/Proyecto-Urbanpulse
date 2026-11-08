package org.bulbaseros.service.resilience;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Componente de resiliencia concurrente y ligero que implementa el patrón Circuit Breaker
 * para proteger la integración con servicios externos de contexto urbano (clima y tráfico).
 * <p>
 * Implementa los requisitos RF-12, RF-13 y RF-14 definidos en la especificación y en el ADR-09:
 * <ul>
 *     <li><b>Ventana deslizante:</b> 10 peticiones (configurable o valor predeterminado).</li>
 *     <li><b>Umbral de fallo:</b> 50% de fallos en la ventana (5 o más fallos en 10 peticiones).</li>
 *     <li><b>Tiempo de enfriamiento (cooldown):</b> 30 segundos (configurable o valor predeterminado).</li>
 *     <li><b>Estados:</b> {@link CircuitState#CLOSED}, {@link CircuitState#OPEN} y {@link CircuitState#HALF_OPEN}.</li>
 * </ul>
 * Toda la gestión de estado y transiciones es completamente segura para hilos concurrentes (Thread-safe)
 * mediante el uso de sincronización con {@link ReentrantLock}.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Component
@Slf4j
public class CircuitBreaker {

    /**
     * Tamaño por defecto de la ventana móvil de evaluación de peticiones.
     */
    public static final int DEFAULT_WINDOW_SIZE = 10;

    /**
     * Umbral de tasa de fallos que dispara la apertura del circuito (50%).
     */
    public static final double DEFAULT_FAILURE_RATE_THRESHOLD = 0.50;

    /**
     * Tiempo predeterminado de enfriamiento en estado OPEN antes de pasar a HALF_OPEN (30.000 ms = 30s).
     */
    public static final long DEFAULT_COOLDOWN_MILLIS = 30_000L;

    private final int windowSize;
    private final double failureRateThreshold;
    private final long cooldownMillis;

    private final ReentrantLock lock = new ReentrantLock();
    private final Deque<Boolean> callHistory = new ArrayDeque<>();

    private CircuitState state = CircuitState.CLOSED;
    private long lastStateChangedTimestamp = System.currentTimeMillis();

    /**
     * Constructor predeterminado con los valores de configuración de la especificación
     * (ventana de 10 peticiones, umbral de fallo del 50% y cooldown de 30 segundos).
     */
    public CircuitBreaker() {
        this(DEFAULT_WINDOW_SIZE, DEFAULT_FAILURE_RATE_THRESHOLD, DEFAULT_COOLDOWN_MILLIS);
    }

    /**
     * Constructor parametrizado para personalizar las características del Circuit Breaker.
     *
     * @param windowSize Tamaño de la ventana deslizante de peticiones (debe ser mayor a 0).
     * @param failureRateThreshold Tasa de fallos requerida para abrir el circuito (ej. 0.5 para 50%).
     * @param cooldownMillis Tiempo de enfriamiento en milisegundos tras el cual se transiciona a HALF_OPEN.
     */
    public CircuitBreaker(int windowSize, double failureRateThreshold, long cooldownMillis) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("El tamaño de ventana debe ser mayor a 0");
        }
        if (failureRateThreshold <= 0.0 || failureRateThreshold > 1.0) {
            throw new IllegalArgumentException("El umbral de fallos debe estar en el rango (0.0, 1.0]");
        }
        if (cooldownMillis < 0) {
            throw new IllegalArgumentException("El cooldown no puede ser negativo");
        }
        this.windowSize = windowSize;
        this.failureRateThreshold = failureRateThreshold;
        this.cooldownMillis = cooldownMillis;
    }

    /**
     * Evalúa si se permite ejecutar una llamada a la API remota o si debe rechazarse de inmediato.
     * <p>
     * - Si el circuito está {@link CircuitState#CLOSED}, permite la ejecución.
     * - Si el circuito está {@link CircuitState#OPEN}, evalúa si ya transcurrió el tiempo de cooldown;
     *   si transcurrió, transiciona automáticamente a {@link CircuitState#HALF_OPEN} y permite la petición de prueba;
     *   de lo contrario, rechaza la llamada.
     * - Si el circuito está {@link CircuitState#HALF_OPEN}, permite la ejecución como intento de prueba.
     * </p>
     *
     * @return {@code true} si la llamada está permitida; {@code false} si el circuito está abierto y debe abortarse.
     */
    public boolean allowExecution() {
        lock.lock();
        try {
            long now = System.currentTimeMillis();
            if (state == CircuitState.OPEN) {
                if (now - lastStateChangedTimestamp >= cooldownMillis) {
                    log.info("CircuitBreaker: Cooldown de {} ms expirado. Transicionando de OPEN a HALF_OPEN.", cooldownMillis);
                    state = CircuitState.HALF_OPEN;
                    lastStateChangedTimestamp = now;
                    return true;
                }
                return false;
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Indica si el circuito se encuentra actualmente en estado abierto (OPEN).
     *
     * @return {@code true} si el estado actual es {@link CircuitState#OPEN} y aún no ha expirado el cooldown;
     *         {@code false} en caso contrario.
     */
    public boolean isOpen() {
        return !allowExecution();
    }

    /**
     * Registra una ejecución exitosa de una llamada al servicio externo.
     * <p>
     * - Si el circuito estaba en {@link CircuitState#HALF_OPEN}, la llamada de prueba exitosa
     *   provoca el restablecimiento inmediato a {@link CircuitState#CLOSED} y limpia la ventana de fallos.
     * - Si estaba en {@link CircuitState#CLOSED}, añade el resultado exitoso a la ventana móvil.
     * </p>
     */
    public void recordSuccess() {
        lock.lock();
        try {
            if (state == CircuitState.HALF_OPEN) {
                log.info("CircuitBreaker: Llamada de prueba exitosa en HALF_OPEN. Restableciendo circuito a CLOSED.");
                state = CircuitState.CLOSED;
                callHistory.clear();
                lastStateChangedTimestamp = System.currentTimeMillis();
            } else if (state == CircuitState.CLOSED) {
                addExecutionResult(true);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Registra una ejecución fallida de una llamada al servicio externo (timeout, error de conexión, HTTP 5xx, etc.).
     * <p>
     * - Si el circuito estaba en {@link CircuitState#HALF_OPEN}, la falla inmediata lo devuelve a {@link CircuitState#OPEN}
     *   reiniciando el temporizador de cooldown.
     * - Si estaba en {@link CircuitState#CLOSED}, añade el fallo a la ventana móvil y evalúa si el porcentaje
     *   de fallos acumulados supera el umbral configurado; en caso afirmativo, transiciona a {@link CircuitState#OPEN}.
     * </p>
     */
    public void recordFailure() {
        lock.lock();
        try {
            if (state == CircuitState.HALF_OPEN) {
                log.warn("CircuitBreaker: Fallo en llamada de prueba en HALF_OPEN. Reabriendo circuito a OPEN.");
                state = CircuitState.OPEN;
                lastStateChangedTimestamp = System.currentTimeMillis();
            } else if (state == CircuitState.CLOSED) {
                addExecutionResult(false);
                evaluateThreshold();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Obtiene el estado actual del Circuit Breaker.
     *
     * @return {@link CircuitState} actual (CLOSED, OPEN o HALF_OPEN).
     */
    public CircuitState getState() {
        lock.lock();
        try {
            // Verificar si debe transicionar a HALF_OPEN por expiración de cooldown
            if (state == CircuitState.OPEN && (System.currentTimeMillis() - lastStateChangedTimestamp >= cooldownMillis)) {
                state = CircuitState.HALF_OPEN;
                lastStateChangedTimestamp = System.currentTimeMillis();
            }
            return state;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Restablece el Circuit Breaker al estado inicial {@link CircuitState#CLOSED}, limpiando el historial de peticiones.
     */
    public void reset() {
        lock.lock();
        try {
            state = CircuitState.CLOSED;
            callHistory.clear();
            lastStateChangedTimestamp = System.currentTimeMillis();
            log.info("CircuitBreaker: Reseteado manualmente a estado CLOSED.");
        } finally {
            lock.unlock();
        }
    }

    /**
     * Añade un resultado (true = éxito, false = fallo) a la ventana móvil, manteniendo su tamaño máximo.
     *
     * @param success {@code true} si la llamada fue exitosa; {@code false} si fue fallida.
     */
    private void addExecutionResult(boolean success) {
        if (callHistory.size() >= windowSize) {
            callHistory.pollFirst();
        }
        callHistory.addLast(success);
    }

    /**
     * Evalúa si la tasa de fallos en la ventana móvil supera el umbral configurado.
     * Si la ventana alcanza su capacidad y el número de fallos es mayor o igual al umbral, transiciona a OPEN.
     */
    private void evaluateThreshold() {
        if (callHistory.isEmpty()) {
            return;
        }
        long failureCount = callHistory.stream().filter(success -> !success).count();
        double currentFailureRate = (double) failureCount / callHistory.size();

        // Si tenemos suficientes muestras y la tasa supera el umbral
        if (callHistory.size() >= windowSize && currentFailureRate >= failureRateThreshold) {
            log.warn("CircuitBreaker: Tasa de fallos ({}/{}) supera el umbral {}%. Abriendo circuito a OPEN por {} ms.",
                    failureCount, callHistory.size(), (failureRateThreshold * 100), cooldownMillis);
            state = CircuitState.OPEN;
            lastStateChangedTimestamp = System.currentTimeMillis();
            callHistory.clear();
        }
    }
}
