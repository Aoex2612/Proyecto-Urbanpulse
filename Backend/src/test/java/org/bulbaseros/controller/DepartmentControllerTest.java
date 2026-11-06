package org.bulbaseros.controller;

import org.bulbaseros.dto.DepartmentResponse;
import org.bulbaseros.dto.UserSummaryResponse;
import org.bulbaseros.entity.Departamento;
import org.bulbaseros.entity.RolUsuario;
import org.bulbaseros.entity.Usuario;
import org.bulbaseros.mappers.DepartmentMapper;
import org.bulbaseros.repository.DepartamentoRepository;
import org.bulbaseros.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración y seguridad RBAC con MockMvc para {@link DepartmentController}.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DepartamentoRepository departamentoRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private DepartmentMapper departmentMapper;

    @Test
    @DisplayName("GET /api/departments sin autenticación debe retornar 401 Unauthorized")
    void shouldReturn401WhenUnauthenticatedOnGetDepartments() throws Exception {
        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "citizen@urbanpulse.es", roles = "CITIZEN")
    @DisplayName("GET /api/departments autenticado con rol CITIZEN debe retornar 200 OK con el listado")
    void shouldReturn200WhenAuthenticatedOnGetDepartments() throws Exception {
        Departamento dept = new Departamento("Vía Pública", "Mantenimiento general");
        dept.setId(1L);

        DepartmentResponse deptDto = DepartmentResponse.builder()
                .id(1L)
                .name("Vía Pública")
                .description("Mantenimiento general")
                .build();

        when(departamentoRepository.findAllByOrderByNameAsc()).thenReturn(List.of(dept));
        when(departmentMapper.toDtoList(anyList())).thenReturn(List.of(deptDto));

        mockMvc.perform(get("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Vía Pública"))
                .andExpect(jsonPath("$[0].description").value("Mantenimiento general"));
    }

    @Test
    @DisplayName("GET /api/departments/{id}/technicians sin autenticación debe retornar 401 Unauthorized")
    void shouldReturn401WhenUnauthenticatedOnGetTechnicians() throws Exception {
        mockMvc.perform(get("/api/departments/1/technicians"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "citizen@urbanpulse.es", roles = "CITIZEN")
    @DisplayName("GET /api/departments/{id}/technicians con rol CITIZEN debe retornar 403 Forbidden")
    void shouldReturn403WhenCitizenRoleOnGetTechnicians() throws Exception {
        mockMvc.perform(get("/api/departments/1/technicians"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "tech@urbanpulse.es", roles = "TECHNICIAN")
    @DisplayName("GET /api/departments/{id}/technicians con rol TECHNICIAN debe retornar 403 Forbidden")
    void shouldReturn403WhenTechnicianRoleOnGetTechnicians() throws Exception {
        mockMvc.perform(get("/api/departments/1/technicians"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "operator@urbanpulse.es", roles = "OPERATOR")
    @DisplayName("GET /api/departments/{id}/technicians con rol OPERATOR y departamento inexistente debe retornar 404 Not Found")
    void shouldReturn404WhenDepartmentNotFound() throws Exception {
        when(departamentoRepository.existsById(999L)).thenReturn(false);

        mockMvc.perform(get("/api/departments/999/technicians"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(username = "operator@urbanpulse.es", roles = "OPERATOR")
    @DisplayName("GET /api/departments/{id}/technicians con rol OPERATOR debe retornar 200 OK con la lista de técnicos")
    void shouldReturn200WhenOperatorRoleOnGetTechnicians() throws Exception {
        when(departamentoRepository.existsById(1L)).thenReturn(true);

        Usuario techUser = new Usuario("Carlos Tecnico", "carlos@urbanpulse.es", "hash", RolUsuario.TECHNICIAN);
        techUser.setId(10L);

        UserSummaryResponse summary = UserSummaryResponse.builder()
                .id(10L)
                .fullName("Carlos Tecnico")
                .email("carlos@urbanpulse.es")
                .role(RolUsuario.TECHNICIAN)
                .departmentId(1L)
                .build();

        when(userRepository.findByRoleAndDepartmentId(RolUsuario.TECHNICIAN, 1L))
                .thenReturn(List.of(techUser));
        when(departmentMapper.toUserSummaryDtoList(anyList()))
                .thenReturn(List.of(summary));

        mockMvc.perform(get("/api/departments/1/technicians"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].fullName").value("Carlos Tecnico"))
                .andExpect(jsonPath("$[0].email").value("carlos@urbanpulse.es"))
                .andExpect(jsonPath("$[0].role").value("TECHNICIAN"))
                .andExpect(jsonPath("$[0].departmentId").value(1));
    }

    @Test
    @WithMockUser(username = "admin@urbanpulse.es", roles = "ADMIN")
    @DisplayName("GET /api/departments/{id}/technicians con rol ADMIN debe retornar 200 OK")
    void shouldReturn200WhenAdminRoleOnGetTechnicians() throws Exception {
        when(departamentoRepository.existsById(1L)).thenReturn(true);
        when(userRepository.findByRoleAndDepartmentId(RolUsuario.TECHNICIAN, 1L))
                .thenReturn(Collections.emptyList());
        when(departmentMapper.toUserSummaryDtoList(anyList()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/departments/1/technicians"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
