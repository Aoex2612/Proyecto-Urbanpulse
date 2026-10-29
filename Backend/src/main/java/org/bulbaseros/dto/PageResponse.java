package org.bulbaseros.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Objeto de transferencia de datos (DTO) generico para encapsular respuestas paginadas.
 * <p>
 * Estandariza la estructura de retorno para listados paginados de la API REST segun el contrato canonico
 * (content, totalElements, totalPages, page y size), abstrayendo la serializacion de objetos Page de Spring Data.
 * </p>
 *
 * @param <T> Tipo de los elementos contenidos en la coleccion paginada.
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    /**
     * Lista de elementos correspondientes a la pagina actual.
     */
    private List<T> content;

    /**
     * Numero total de elementos disponibles en todas las paginas.
     */
    private long totalElements;

    /**
     * Numero total de paginas calculadas segun el tamano solicitado.
     */
    private int totalPages;

    /**
     * Indice de la pagina actual (base 0).
     */
    private int page;

    /**
     * Cantidad maxima de elementos por pagina.
     */
    private int size;

    /**
     * Construye un {@link PageResponse} a partir de un {@link Page} de Spring Data.
     *
     * @param <T>  Tipo de elemento en la pagina.
     * @param page Pagina origen de Spring Data.
     * @return Instancia estructurada de {@link PageResponse}.
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        if (page == null) {
            return new PageResponse<>(List.of(), 0L, 0, 0, 0);
        }
        return new PageResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }
}
