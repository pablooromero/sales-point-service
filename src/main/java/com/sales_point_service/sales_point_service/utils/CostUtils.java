package com.sales_point_service.sales_point_service.utils;

import com.sales_point_service.sales_point_service.models.CostId;
import org.springframework.stereotype.Component;

@Component
public class CostUtils {

    public CostId createOrderedCostId(Long a, Long b) {
        return a < b ? new CostId(a, b) : new CostId(b, a);
    }
}
