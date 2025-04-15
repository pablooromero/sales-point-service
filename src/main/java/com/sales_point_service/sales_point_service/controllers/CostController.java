package com.sales_point_service.sales_point_service.controllers;

import com.sales_point_service.sales_point_service.dtos.CostDTO;
import com.sales_point_service.sales_point_service.dtos.CreateCostRequest;
import com.sales_point_service.sales_point_service.dtos.ShortestPathDTO;
import com.sales_point_service.sales_point_service.exceptions.CostException;
import com.sales_point_service.sales_point_service.services.CostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("api/costs")
public class CostController {

    @Autowired
    private CostService costService;

    @GetMapping
    public ResponseEntity<Set<CostDTO>> findAll() {
        return costService.getAllCosts();
    }

    @PostMapping
    public ResponseEntity<String> createCost(@RequestBody CreateCostRequest newCost) throws CostException {
        return costService.createCost(newCost);
    }

    @DeleteMapping("/{fromId}/{toId}")
    public ResponseEntity<String> deleteCost(@PathVariable Long fromId, @PathVariable Long toId) throws CostException {
        return costService.deleteCost(fromId, toId);
    }

    @GetMapping("/shortest-path")
    public ResponseEntity<ShortestPathDTO> getShortestPath(
            @RequestParam Long from,
            @RequestParam Long to
    ) throws CostException {
        return costService.getShortestPath(from, to);
    }

    @GetMapping("/direct-connections/{fromId}")
    public ResponseEntity<Set<CostDTO>> getDirectConnections(@PathVariable Long fromId) throws CostException {
        return costService.getDirectConnections(fromId);
    }

}
