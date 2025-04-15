package com.sales_point_service.sales_point_service.cache.interfaces;

import java.util.Map;
import java.util.Set;

public interface CacheManager<ID, T> {
    void loadSalePointNames();

    String getSalePointName(Long salePointId);

    void add(T value);
    void update(T value);
    void remove(ID id);
    T getById(ID id);
    Set<T> getAll();
    boolean isEmpty();
    void bulkLoad(Set<T> values);

    Map<Long, Map<Long, Double>> getGraph();
}