package org.bulbaseros.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos (DTO) que representa el resultado de la resolucion
 * territorial y geocodificacion inversa a partir de coordenadas geograficas en Malaga.
 * <p>
 * Proporciona la direccion postal normalizada, el distrito municipal y el barrio correspondientes
 * obtenidos desde los servicios de datos abiertos del Ayuntamiento de Malaga.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReverseGeocodeResponse {

    /**
     * Direccion postal normalizada del punto o calle mas cercana.
     */
    private String address;

    /**
     * Nombre del distrito municipal administrativo en el que se ubica la coordenada.
     */
    private String district;

    /**
     * Nombre del barrio o vecindario perteneciente al distrito en el que se ubica la coordenada.
     */
    private String neighborhood;
}
