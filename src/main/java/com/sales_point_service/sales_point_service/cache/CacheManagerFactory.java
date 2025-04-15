package com.sales_point_service.sales_point_service.cache;

import com.sales_point_service.sales_point_service.cache.implementations.CostCacheManagerImplementation;
import com.sales_point_service.sales_point_service.cache.implementations.SalePointCacheManagerImplementation;
import com.sales_point_service.sales_point_service.cache.interfaces.CacheManager;
import com.sales_point_service.sales_point_service.enums.CacheType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class CacheManagerFactory {
    private final Map<CacheType, CacheManager<?, ?>> cacheManagers = new EnumMap<>(CacheType.class);

    @Autowired
    public CacheManagerFactory(
            SalePointCacheManagerImplementation salePointCacheManager,
            CostCacheManagerImplementation costCacheManager
    ) {
        cacheManagers.put(CacheType.SALE_POINT, salePointCacheManager);
        cacheManagers.put(CacheType.COST, costCacheManager);
    }

    @SuppressWarnings("unchecked")
    public <ID, T> CacheManager<ID, T> getCacheManager(CacheType cacheType) {
        return (CacheManager<ID, T>) cacheManagers.get(cacheType);
    }
}
