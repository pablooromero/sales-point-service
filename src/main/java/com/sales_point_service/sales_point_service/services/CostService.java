package com.sales_point_service.sales_point_service.services;

import com.sales_point_service.sales_point_service.dtos.CostDTO;
import com.sales_point_service.sales_point_service.dtos.CreateCostRequest;
import com.sales_point_service.sales_point_service.dtos.ShortestPathDTO;
import com.sales_point_service.sales_point_service.exceptions.CostException;
import com.sales_point_service.sales_point_service.exceptions.SalePointException;
import com.sales_point_service.sales_point_service.models.Cost;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;

public interface CostService {
    Cost saveCost(Cost cost);

    ResponseEntity<Set<CostDTO>> getAllCosts();

    ResponseEntity<String> createCost(CreateCostRequest newCost) throws CostException;

    ResponseEntity<String> deleteCost(Long fromId, Long toId) throws CostException;

    ResponseEntity<Set<CostDTO>> getDirectConnections(Long originId) throws CostException;

    ResponseEntity<ShortestPathDTO> getShortestPath(Long origin, Long destination) throws CostException, SalePointException;
}
