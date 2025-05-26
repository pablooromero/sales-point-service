package com.sales_point_service.sales_point_service.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sales_point_service.sales_point_service.config.SecurityConfig;
import com.sales_point_service.sales_point_service.dtos.CostDTO;
import com.sales_point_service.sales_point_service.dtos.CreateCostRequest;
import com.sales_point_service.sales_point_service.dtos.SalePointPathItem;
import com.sales_point_service.sales_point_service.dtos.ShortestPathDTO;
import com.sales_point_service.sales_point_service.exceptions.CostException;
import com.sales_point_service.sales_point_service.exceptions.ExceptionHandlers;
import com.sales_point_service.sales_point_service.services.CostService;
import com.sales_point_service.sales_point_service.utils.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CostController.class)
@Import({SecurityConfig.class, ExceptionHandlers.class})
public class CostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CostService costService;

    @Autowired
    private ObjectMapper objectMapper;

    private CostDTO costDTO1;
    private CostDTO costDTO2;
    private Set<CostDTO> costSet;

    @BeforeEach
    void setUp() {
        costDTO1 = new CostDTO(1L, "Point A", 2L, "Point B", 10.0);
        costDTO2 = new CostDTO(2L, "Point B", 3L, "Point C", 5.0);
        costSet = new HashSet<>();
        costSet.add(costDTO1);
        costSet.add(costDTO2);
    }

    @Test
    @DisplayName("GET /api/costs/admin - Debería devolver todos los costos si es ADMIN")
    void findAll_asAdmin_shouldReturnAllCosts() throws Exception {
        when(costService.getAllCosts()).thenReturn(ResponseEntity.ok(costSet));

        mockMvc.perform(get("/api/costs/admin")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(token -> token.claim("role", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.fromId == 1 && @.toId == 2)]", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/costs/admin - Debería devolver 403 si NO es ADMIN")
    void findAll_asNonAdmin_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/costs/admin")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(token -> token.claim("role", "USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/costs/admin - Debería crear costo si es ADMIN")
    void createCost_asAdmin_shouldCreateCost() throws Exception {
        CreateCostRequest request = new CreateCostRequest(1L, 2L, 15.0);
        when(costService.createCost(any(CreateCostRequest.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body("Costo creado"));

        mockMvc.perform(post("/api/costs/admin")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(token -> token.claim("role", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Costo creado"));
    }

    @Test
    @DisplayName("POST /api/costs/admin - Debería devolver 400 si el request es inválido (manejado por servicio)")
    void createCost_asAdmin_withInvalidRequest_shouldReturnBadRequest() throws Exception {
        CreateCostRequest invalidRequest = new CreateCostRequest(1L, 2L, -5.0);
        when(costService.createCost(any(CreateCostRequest.class)))
                .thenThrow(new CostException("El costo no puede ser negativo", HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/api/costs/admin")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(token -> token.claim("role", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("El costo no puede ser negativo")));
    }

    @Test
    @DisplayName("DELETE /api/costs/admin/{fromId}/{toId} - Debería eliminar costo si es ADMIN")
    void deleteCost_asAdmin_shouldDeleteCost() throws Exception {
        when(costService.deleteCost(1L, 2L))
                .thenReturn(ResponseEntity.ok(Constants.COST_DELETED));

        mockMvc.perform(delete("/api/costs/admin/1/2")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(token -> token.claim("role", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(content().string(Constants.COST_DELETED));
    }

    @Test
    @DisplayName("DELETE /api/costs/admin/{fromId}/{toId} - Debería devolver 404 si el costo no existe (manejado por servicio)")
    void deleteCost_asAdmin_whenCostNotFound_shouldReturnNotFound() throws Exception {
        when(costService.deleteCost(98L, 99L))
                .thenThrow(new CostException(Constants.COST_NOT_EXISTS, HttpStatus.NOT_FOUND));

        mockMvc.perform(delete("/api/costs/admin/98/99")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(token -> token.claim("role", "ADMIN"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is(Constants.COST_NOT_EXISTS)));
    }

    @Test
    @DisplayName("GET /api/costs/shortest-path - Debería devolver la ruta más corta")
    void getShortestPath_shouldReturnShortestPath() throws Exception {
        List<SalePointPathItem> path = List.of(new SalePointPathItem(1L, "A"), new SalePointPathItem(2L, "B"));
        ShortestPathDTO shortestPathDTO = new ShortestPathDTO(path, 10.0);
        when(costService.getShortestPath(1L, 2L)).thenReturn(ResponseEntity.ok(shortestPathDTO));

        mockMvc.perform(get("/api/costs/shortest-path")
                        .param("from", "1")
                        .param("to", "2")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalCost", is(10.0)))
                .andExpect(jsonPath("$.path", hasSize(2)))
                .andExpect(jsonPath("$.path[0].name", is("A")));
    }

    @Test
    @DisplayName("GET /api/costs/shortest-path - Debería devolver 404 si no hay ruta (manejado por servicio)")
    void getShortestPath_whenNoPath_shouldReturnNotFound() throws Exception {
        when(costService.getShortestPath(1L, 99L))
                .thenThrow(new CostException("No path found between points", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/costs/shortest-path")
                        .param("from", "1")
                        .param("to", "99")
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("No path found between points")));
    }


    @Test
    @DisplayName("GET /api/costs/direct-connections/{fromId} - Debería devolver conexiones directas")
    void getDirectConnections_shouldReturnDirectConnections() throws Exception {
        Set<CostDTO> directConnections = Set.of(costDTO1);
        when(costService.getDirectConnections(1L)).thenReturn(ResponseEntity.ok(directConnections));

        mockMvc.perform(get("/api/costs/direct-connections/1")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].toId", is(2)));
    }

    @Test
    @DisplayName("GET /api/costs/direct-connections/{fromId} - Debería devolver 404 si no hay conexiones (manejado por servicio)")
    void getDirectConnections_whenNoConnections_shouldReturnNotFound() throws Exception {
        when(costService.getDirectConnections(99L))
                .thenThrow(new CostException(Constants.NOT_DIRECT_CONNECTIONS, HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/costs/direct-connections/99")
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is(Constants.NOT_DIRECT_CONNECTIONS)));
    }
}
