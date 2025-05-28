package com.sales_point_service.sales_point_service.utils;

import com.sales_point_service.sales_point_service.dtos.NodeDistance;
import com.sales_point_service.sales_point_service.exceptions.CostException;
import com.sales_point_service.sales_point_service.models.CostId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CostUtils {

    public CostId createOrderedCostId(Long a, Long b) {
        return a < b ? new CostId(a, b) : new CostId(b, a);
    }

    public Map<String, Object> calculateShortestPath(
            Long origin,
            Long destination,
            Map<Long, Map<Long, Double>> graph
    ) {
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

            Map<Long, Double> neighbors = graph.get(currentNode);
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
            throw new CostException(Constants.PATH_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        List<Long> pathIds = reconstructPath(previous, origin, destination);
        Double totalCost = distances.get(destination);

        Map<String, Object> result = new HashMap<>();
        result.put("path", pathIds);
        result.put("cost", totalCost);
        return result;
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
