package org.bulbaseros.controller.v2;

import org.bulbaseros.dto.v2.DepartmentResponseV2;
import org.bulbaseros.entity.Departamento;
import org.bulbaseros.mappers.DepartmentMapperV2;
import org.bulbaseros.repository.DepartamentoRepository;
import org.bulbaseros.repository.UserRepository;
import org.bulbaseros.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DepartmentControllerV2Test {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DepartamentoRepository departamentoRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private DepartmentMapperV2 departmentMapperV2;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @WithMockUser(username = "operario@malaga.es", roles = "OPERATOR")
    @DisplayName("GET /api/v2/departments debe retornar lista con id UUID")
    void getAllDepartments_shouldReturnDepartmentsWithUuid() throws Exception {
        UUID publicId = UUID.randomUUID();
        Departamento dept = new Departamento(1L, "Alumbrado Publico", "Gestion de luminarias y redes electricas");
        dept.setPublicId(publicId);

        DepartmentResponseV2 deptDto = new DepartmentResponseV2(
                publicId.toString(),
                "Alumbrado Publico",
                "Gestion de luminarias y redes electricas"
        );

        when(departamentoRepository.findAllByOrderByNameAsc()).thenReturn(List.of(dept));
        when(departmentMapperV2.toDtoList(List.of(dept))).thenReturn(List.of(deptDto));

        mockMvc.perform(get("/api/v2/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(publicId.toString()))
                .andExpect(jsonPath("$[0].name").value("Alumbrado Publico"));
    }
}
