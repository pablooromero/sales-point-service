package com.sales_point_service.sales_point_service.services.implementations;

import com.sales_point_service.sales_point_service.cache.interfaces.CacheManager;
import com.sales_point_service.sales_point_service.cache.CacheManagerFactory;
import com.sales_point_service.sales_point_service.dtos.*;
import com.sales_point_service.sales_point_service.enums.CacheType;
import com.sales_point_service.sales_point_service.exceptions.CostException;
import com.sales_point_service.sales_point_service.exceptions.SalePointException;
import com.sales_point_service.sales_point_service.models.Cost;
import com.sales_point_service.sales_point_service.models.CostId;
import com.sales_point_service.sales_point_service.repositories.CostRepository;
import com.sales_point_service.sales_point_service.services.CostService;
import com.sales_point_service.sales_point_service.utils.Constants;
import com.sales_point_service.sales_point_service.utils.CostUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CostServiceImplementation implements CostService {

    private final CostRepository costRepository;

    private final CostUtils costUtils;

    private final CacheManagerFactory cacheManagerFactory;

    private CacheManager<CostId, Cost> getCostCache() {
        return cacheManagerFactory.getCacheManager(CacheType.COST);
    }


    @Override
    @Transactional
    public Cost saveCost(Cost cost) {
        log.info(Constants.SAVING_COST, cost);

        CostId canonicalId = costUtils.createOrderedCostId(cost.getId().getFromId(), cost.getId().getToId());
        cost.setId(canonicalId);

        Cost savedCost = costRepository.save(cost);
        getCostCache().add(savedCost);
        log.info(Constants.COST_SAVED_SUCCESSFULLY);

        return savedCost;
    }

    @Override
    public ResponseEntity<Set<CostDTO>> getAllCosts() {
        log.info(Constants.GET_ALL_COSTS);
        CacheManager<CostId, Cost> costCache = getCostCache();

        if (costCache.isEmpty()) {
            log.info(Constants.POPULATING_CACHE_FROM_REPOSITORY);
            costRepository.findAll().forEach(costCache::add);
        } else {
            log.info(Constants.GET_ALL_COSTS + " from cache");
        }

        Set<CostDTO> costs = costCache.getAll()
                .stream()
                .map(cost -> {
                    Long fromId = cost.getId().getFromId();
                    Long toId = cost.getId().getToId();
                    String fromName = costCache.getSalePointName(fromId);
                    String toName = costCache.getSalePointName(toId);
                    return new CostDTO(fromId, fromName, toId, toName, cost.getCost());
                })
                .collect(Collectors.toSet());

        log.info(Constants.GET_ALL_COSTS_SUCCESSFULLY);
        return ResponseEntity.ok(costs);
    }

    @Override
    @Transactional
    public ResponseEntity<String> createCost(CreateCostRequest newCost) {
        log.info(Constants.CREATING_COST);
        LocalDateTime now = LocalDateTime.now();

        if (newCost.cost() < 0) {
            log.error(Constants.COST_NOT_NEGATIVE + " Value: {}", newCost.cost());
            throw new CostException(Constants.COST_NOT_NEGATIVE, HttpStatus.NOT_ACCEPTABLE);
        }

        CostId orderedId = costUtils.createOrderedCostId(newCost.from(), newCost.to());

        Optional<Cost> existingCostOpt = costRepository.findById(orderedId);
        Cost costToSave;

        if (existingCostOpt.isPresent()) {
            log.info(Constants.UPDATING_COST, orderedId);
            costToSave = existingCostOpt.get();
            costToSave.setCost(Objects.equals(newCost.from(), newCost.to()) ? 0.0 : newCost.cost());
            costToSave.setUpdatedAt(now);
        } else {
            log.info(Constants.CREATING_COST);
            costToSave = new Cost(orderedId, newCost.cost(), now, now, null);
            costToSave.setCost(Objects.equals(newCost.from(), newCost.to()) ? 0.0 : newCost.cost());
            costToSave.setCreatedAt(now);
            costToSave.setUpdatedAt(now);
        }

        saveCost(costToSave);

        log.info(Constants.COST_CREATED_SUCCESSFULLY + " for ID: {}", orderedId);
        return new ResponseEntity<>(Constants.COST_CREATED_SUCCESSFULLY, HttpStatus.CREATED);
    }

    @Override
    @Transactional
    public ResponseEntity<String> deleteCost(Long fromId, Long toId) {
        log.info(Constants.DELETING_COST, fromId, toId);

        CostId costId = costUtils.createOrderedCostId(fromId, toId);

        if (!costRepository.existsById(costId)) {
            log.warn(Constants.COST_NOT_EXISTS + " for ID: {}", costId);
            throw new CostException(Constants.COST_NOT_EXISTS, HttpStatus.NOT_FOUND);
        }

        costRepository.deleteById(costId);
        getCostCache().remove(costId);

        log.info(Constants.COST_DELETED_SUCCESSFULLY);
        return new ResponseEntity<>(Constants.COST_DELETED, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Set<CostDTO>> getDirectConnections(Long originId) {
        log.info(Constants.GET_DIRECT_CONNECTIONS);

        CacheManager<CostId, Cost> costCache = getCostCache();

        if (costCache.isEmpty()) {
            log.info(Constants.POPULATING_CACHE_FOR_GET_DIRECT_CONNECTIONS);
            costCache.bulkLoad(Set.copyOf(costRepository.findAll()));
        }

        Map<Long, Map<Long, Double>> graph = costCache.getGraph();
        Map<Long, Double> neighbors = graph.get(originId);

        if (neighbors == null || neighbors.isEmpty()) {
            log.warn(Constants.NOT_DIRECT_CONNECTIONS);
            throw new CostException(Constants.NOT_DIRECT_CONNECTIONS, HttpStatus.NOT_FOUND);
        }

        Set<CostDTO> result = neighbors.entrySet().stream()
                .map(entry -> {
                    Long toId = entry.getKey();
                    Double cost = entry.getValue();
                    return new CostDTO(
                            originId,
                            costCache.getSalePointName(originId),
                            toId,
                            costCache.getSalePointName(toId),
                            cost
                    );
                })
                .collect(Collectors.toSet());

        log.info(Constants.GET_DIRECT_CONNECTIONS_SUCCESSFULLY);
        return ResponseEntity.ok(result);
    }


    @Override
    @SuppressWarnings("unchecked")
    public ResponseEntity<ShortestPathDTO> getShortestPath(Long origin, Long destination) {
        log.info(Constants.GET_SHORTEST_PATH);
        CacheManager<CostId, Cost> costCache = getCostCache();

        if (costCache.isEmpty()) {
            log.info(Constants.POPULATING_CACHE_FROM_REPOSITORY);
            costCache.bulkLoad(Set.copyOf(costRepository.findAll()));
        }

        Map<Long, Map<Long, Double>> costGraph = costCache.getGraph();

        if (!costGraph.containsKey(origin)) {
            log.warn(Constants.SOURCE_NODE_NOT_FOUND, origin);
            throw new CostException(Constants.SALE_POINTS_NOT_CONNECTED + " (Source node not found in the graph)", HttpStatus.NOT_FOUND);
        }
        if (!costGraph.containsKey(destination)) {
            log.warn(Constants.DESTINATION_NODE_NOT_FOUND, destination);
            throw new CostException(Constants.SALE_POINTS_NOT_CONNECTED + " (Destination node not found in the graph)", HttpStatus.NOT_FOUND);
        }

        Map<String, Object> result = costUtils.calculateShortestPath(origin, destination, costGraph);

        List<Long> pathIds = (List<Long>) result.get("path");
        Double totalCost = (Double) result.get("cost");

        if (pathIds == null || pathIds.isEmpty()) {
            throw new CostException(Constants.SALE_POINTS_NOT_CONNECTED + " (Empty road)", HttpStatus.NOT_FOUND);
        }

        List<SalePointPathItem> fullPath = pathIds.stream()
                .map(id -> {
                    String salePointName = getCostCache().getSalePointName(id);
                    if (salePointName.startsWith("Unknown SP ID:")) {
                        log.error(Constants.SALE_POINTS_NOT_FOUND + id);
                        throw new SalePointException(Constants.SALE_POINTS_NOT_FOUND + id, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                    return new SalePointPathItem(id, salePointName);
                })
                .toList();

        log.info(Constants.GET_SHORTEST_PATH_SUCCESSFULLY);

        ShortestPathDTO response = new ShortestPathDTO(fullPath, totalCost);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}