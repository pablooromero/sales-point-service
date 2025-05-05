package com.sales_point_service.sales_point_service.controllers;

import com.sales_point_service.sales_point_service.dtos.CreateSalePointRequest;
import com.sales_point_service.sales_point_service.dtos.SalePointDTO;
import com.sales_point_service.sales_point_service.dtos.UpdateSalePointRequest;
import com.sales_point_service.sales_point_service.exceptions.SalePointException;
import com.sales_point_service.sales_point_service.services.SalePointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Tag(name = "Sales Point", description = "Sales Point Controller")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/sales-point")
public class SalePointController {

    private final SalePointService salePointService;

    @Operation(summary = "Get all sale points", description = "Returns all sale points stored in the system")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of all sale points",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SalePointDTO.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping
    public ResponseEntity<Set<SalePointDTO>> getAllSalePoints() {
        return salePointService.getAllSalePoints();
    }


    @Operation(summary = "Get sale point by ID", description = "Returns a specific sale point by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sale point found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SalePointDTO.class))),
            @ApiResponse(responseCode = "404", description = "Sale point not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<SalePointDTO> getSalePointById(@PathVariable Long id) throws SalePointException {
        return salePointService.getSalePointById(id);
    }


    @Operation(summary = "Create a new sale point", description = "Creates a new sale point")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sale point created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @PostMapping("/admin")
    public ResponseEntity<SalePointDTO> createSalePoint(@RequestBody CreateSalePointRequest createSalePointRequest) throws SalePointException {
        return salePointService.createSalePoint(createSalePointRequest);
    }


    @Operation(summary = "Update sale point", description = "Updates the name of a specific sale point")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sale point updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Sale point not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @PutMapping("/admin/{id}")
    public ResponseEntity<SalePointDTO> updateSalePoint(@PathVariable Long id, @RequestBody UpdateSalePointRequest updateSalePointRequest) throws SalePointException {
        return salePointService.updateSalePoint(id, updateSalePointRequest);
    }


    @Operation(summary = "Delete sale point", description = "Deletes a specific sale point by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sale point deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Sale point not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<String> deleteSalePoint(@PathVariable Long id) {
        return salePointService.deleteSalePoint(id);
    }
}
