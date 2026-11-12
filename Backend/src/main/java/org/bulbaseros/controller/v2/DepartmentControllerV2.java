package org.bulbaseros.controller.v2;

import org.bulbaseros.dto.v2.DepartmentResponseV2;
import org.bulbaseros.dto.v2.UserSummaryResponseV2;
import org.bulbaseros.entity.Departamento;
import org.bulbaseros.entity.RolUsuario;
import org.bulbaseros.entity.Usuario;
import org.bulbaseros.mappers.DepartmentMapperV2;
import org.bulbaseros.repository.DepartamentoRepository;
import org.bulbaseros.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para la consulta de departamentos municipales en API v2.
 * Opera exclusivamente con identificadores publicos UUID.
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@RestController
@RequestMapping("/api/v2/departments")
public class DepartmentControllerV2 {

    private final DepartamentoRepository departamentoRepository;
    private final UserRepository userRepository;
    private final DepartmentMapperV2 departmentMapperV2;

    @Autowired
    public DepartmentControllerV2(DepartamentoRepository departamentoRepository,
                                  UserRepository userRepository,
                                  DepartmentMapperV2 departmentMapperV2) {
        this.departamentoRepository = departamentoRepository;
        this.userRepository = userRepository;
        this.departmentMapperV2 = departmentMapperV2;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DepartmentResponseV2>> getAllDepartments() {
        List<Departamento> departments = departamentoRepository.findAllByOrderByNameAsc();
        return ResponseEntity.ok(departmentMapperV2.toDtoList(departments));
    }

    @GetMapping("/{id}/technicians")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<List<UserSummaryResponseV2>> getTechniciansByDepartment(@PathVariable("id") UUID id) {
        Departamento dept = departamentoRepository.findByPublicId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Departamento no encontrado con publicId: " + id));

        List<Usuario> technicians = userRepository.findByRoleAndDepartmentId(RolUsuario.TECHNICIAN, dept.getId());
        return ResponseEntity.ok(departmentMapperV2.toUserSummaryDtoList(technicians));
    }
}
