package com.sales_point_service.sales_point_service.cache.implementations;

import com.sales_point_service.sales_point_service.cache.interfaces.CacheManager;
import com.sales_point_service.sales_point_service.models.SalePoint;
import com.sales_point_service.sales_point_service.repositories.SalePointRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SalePointCacheManagerImplementation implements CacheManager<Long, SalePoint> {
    private final Map<Long, SalePoint> salePointCache = new ConcurrentHashMap<>();

    @Autowired
    private SalePointRepository salePointRepository;

    @PostConstruct
    public void initCache() {
        Set<SalePoint> allSalePoints = Set.copyOf(salePointRepository.findAll());
        bulkLoad(allSalePoints);
        System.out.println("SalePoint cache initialized");
    }

    @Override
    public void loadSalePointNames() {

    }

    @Override
    public String getSalePointName(Long salePointId) {
        return "";
    }

    @Override
    @CachePut(value = "salePoints", key = "#salePoint.id")
    public void add(SalePoint salePoint) {
        salePointCache.put(salePoint.getId(), salePoint);
    }

    @Override
    @CachePut(value = "salePoints", key = "#salePoint.id")
    public void update(SalePoint salePoint) {
        salePointCache.put(salePoint.getId(), salePoint);
    }

    @Override
    @CacheEvict(value = "salePoints", key = "#id")
    public void remove(Long id) {
        salePointCache.remove(id);
    }

    @Override
    @Cacheable(value = "salePoints", key = "#id")
    public SalePoint getById(Long id) {
        return salePointCache.get(id);
    }

    @Override
    public Set<SalePoint> getAll() {
        return Set.copyOf(salePointCache.values());
    }

    @Override
    public boolean isEmpty() {
        return salePointCache.isEmpty();
    }

    @Override
    public void bulkLoad(Set<SalePoint> salePoints) {
        salePointCache.clear();
        salePoints.forEach(s -> salePointCache.put(s.getId(), s));
    }

    @Override
    public Map<Long, Map<Long, Double>> getGraph() {
        return Map.of();
    }
}
