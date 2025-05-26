package com.sales_point_service.sales_point_service.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sales_point_service.sales_point_service.config.SecurityConfig;
import com.sales_point_service.sales_point_service.dtos.CreateSalePointRequest;
import com.sales_point_service.sales_point_service.dtos.SalePointDTO;
import com.sales_point_service.sales_point_service.dtos.UpdateSalePointRequest;
import com.sales_point_service.sales_point_service.exceptions.ExceptionHandlers;
import com.sales_point_service.sales_point_service.exceptions.SalePointException;
import com.sales_point_service.sales_point_service.services.SalePointService;
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
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SalePointController.class)
@Import({SecurityConfig.class, ExceptionHandlers.class})
public class SalePointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SalePointService salePointService;

    @Autowired
    private ObjectMapper objectMapper;

    private SalePointDTO salePointDTO1;
    private SalePointDTO salePointDTO2;
    private Set<SalePointDTO>  salePointDTOSet;

    public SalePointControllerTest() {}

    @BeforeEach
    void setUp() {
        salePointDTO1 = new SalePointDTO(1L, "Point A");
        salePointDTO2 = new SalePointDTO(2L, "Point B");
        salePointDTOSet = new HashSet<>();
        salePointDTOSet.add(salePointDTO1);
        salePointDTOSet.add(salePointDTO2);
    }

    @Test
    @DisplayName("GET /api/sales-point - Debería devolver todos los puntos de venta")
    void getAllSalesPoint_shouldReturnAllSalePoints() throws Exception {
        when(salePointService.getAllSalePoints()).thenReturn(ResponseEntity.ok(salePointDTOSet));

        mockMvc.perform(get("/api/sales-point")
                    .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.id == 1 && @.name == 'Point A')]", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/sales-point/{id} - Debería devolver un punto de venta si existe")
    void getSalePointById_whenExists_shouldReturnSalePoint() throws Exception {
        when(salePointService.getSalePointById(1L)).thenReturn(ResponseEntity.ok(salePointDTO1));

        mockMvc.perform(get("/api/sales-point/{id}", 1L)
                    .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Point A")));
    }

    @Test
    @DisplayName("GET /api/sales-point/{id} - Debería devolver 404 si no existe")
    void getSalePointById_whenNotExists_shouldReturnNotFound() throws Exception {
        when(salePointService.getSalePointById(99L))
                .thenThrow(new SalePointException("Sale point not found with ID: 99", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/sales-point/99")
                        .with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/sales-point/admin - Debería crear punto de venta si es ADMIN")
    void createSalePoint_asAdmin_shouldCreateSalePoint() throws Exception {
        CreateSalePointRequest request = new CreateSalePointRequest("New Point");
        SalePointDTO createdDto = new SalePointDTO(3L, "New Point");
        when(salePointService.createSalePoint(any(CreateSalePointRequest.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(createdDto));

        mockMvc.perform(post("/api/sales-point/admin")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(token -> token.claim("id", "123").subject("admin@example.com").claim("role", "ADMIN"))
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.name", is("New Point")));
    }

    @Test
    @DisplayName("POST /api/sales-point/admin - Debería devolver 403 si NO es ADMIN")
    void createSalePoint_asNonAdmin_shouldReturnForbidden() throws Exception {
        CreateSalePointRequest request = new CreateSalePointRequest("New Point");

        mockMvc.perform(post("/api/sales-point/admin")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(token -> token.claim("role", "USER"))
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", is("Acceso denegado. No tiene los permisos necesarios para realizar esta acción.")));
    }

    @Test
    @DisplayName("POST /api/sales-point/admin - Debería devolver 401 si no está autenticado")
    void createSalePoint_notAuthenticated_shouldReturnUnauthorized() throws Exception {
        CreateSalePointRequest request = new CreateSalePointRequest("New Point");

        mockMvc.perform(post("/api/sales-point/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/sales-point/admin/{id} - Debería actualizar si es ADMIN")
    void updateSalePoint_asAdmin_shouldUpdateSalePoint() throws Exception {
        UpdateSalePointRequest request = new UpdateSalePointRequest(1L, "Updated Point A");
        SalePointDTO updatedDto = new SalePointDTO(1L, "Updated Point A");
        when(salePointService.updateSalePoint(eq(1L), any(UpdateSalePointRequest.class)))
                .thenReturn(ResponseEntity.ok(updatedDto));

        mockMvc.perform(put("/api/sales-point/admin/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(token -> token.claim("role", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Point A")));
    }

    @Test
    @DisplayName("DELETE /api/sales-point/admin/{id} - Debería eliminar si es ADMIN")
    void deleteSalePoint_asAdmin_shouldDeleteSalePoint() throws Exception {
        when(salePointService.deleteSalePoint(1L))
                .thenReturn(ResponseEntity.ok(Constants.SALE_POINTS_DELETED + 1L));

        mockMvc.perform(delete("/api/sales-point/admin/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(token -> token.claim("role", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(content().string(Constants.SALE_POINTS_DELETED + 1L));
    }
}
