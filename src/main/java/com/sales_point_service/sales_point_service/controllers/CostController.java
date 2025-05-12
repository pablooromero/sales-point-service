package com.sales_point_service.sales_point_service.controllers;

import com.sales_point_service.sales_point_service.dtos.CostDTO;
import com.sales_point_service.sales_point_service.dtos.CreateCostRequest;
import com.sales_point_service.sales_point_service.dtos.ShortestPathDTO;
import com.sales_point_service.sales_point_service.exceptions.CostException;
import com.sales_point_service.sales_point_service.exceptions.SalePointException;
import com.sales_point_service.sales_point_service.services.CostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Tag(name = "Costs", description = "Cost Controller")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/costs")
public class CostController {

    private final CostService costService;

    @Operation(summary = "Get all costs", description = "Returns all costs stored in the system")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of all costs",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CostDTO.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Set<CostDTO>> findAll() {
        return costService.getAllCosts();
    }


    @Operation(summary = "Create a new cost", description = "Creates a new cost between two sale points")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cost created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @PostMapping("/admin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> createCost(@RequestBody CreateCostRequest newCost) throws CostException {
        return costService.createCost(newCost);
    }


    @Operation(summary = "Delete a cost", description = "Deletes a cost between two sale points")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cost deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Cost not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @DeleteMapping("/admin/{fromId}/{toId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> deleteCost(@PathVariable Long fromId, @PathVariable Long toId) throws CostException {
        return costService.deleteCost(fromId, toId);
    }


    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shortest path found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ShortestPathDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Path not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping("/shortest-path")
    public ResponseEntity<ShortestPathDTO> getShortestPath(
            @RequestParam Long from,
            @RequestParam Long to
    ) throws CostException, SalePointException {
        return costService.getShortestPath(from, to);
    }


    @Operation(summary = "Get direct connections", description = "Finds all direct connections from a sale point")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Direct connections found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CostDTO.class))),
            @ApiResponse(responseCode = "404", description = "No direct connections found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping("/direct-connections/{fromId}")
    public ResponseEntity<Set<CostDTO>> getDirectConnections(@PathVariable Long fromId) throws CostException {
        return costService.getDirectConnections(fromId);
    }

}
