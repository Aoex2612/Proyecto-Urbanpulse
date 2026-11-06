package org.bulbaseros.mappers;

import org.bulbaseros.dto.DepartmentResponse;
import org.bulbaseros.dto.UserSummaryResponse;
import org.bulbaseros.entity.Departamento;
import org.bulbaseros.entity.Usuario;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapeador desacoplado de componentes Spring para la conversión entre entidades {@link Departamento}
 * y DTOs {@link DepartmentResponse}, así como para la proyección de resúmenes de usuarios técnicos {@link UserSummaryResponse}.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Component
public class DepartmentMapper extends BaseMapper<Departamento, DepartmentResponse> {

    /**
     * Transforma una entidad {@link Departamento} a su correspondiente DTO {@link DepartmentResponse}.
     *
     * @param entity Entidad de departamento municipal de origen, o {@code null}.
     * @return DTO {@link DepartmentResponse} mapeado, o {@code null} si la entidad es nula.
     */
    @Override
    public DepartmentResponse toDto(Departamento entity) {
        if (entity == null) {
            return null;
        }

        return DepartmentResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .build();
    }

    /**
     * Construye una nueva entidad {@link Departamento} a partir de su DTO representativo.
     *
     * @param dto DTO del departamento con datos de nombre y descripción, o {@code null}.
     * @return Nueva entidad {@link Departamento}, o {@code null} si el DTO es nulo.
     */
    public Departamento toEntity(DepartmentResponse dto) {
        if (dto == null) {
            return null;
        }

        Departamento departamento = new Departamento(dto.getName(), dto.getDescription());
        departamento.setId(dto.getId());
        return departamento;
    }

    /**
     * Transforma una entidad {@link Usuario} a un DTO de resumen {@link UserSummaryResponse}.
     *
     * @param user Entidad de usuario de origen, o {@code null}.
     * @return DTO {@link UserSummaryResponse} mapeado, o {@code null} si el usuario es nulo.
     */
    public UserSummaryResponse toUserSummaryDto(Usuario user) {
        if (user == null) {
            return null;
        }

        Long departmentId = (user.getDepartment() != null) ? user.getDepartment().getId() : null;

        return UserSummaryResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .departmentId(departmentId)
                .build();
    }

    /**
     * Transforma una lista de entidades {@link Usuario} a una lista de DTOs {@link UserSummaryResponse}.
     *
     * @param users Lista de entidades de usuarios, o {@code null}.
     * @return Lista inmutable o vacía de DTOs mapeados.
     */
    public List<UserSummaryResponse> toUserSummaryDtoList(List<Usuario> users) {
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        return users.stream()
                .map(this::toUserSummaryDto)
                .collect(Collectors.toList());
    }
}
