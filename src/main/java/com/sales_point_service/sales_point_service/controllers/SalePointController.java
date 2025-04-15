package com.sales_point_service.sales_point_service.controllers;

import com.sales_point_service.sales_point_service.dtos.CreateSalePointRequest;
import com.sales_point_service.sales_point_service.dtos.SalePointDTO;
import com.sales_point_service.sales_point_service.dtos.UpdateSalePointRequest;
import com.sales_point_service.sales_point_service.exceptions.SalePointException;
import com.sales_point_service.sales_point_service.services.SalePointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("api/sales-point")
public class SalePointController {

    @Autowired
    private SalePointService salePointService;

    @GetMapping
    public ResponseEntity<Set<SalePointDTO>> getAllSalePoints() {
        return salePointService.getAllSalePoints();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalePointDTO> getSalePointById(@PathVariable Long id) throws SalePointException {
        return salePointService.getSalePointById(id);
    }

    @PostMapping
    public ResponseEntity<SalePointDTO> createSalePoint(@RequestBody CreateSalePointRequest createSalePointRequest) throws SalePointException {
        return salePointService.createSalePoint(createSalePointRequest);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalePointDTO> updateSalePoint(@PathVariable Long id, @RequestBody UpdateSalePointRequest updateSalePointRequest) throws SalePointException {
        return salePointService.updateSalePoint(id, updateSalePointRequest);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSalePoint(@PathVariable Long id) {
        return salePointService.deleteSalePoint(id);
    }
}
