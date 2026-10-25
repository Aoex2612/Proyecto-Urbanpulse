package org.bulbaseros.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepcion personalizada lanzada cuando una incidencia urbana solicitada no existe en el sistema.
 * <p>
 * Mapea directamente al codigo de estado HTTP 404 Not Found cuando es procesada
 * por los controladores REST o el gestor global de excepciones.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class IncidentNotFoundException extends RuntimeException {

    /**
     * Identificador unico de version para la serializacion de la excepcion.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Construye una nueva excepcion con un mensaje descriptivo especifico.
     *
     * @param message Mensaje explicativo del motivo del error.
     */
    public IncidentNotFoundException(String message) {
        super(message);
    }

    /**
     * Construye una nueva excepcion a partir del identificador numerico de la incidencia no hallada.
     *
     * @param id Identificador unico de la incidencia buscada.
     */
    public IncidentNotFoundException(Long id) {
        super("Incidencia no encontrada con id: " + id);
    }
}
