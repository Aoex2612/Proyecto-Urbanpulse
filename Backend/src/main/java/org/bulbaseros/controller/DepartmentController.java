package org.bulbaseros.controller;

import org.bulbaseros.dto.DepartmentResponse;
import org.bulbaseros.dto.UserSummaryResponse;
import org.bulbaseros.entity.Departamento;
import org.bulbaseros.entity.RolUsuario;
import org.bulbaseros.entity.Usuario;
import org.bulbaseros.mappers.DepartmentMapper;
import org.bulbaseros.repository.DepartamentoRepository;
import org.bulbaseros.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Controlador REST que gestiona la consulta de departamentos municipales y sus técnicos adscritos.
 * <p>
 * Proporciona endpoints protegidos mediante RBAC (Role-Based Access Control) para obtener
 * el catálogo oficial de áreas departamentales del ayuntamiento y el listado de técnicos
 * habilitados para la asignación y resolución operativa de incidencias.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@RestController
@RequestMapping({"/api/v1/departments", "/api/departments"})
public class DepartmentController {

    private final DepartamentoRepository departamentoRepository;
    private final UserRepository userRepository;
    private final DepartmentMapper departmentMapper;

    /**
     * Construye una nueva instancia de {@link DepartmentController} con sus dependencias requeridas.
     *
     * @param departamentoRepository Repositorio de consulta de departamentos municipales.
     * @param userRepository         Repositorio de usuarios del sistema.
     * @param departmentMapper       Mapeador de conversión a DTOs de departamento y usuario.
     */
    public DepartmentController(DepartamentoRepository departamentoRepository,
                                UserRepository userRepository,
                                DepartmentMapper departmentMapper) {
        this.departamentoRepository = departamentoRepository;
        this.userRepository = userRepository;
        this.departmentMapper = departmentMapper;
    }

    /**
     * Recupera el catálogo completo de departamentos municipales ordenados alfabéticamente por nombre.
     * <p>
     * Accesible por cualquier usuario autenticado con un rol válido en el sistema.
     * </p>
     *
     * @return {@link ResponseEntity} con código HTTP 200 OK y la lista de {@link DepartmentResponse}.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments() {
        List<Departamento> departments = departamentoRepository.findAllByOrderByNameAsc();
        List<DepartmentResponse> response = departmentMapper.toDtoList(departments);
        return ResponseEntity.ok(response);
    }

    /**
     * Recupera el listado de técnicos municipales pertenecientes a un departamento específico.
     * <p>
     * Filtra estrictamente los usuarios con rol {@link RolUsuario#TECHNICIAN} asignados al departamento indicado.
     * Requiere permisos de operador o administrador ({@code OPERATOR} o {@code ADMIN}).
     * </p>
     *
     * @param id Identificador único del departamento municipal.
     * @return {@link ResponseEntity} con código HTTP 200 OK y la lista de {@link UserSummaryResponse}.
     * @throws ResponseStatusException con código HTTP 404 NOT_FOUND si el departamento especificado no existe.
     */
    @GetMapping("/{id}/technicians")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<List<UserSummaryResponse>> getTechniciansByDepartment(@PathVariable("id") Long id) {
        if (!departamentoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Departamento no encontrado con id: " + id);
        }

        List<Usuario> technicians = userRepository.findByRoleAndDepartmentId(RolUsuario.TECHNICIAN, id);
        List<UserSummaryResponse> response = departmentMapper.toUserSummaryDtoList(technicians);
        return ResponseEntity.ok(response);
    }
}
