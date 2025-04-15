package com.sales_point_service.sales_point_service.services;

import com.sales_point_service.sales_point_service.dtos.CreateSalePointRequest;
import com.sales_point_service.sales_point_service.dtos.SalePointDTO;
import com.sales_point_service.sales_point_service.dtos.UpdateSalePointRequest;
import com.sales_point_service.sales_point_service.exceptions.SalePointException;
import com.sales_point_service.sales_point_service.models.SalePoint;
import org.springframework.http.ResponseEntity;

import java.util.Set;

public interface SalePointService {
    SalePoint saveSalePoint(SalePoint salePoint);

    ResponseEntity<Set<SalePointDTO>> getAllSalePoints();

    ResponseEntity<SalePointDTO> getSalePointById(Long id) throws SalePointException;

    ResponseEntity<SalePointDTO> createSalePoint(CreateSalePointRequest newSalePoint) throws SalePointException;

    ResponseEntity<SalePointDTO> updateSalePoint(Long id, UpdateSalePointRequest updateSalePoint) throws SalePointException;

    ResponseEntity<String> deleteSalePoint(Long id);
}
