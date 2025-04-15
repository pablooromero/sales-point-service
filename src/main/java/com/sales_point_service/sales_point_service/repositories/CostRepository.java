package com.sales_point_service.sales_point_service.repositories;

import com.sales_point_service.sales_point_service.models.Cost;
import com.sales_point_service.sales_point_service.models.CostId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CostRepository extends JpaRepository<Cost, CostId> {
    Optional<Cost> findById(CostId id);
}
