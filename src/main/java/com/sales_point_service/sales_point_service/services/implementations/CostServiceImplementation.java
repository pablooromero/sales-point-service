package com.sales_point_service.sales_point_service.services.implementations;

import com.sales_point_service.sales_point_service.cache.interfaces.CacheManager;
import com.sales_point_service.sales_point_service.cache.CacheManagerFactory;
import com.sales_point_service.sales_point_service.dtos.*;
import com.sales_point_service.sales_point_service.enums.CacheType;
import com.sales_point_service.sales_point_service.exceptions.CostException;
import com.sales_point_service.sales_point_service.models.Cost;
import com.sales_point_service.sales_point_service.models.CostId;
import com.sales_point_service.sales_point_service.repositories.CostRepository;
import com.sales_point_service.sales_point_service.repositories.SalePointRepository;
import com.sales_point_service.sales_point_service.services.CostService;
import com.sales_point_service.sales_point_service.utils.CostUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CostServiceImplementation implements CostService {

    private static final Logger logger = LoggerFactory.getLogger(CostServiceImplementation.class);

    @Autowired
    private CostRepository costRepository;

    @Autowired
    private CostUtils costUtils;

    @Autowired
    private SalePointRepository salePointRepository;

    @Autowired
    private CacheManagerFactory cacheManagerFactory;


    private CacheManager<CostId, Cost> getCostCache() {
        return cacheManagerFactory.getCacheManager(CacheType.COST);
    }


    @Override
    public Cost saveCost(Cost cost) {
        Cost savedCost = costRepository.save(cost);

        getCostCache().add(cost);

        return savedCost;
    }

    @Override
    public ResponseEntity<Set<CostDTO>> getAllCosts() {
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


        return ResponseEntity.ok(costs);
    }

    @Override
    public ResponseEntity<String> createCost(CreateCostRequest newCost) throws CostException {
        LocalDateTime now = LocalDateTime.now();

        if (newCost.cost() < 0) {
            throw new CostException("Cost cannot be negative", HttpStatus.NOT_ACCEPTABLE);
        }

        CostId orderedId = costUtils.createOrderedCostId(newCost.from(), newCost.to());

        Cost cost = costRepository.findById(orderedId)
                .orElse(new Cost(orderedId, newCost.cost(), now, now, null));

        cost.setCost(Objects.equals(newCost.from(), newCost.to()) ? 0.0 : newCost.cost());
        cost.setCreatedAt(now);

        saveCost(cost);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<String> deleteCost(Long fromId, Long toId) throws CostException {
        CostId costId = costUtils.createOrderedCostId(fromId, toId);
        Optional<Cost> cost = costRepository.findById(costId);

        if (cost.isEmpty()) {
            throw new CostException("Cost does not exist", HttpStatus.NOT_FOUND);
        }

        costRepository.deleteById(costId);
        getCostCache().remove(costId);
        return new ResponseEntity<>("Cost removed successfully", HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Set<CostDTO>> getDirectConnections(Long originId) throws CostException {
        CacheManager<CostId, Cost> costCache = getCostCache();

        if (costCache.isEmpty()) {
            costCache.bulkLoad(Set.copyOf(costRepository.findAll()));
        }

        Map<Long, Double> neighbors = costCache.getGraph().get(originId);

        if (neighbors == null || neighbors.isEmpty()) {
            throw new CostException("No direct connections found for this SalePoint", HttpStatus.NOT_FOUND);
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

        return ResponseEntity.ok(result);
    }


    @Override
    public ResponseEntity<ShortestPathDTO> getShortestPath(Long origin, Long destination) throws CostException {
        CacheManager<CostId, Cost> costCache = getCostCache();

        if (costCache.isEmpty()) {
            costCache.bulkLoad(Set.copyOf(costRepository.findAll()));
        }

        Map<Long, Map<Long, Double>> costGraph = costCache.getGraph();

        if (!costGraph.containsKey(origin) || !costGraph.containsKey(destination)) {
            throw new CostException("One or both SalePoints are not connected", HttpStatus.NOT_FOUND);
        }

        Map<Long, Double> distances = new HashMap<>();
        Map<Long, Long> previous = new HashMap<>();
        Set<Long> visited = new HashSet<>();
        PriorityQueue<NodeDistance> queue = new PriorityQueue<>(Comparator.comparingDouble(NodeDistance::distance));

        distances.put(origin, 0.0);
        queue.add(new NodeDistance(origin, 0.0));

        while (!queue.isEmpty()) {
            NodeDistance current = queue.poll();
            Long currentNode = current.node();

            if (!visited.add(currentNode)) continue;

            Map<Long, Double> neighbors = costGraph.get(currentNode);
            if (neighbors == null) continue;

            for (Map.Entry<Long, Double> entry : neighbors.entrySet()) {
                Long neighbor = entry.getKey();
                Double edgeCost = entry.getValue();

                double newDistance = distances.get(currentNode) + edgeCost;
                if (newDistance < distances.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    distances.put(neighbor, newDistance);
                    previous.put(neighbor, currentNode);
                    queue.add(new NodeDistance(neighbor, newDistance));
                }
            }
        }

        if (!distances.containsKey(destination)) {
            throw new CostException("No path found between points", HttpStatus.NOT_FOUND);
        }

        List<Long> pathIds = reconstructPath(previous, origin, destination);
        Double totalCost = distances.get(destination);

        List<SalePointPathItem> fullPath = pathIds.stream()
                .map(id -> salePointRepository.findById(id)
                        .map(sp -> new SalePointPathItem(sp.getId(), sp.getName()))
                        .orElse(new SalePointPathItem(id, "Not name")))
                .toList();

        ShortestPathDTO response = new ShortestPathDTO(fullPath, totalCost);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private List<Long> reconstructPath(Map<Long, Long> previous, Long origin, Long destination) {
        LinkedList<Long> path = new LinkedList<>();
        for (Long at = destination; at != null; at = previous.get(at)) {
            path.addFirst(at);
            if (at.equals(origin)) break;
        }
        return path;
    }
}