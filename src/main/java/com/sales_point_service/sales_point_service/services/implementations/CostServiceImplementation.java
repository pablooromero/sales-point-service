package com.sales_point_service.sales_point_service.services.implementations;

import com.sales_point_service.sales_point_service.cache.interfaces.CacheManager;
import com.sales_point_service.sales_point_service.cache.CacheManagerFactory;
import com.sales_point_service.sales_point_service.dtos.*;
import com.sales_point_service.sales_point_service.enums.CacheType;
import com.sales_point_service.sales_point_service.exceptions.CostException;
import com.sales_point_service.sales_point_service.exceptions.SalePointException;
import com.sales_point_service.sales_point_service.models.Cost;
import com.sales_point_service.sales_point_service.models.CostId;
import com.sales_point_service.sales_point_service.models.SalePoint;
import com.sales_point_service.sales_point_service.repositories.CostRepository;
import com.sales_point_service.sales_point_service.repositories.SalePointRepository;
import com.sales_point_service.sales_point_service.services.CostService;
import com.sales_point_service.sales_point_service.utils.Constants;
import com.sales_point_service.sales_point_service.utils.CostUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CostServiceImplementation implements CostService {

    private final CostRepository costRepository;

    private final CostUtils costUtils;

    private final SalePointRepository salePointRepository;

    private final CacheManagerFactory cacheManagerFactory;

    private CacheManager<CostId, Cost> getCostCache() {
        return cacheManagerFactory.getCacheManager(CacheType.COST);
    }


    @Override
    public Cost saveCost(Cost cost) {
        log.info(Constants.SAVING_COST, cost);

        Cost savedCost = costRepository.save(cost);
        getCostCache().add(cost);

        log.info(Constants.COST_SAVED_SUCCESSFULLY);
        return savedCost;
    }

    @Override
    public ResponseEntity<Set<CostDTO>> getAllCosts() {
        log.info(Constants.GET_ALL_COSTS);

        CacheManager<CostId, Cost> costCache = getCostCache();
        if (costCache.isEmpty()) {
            costRepository.findAll().forEach(costCache::add);
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
    public ResponseEntity<String> createCost(CreateCostRequest newCost) throws CostException {
        log.info(Constants.CREATING_COST);
        LocalDateTime now = LocalDateTime.now();

        if (newCost.cost() < 0) {
            throw new CostException(Constants.COST_NOT_NEGATIVE, HttpStatus.NOT_ACCEPTABLE);
        }

        CostId orderedId = costUtils.createOrderedCostId(newCost.from(), newCost.to());

        Cost cost = costRepository.findById(orderedId)
                .orElse(new Cost(orderedId, newCost.cost(), now, now, null));

        cost.setCost(Objects.equals(newCost.from(), newCost.to()) ? 0.0 : newCost.cost());
        cost.setCreatedAt(now);

        saveCost(cost);

        log.info(Constants.COST_CREATED_SUCCESSFULLY);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<String> deleteCost(Long fromId, Long toId) throws CostException {
        log.info(Constants.DELETING_COST, fromId, toId);

        CostId costId = costUtils.createOrderedCostId(fromId, toId);
        Optional<Cost> cost = costRepository.findById(costId);

        if (cost.isEmpty()) {
            throw new CostException(Constants.COST_NOT_EXISTS, HttpStatus.NOT_FOUND);
        }

        costRepository.deleteById(costId);
        getCostCache().remove(costId);

        log.info(Constants.COST_DELETED_SUCCESSFULLY);
        return new ResponseEntity<>(Constants.COST_DELETED, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Set<CostDTO>> getDirectConnections(Long originId) throws CostException {
        log.info(Constants.GET_DIRECT_CONNECTIONS);

        CacheManager<CostId, Cost> costCache = getCostCache();

        if (costCache.isEmpty()) {
            costCache.bulkLoad(Set.copyOf(costRepository.findAll()));
        }

        Map<Long, Double> neighbors = costCache.getGraph().get(originId);

        if (neighbors == null || neighbors.isEmpty()) {
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
    public ResponseEntity<ShortestPathDTO> getShortestPath(Long origin, Long destination) throws CostException, SalePointException {
        log.info(Constants.GET_SHORTEST_PATH);

        CacheManager<CostId, Cost> costCache = getCostCache();

        if (costCache.isEmpty()) {
            costCache.bulkLoad(Set.copyOf(costRepository.findAll()));
        }

        Map<Long, Map<Long, Double>> costGraph = costCache.getGraph();

        if (!costGraph.containsKey(origin) || !costGraph.containsKey(destination)) {
            throw new CostException(Constants.SALE_POINTS_NOT_CONNECTED, HttpStatus.NOT_FOUND);
        }

        Map<String, Object> result = costUtils.calculateShortestPath(origin, destination, costGraph);

        List<Long> pathIds = (List<Long>) result.get("path");
        Double totalCost = (Double) result.get("cost");

        Map<Long, SalePoint> salePointsMap = salePointRepository.findAllById(pathIds)
                .stream()
                .collect(Collectors.toMap(SalePoint::getId, sp -> sp));

        List<SalePointPathItem> fullPath = pathIds.stream()
                .map(id -> salePointRepository.findById(id)
                        .map(sp -> new SalePointPathItem(sp.getId(), sp.getName()))
                        .orElseThrow(() -> new SalePointException(Constants.SALE_POINTS_NOT_FOUND + id, HttpStatus.NOT_FOUND)))
                .toList();

        log.info(Constants.GET_SHORTEST_PATH_SUCCESSFULLY);

        ShortestPathDTO response = new ShortestPathDTO(fullPath, totalCost);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}