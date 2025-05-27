package com.sales_point_service.sales_point_service.cache.implementations;

import com.sales_point_service.sales_point_service.cache.interfaces.CacheManager;
import com.sales_point_service.sales_point_service.models.Cost;
import com.sales_point_service.sales_point_service.models.CostId;
import com.sales_point_service.sales_point_service.models.SalePoint;
import com.sales_point_service.sales_point_service.repositories.SalePointRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import com.sales_point_service.sales_point_service.repositories.CostRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CostCacheManagerImplementation implements CacheManager<CostId, Cost> {

    private final Map<Long, Map<Long, Double>> costGraph = new ConcurrentHashMap<>();
    private final Map<Long, String> salePointNames = new HashMap<>();

    private final CostRepository costRepository;

    private final SalePointRepository salePointRepository;

    @PostConstruct
    public void initCache() {
        Set<Cost> allCosts = Set.copyOf(costRepository.findAll());
        bulkLoad(allCosts);
        loadSalePointNames();
        System.out.println("Cost cache initialized");
    }

    @Override
    public void loadSalePointNames() {
        List<SalePoint> allSalePoints = salePointRepository.findAll();
        for (SalePoint sp : allSalePoints) {
            salePointNames.put(sp.getId(), sp.getName());
        }
    }

    @Override
    public String getSalePointName(Long salePointId) {
        return salePointNames.getOrDefault(salePointId, "Unknown");
    }

    @Override
    @CachePut(value = "costs", key = "#cost.id")
    public void add(Cost cost) {
        Long from = cost.getId().getFromId();
        Long to = cost.getId().getToId();
        Double value = cost.getCost();

        costGraph.computeIfAbsent(from, k -> new ConcurrentHashMap<>()).put(to, value);
        costGraph.computeIfAbsent(to, k -> new ConcurrentHashMap<>()).put(from, value);
    }

    @Override
    @CachePut(value = "costs", key = "#cost.id")
    public void update(Cost cost) {
        add(cost);
    }

    @Override
    @CacheEvict(value = "costs", key = "#costId")
    public void remove(CostId costId) {
        Long from = costId.getFromId();
        Long to = costId.getToId();

        costGraph.getOrDefault(from, new ConcurrentHashMap<>()).remove(to);
        costGraph.getOrDefault(to, new ConcurrentHashMap<>()).remove(from);
    }

    @Override
    @Cacheable(value = "costs", key = "#costId")
    public Cost getById(CostId costId) {
        Double value = costGraph.getOrDefault(costId.getFromId(), Map.of()).get(costId.getToId());
        return value != null ? new Cost(costId, value, null, null, null) : null;
    }

    @Override
    public Set<Cost> getAll() {
        return costGraph.entrySet().stream()
                .flatMap(entry -> entry.getValue().entrySet().stream()
                        .map(inner -> {
                            Long from = entry.getKey();
                            Long to = inner.getKey();
                            double cost = inner.getValue();
                            return new Cost(new CostId(from, to), cost, null, null, null);
                        }))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isEmpty() {
        return costGraph.isEmpty();
    }

    @Override
    public void bulkLoad(Set<Cost> values) {
        costGraph.clear();
        values.forEach(this::add);
    }

    @Override
    public Map<Long, Map<Long, Double>> getGraph() {
        return costGraph;
    }
}
