package com.sales_point_service.sales_point_service.repositories;

import com.sales_point_service.sales_point_service.models.SalePoint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalePointRepository extends JpaRepository<SalePoint, Long> {
}