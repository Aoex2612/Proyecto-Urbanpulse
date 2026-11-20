package org.bulbaseros.service.resilience;

/**
 * Enumeración que define los posibles estados operacionales del patrón Circuit Breaker.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
public enum CircuitState {
    /**
     * Circuito cerrado: operación normal, todas las peticiones a la API externa son permitidas.
     */
    CLOSED,

    /**
     * Circuito abierto: la tasa de fallos superó el umbral. Las peticiones son rechazadas de inmediato
     * para no sobrecargar el sistema remoto y activar el mecanismo de fallback instantáneo.
     */
    OPEN,

    /**
     * Circuito semi-abierto: tras el periodo de enfriamiento (cooldown), se permite una petición de prueba
     * para verificar si el servicio remoto se ha recuperado.
     */
    HALF_OPEN
}
