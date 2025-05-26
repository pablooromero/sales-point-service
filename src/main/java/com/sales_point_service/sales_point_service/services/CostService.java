package com.sales_point_service.sales_point_service.services;

import com.sales_point_service.sales_point_service.dtos.CostDTO;
import com.sales_point_service.sales_point_service.dtos.CreateCostRequest;
import com.sales_point_service.sales_point_service.dtos.ShortestPathDTO;
import com.sales_point_service.sales_point_service.models.Cost;
import org.springframework.http.ResponseEntity;

import java.util.Set;

public interface CostService {
    Cost saveCost(Cost cost);

    ResponseEntity<Set<CostDTO>> getAllCosts();

    ResponseEntity<String> createCost(CreateCostRequest newCost);

    ResponseEntity<String> deleteCost(Long fromId, Long toId);

    ResponseEntity<Set<CostDTO>> getDirectConnections(Long originId);

    ResponseEntity<ShortestPathDTO> getShortestPath(Long origin, Long destination);
}
