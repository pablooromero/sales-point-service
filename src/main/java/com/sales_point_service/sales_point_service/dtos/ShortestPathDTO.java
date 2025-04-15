package com.sales_point_service.sales_point_service.dtos;

import java.util.List;

public record ShortestPathDTO(List<SalePointPathItem> path, Double totalCost) {
}
