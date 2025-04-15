package com.sales_point_service.sales_point_service.services.implementations;

import com.sales_point_service.sales_point_service.cache.interfaces.CacheManager;
import com.sales_point_service.sales_point_service.cache.CacheManagerFactory;
import com.sales_point_service.sales_point_service.dtos.CreateSalePointRequest;
import com.sales_point_service.sales_point_service.dtos.SalePointDTO;
import com.sales_point_service.sales_point_service.dtos.UpdateSalePointRequest;
import com.sales_point_service.sales_point_service.enums.CacheType;
import com.sales_point_service.sales_point_service.exceptions.SalePointException;
import com.sales_point_service.sales_point_service.models.SalePoint;
import com.sales_point_service.sales_point_service.repositories.SalePointRepository;
import com.sales_point_service.sales_point_service.services.SalePointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SalePointServiceImplementation implements SalePointService {

    @Autowired
    private SalePointRepository salePointRepository;

    @Autowired
    private CacheManagerFactory cacheManagerFactory;

    private CacheManager<Long, SalePoint> getSalePointCache() {
        return cacheManagerFactory.getCacheManager(CacheType.SALE_POINT);
    }

    @Override
    public SalePoint saveSalePoint(SalePoint salePoint) {
        SalePoint savedSalePoint = salePointRepository.save(salePoint);

        getSalePointCache().add(savedSalePoint);

        return savedSalePoint;
    }

    @Override
    public ResponseEntity<Set<SalePointDTO>> getAllSalePoints() {
        CacheManager<Long, SalePoint> salePointCache = getSalePointCache();
        if (salePointCache.isEmpty()) {
            salePointRepository.findAll().forEach(salePointCache::add);
        }

        Set<SalePointDTO> salePoints = salePointCache.getAll()
                .stream()
                .map(salePoint -> new SalePointDTO(salePoint.getId(), salePoint.getName()))
                .collect(Collectors.toSet());

        return new ResponseEntity<>(salePoints, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<SalePointDTO> getSalePointById(Long id) throws SalePointException {
        CacheManager<Long, SalePoint> salePointCache = getSalePointCache();

        SalePoint salePoint = salePointCache.getById(id);

        if(salePoint == null) {
            salePoint = salePointRepository.findById(id)
                    .orElseThrow(() -> new SalePointException("SalePoint with id " + id + " not found", HttpStatus.NOT_FOUND));

            salePointCache.add(salePoint);
        }

        SalePointDTO salePointDTO = new SalePointDTO(salePoint.getId(), salePoint.getName());
        return new ResponseEntity<>(salePointDTO, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<SalePointDTO> createSalePoint(CreateSalePointRequest newSalePoint) throws SalePointException {
        validateName(newSalePoint.name());

        SalePoint salePoint = new SalePoint();
        salePoint.setName(newSalePoint.name());

        saveSalePoint(salePoint);

        SalePointDTO salePointDTO = new SalePointDTO(salePoint.getId(), salePoint.getName());

        return new ResponseEntity<>(salePointDTO, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<SalePointDTO> updateSalePoint(Long id, UpdateSalePointRequest updateSalePoint) throws SalePointException {
        validateName(updateSalePoint.name());

        CacheManager<Long, SalePoint> salePointCache = getSalePointCache();

        SalePoint salePoint = salePointCache.getById(id);

        if(salePoint == null) {
            salePoint = salePointRepository.findById(id)
                    .orElseThrow(() -> new SalePointException("SalePoint with id " + id + " not found", HttpStatus.NOT_FOUND));
        }

        salePoint.setName(updateSalePoint.name());
        saveSalePoint(salePoint);

        SalePointDTO salePointDTO = new SalePointDTO(salePoint.getId(), salePoint.getName());

        return new ResponseEntity<>(salePointDTO, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> deleteSalePoint(Long id) {
        if(!salePointRepository.existsById(id)) {
            return new ResponseEntity<>("SalePoint with id " + id + " not found", HttpStatus.NOT_FOUND);
        }

        salePointRepository.deleteById(id);
        getSalePointCache().remove(id);

        return new ResponseEntity<>("SalePoint with id " + id + " deleted", HttpStatus.OK);
    }

    private void validateName(String name) throws SalePointException {
        if(name == null || name.isBlank()) {
            throw new SalePointException("Name cannot be null", HttpStatus.BAD_REQUEST);
        }
    }
}