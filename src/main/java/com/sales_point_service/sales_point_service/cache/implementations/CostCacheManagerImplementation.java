package com.sales_point_service.sales_point_service.cache.implementations;

import com.sales_point_service.sales_point_service.cache.interfaces.CacheManager;
import com.sales_point_service.sales_point_service.models.Cost;
import com.sales_point_service.sales_point_service.models.CostId;
import com.sales_point_service.sales_point_service.models.SalePoint;
import com.sales_point_service.sales_point_service.repositories.SalePointRepository;
import com.sales_point_service.sales_point_service.utils.Constants;
import com.sales_point_service.sales_point_service.utils.CostUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import com.sales_point_service.sales_point_service.repositories.CostRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class CostCacheManagerImplementation implements CacheManager<CostId, Cost> {

    private final Map<CostId, Cost> canonicalCostsMap = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Double>> costGraph = new ConcurrentHashMap<>();
    private final Map<Long, String> salePointNames = new HashMap<>();

    private final CostRepository costRepository;

    private final SalePointRepository salePointRepository;

    private final CostUtils costUtils;

    @PostConstruct
    public void initCache() {
        log.info(Constants.INIT_CACHE);
        loadSalePointNames();

        Set<Cost> allCosts = Set.copyOf(costRepository.findAll());
        bulkLoad(allCosts);
        log.info(Constants.INIT_CACHE_SUCCESSFULLY);
    }

    @Override
    public void loadSalePointNames() {
        salePointNames.clear();

        List<SalePoint> allSalePoints = salePointRepository.findAll();
        for (SalePoint sp : allSalePoints) {
            salePointNames.put(sp.getId(), sp.getName());
        }
    }

    @Override
    public String getSalePointName(Long salePointId) {
        String name = salePointNames.get(salePointId);
        if (name == null) {
            log.warn(Constants.SALE_POINT_NAME_NOT_FOUND_IN_CACHE, salePointId);
            Optional<SalePoint> spOpt = salePointRepository.findById(salePointId);
            if (spOpt.isPresent()) {
                name = spOpt.get().getName();
                salePointNames.put(salePointId, name);
                log.info(Constants.GET_SALE_POINT_NAME_FROM_CACHE_SUCCESSFULLY, salePointId);
            } else {
                log.error(Constants.SALE_POINT_NAME_NOT_FOUND_IN_BD, salePointId);
                name = "Unknown SP ID: " + salePointId;
            }
        }
        return name;
    }

    @Override
    @CachePut(value = "costs", key = "#cost.id")
    public void add(Cost cost) {
        if (cost == null || cost.getId() == null || cost.getId().getFromId() == null || cost.getId().getToId() == null) {
            log.warn(Constants.CREATING_COST_WITH_NULL_ID_ATTEMPT, cost);
            return;
        }

        CostId canonicalId = costUtils.createOrderedCostId(cost.getId().getFromId(), cost.getId().getToId());

        Cost costToCache = new Cost(canonicalId, cost.getCost(), cost.getCreatedAt(), cost.getUpdatedAt(), cost.getDeletedAt());

        log.info(Constants.CREATING_UPDATING_COST_IN_CACHE, canonicalId, costToCache.getCost());
        canonicalCostsMap.put(canonicalId, costToCache);

        Long from = canonicalId.getFromId();
        Long to = canonicalId.getToId();
        Double value = costToCache.getCost();

        costGraph.computeIfAbsent(from, k -> new ConcurrentHashMap<>()).put(to, value);
        costGraph.computeIfAbsent(to, k -> new ConcurrentHashMap<>()).put(from, value);

        log.info(Constants.COST_GRAPH_UPDATED_SUCCESSFULLY, from, to, value);
    }

    @Override
    @CachePut(value = "costs", key = "#cost.id")
    public void update(Cost cost) {
        log.info(Constants.UPDATING_COST_IN_CACHE, cost);
        add(cost);
    }

    @Override
    @CacheEvict(value = "costs", key = "#costId")
    public void remove(CostId costId) {
        if (costId == null || costId.getFromId() == null || costId.getToId() == null) {
            log.warn(Constants.DELETING_COST_WITH_NULL_ID_IN_CACHE, costId);
            return;
        }

        CostId canonicalId = costUtils.createOrderedCostId(costId.getFromId(), costId.getToId());
        log.info(Constants.DELETING_COST_IN_CACHE, canonicalId);

        canonicalCostsMap.remove(canonicalId);

        Long from = canonicalId.getFromId();
        Long to = canonicalId.getToId();

        Map<Long, Double> fromNeighbors = costGraph.get(from);
        if (fromNeighbors != null) {
            fromNeighbors.remove(to);
            if (fromNeighbors.isEmpty()) {
                costGraph.remove(from);
            }
        }

        Map<Long, Double> toNeighbors = costGraph.get(to);
        if (toNeighbors != null) {
            toNeighbors.remove(from);
            if (toNeighbors.isEmpty()) {
                costGraph.remove(to);
            }
        }
        log.info(Constants.COST_IN_CACHE_DELETED_SUCCESSFULLY, from, to);
    }

    @Override
    @Cacheable(value = "costs", key = "#costId")
    public Cost getById(CostId costId) {
        if (costId == null || costId.getFromId() == null || costId.getToId() == null) return null;

        CostId canonicalId = costUtils.createOrderedCostId(costId.getFromId(), costId.getToId());

        log.info(Constants.GET_COST_FROM_CACHE, canonicalId);
        return canonicalCostsMap.get(canonicalId);
    }

    @Override
    public Set<Cost> getAll() {
        log.info(Constants.GET_ALL_COSTS_FROM_CACHE, canonicalCostsMap.size());
        return new HashSet<>(canonicalCostsMap.values());
    }

    @Override
    public boolean isEmpty() {
        boolean empty = canonicalCostsMap.isEmpty();

        log.info(Constants.CHECKING_IF_CACHE_IS_EMPTY, empty);
        return empty;
    }

    @Override
    public void bulkLoad(Set<Cost> values) {
        log.info(Constants.PERFORMING_BULK_LOAD_IN_CACHE, values != null ? values.size() : 0);
        canonicalCostsMap.clear();
        costGraph.clear();

        if (values != null) {
            values.forEach(this::add);
        }
        log.info(Constants.BULK_LOAD_IN_CACHE_SUCCESSFULLY, canonicalCostsMap.size());
    }

    @Override
    public Map<Long, Map<Long, Double>> getGraph() {
        log.info(Constants.GET_COST_GRAPH, costGraph.size());

        Map<Long, Map<Long, Double>> unmodifiableGraph = new HashMap<>();
        costGraph.forEach((key, valueMap) -> unmodifiableGraph.put(key, Collections.unmodifiableMap(new HashMap<>(valueMap))));

        return Collections.unmodifiableMap(unmodifiableGraph);
    }
}
