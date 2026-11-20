package org.bulbaseros.mappers;

import org.bulbaseros.dto.v2.DepartmentResponseV2;
import org.bulbaseros.dto.v2.UserSummaryResponseV2;
import org.bulbaseros.entity.Departamento;
import org.bulbaseros.entity.Usuario;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapeador de componentes Spring para transformar {@link Departamento} y {@link Usuario}
 * a DTOs V2 ({@link DepartmentResponseV2} y {@link UserSummaryResponseV2}).
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@Component
public class DepartmentMapperV2 {

    public DepartmentResponseV2 toDto(Departamento entity) {
        if (entity == null) {
            return null;
        }

        String id = entity.getPublicId() != null ? entity.getPublicId().toString() : null;
        return DepartmentResponseV2.builder()
                .id(id)
                .name(entity.getName())
                .description(entity.getDescription())
                .build();
    }

    public List<DepartmentResponseV2> toDtoList(List<Departamento> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    public UserSummaryResponseV2 toUserSummaryDto(Usuario user) {
        if (user == null) {
            return null;
        }

        String id = user.getPublicId() != null ? user.getPublicId().toString() : null;
        String departmentId = (user.getDepartment() != null && user.getDepartment().getPublicId() != null)
                ? user.getDepartment().getPublicId().toString() : null;

        return UserSummaryResponseV2.builder()
                .id(id)
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .departmentId(departmentId)
                .build();
    }

    public List<UserSummaryResponseV2> toUserSummaryDtoList(List<Usuario> users) {
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        return users.stream().map(this::toUserSummaryDto).collect(Collectors.toList());
    }
}
