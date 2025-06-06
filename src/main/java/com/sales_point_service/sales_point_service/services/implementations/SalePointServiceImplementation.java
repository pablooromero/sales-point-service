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
import com.sales_point_service.sales_point_service.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalePointServiceImplementation implements SalePointService {

    private final SalePointRepository salePointRepository;

    private final CacheManagerFactory cacheManagerFactory;

    private CacheManager<Long, SalePoint> getSalePointCache() {
        return cacheManagerFactory.getCacheManager(CacheType.SALE_POINT);
    }

    @Override
    public SalePoint saveSalePoint(SalePoint salePoint) {
        log.info(Constants.SAVING_SALE_POINT, salePoint);

        SalePoint savedSalePoint = salePointRepository.save(salePoint);
        getSalePointCache().add(savedSalePoint);

        log.info(Constants.SALE_POINT_SAVED_SUCCESSFULLY);
        return savedSalePoint;
    }

    @Override
    public ResponseEntity<Set<SalePointDTO>> getAllSalePoints() {
        log.info(Constants.GET_ALL_SALES_POINT);

        CacheManager<Long, SalePoint> salePointCache = getSalePointCache();
        if (salePointCache.isEmpty()) {
            salePointRepository.findAll().forEach(salePointCache::add);
        }

        Set<SalePointDTO> salePoints = salePointCache.getAll()
                .stream()
                .map(salePoint -> new SalePointDTO(salePoint.getId(), salePoint.getName()))
                .collect(Collectors.toSet());

        log.info(Constants.GET_ALL_SALES_POINT_SUCCESSFULLY);
        return new ResponseEntity<>(salePoints, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<SalePointDTO> getSalePointById(Long id) {
        log.info(Constants.GET_SALE_POINT, id);

        CacheManager<Long, SalePoint> salePointCache = getSalePointCache();

        SalePoint salePoint = salePointCache.getById(id);

        if(salePoint == null) {
            salePoint = salePointRepository.findById(id)
                    .orElseThrow(() -> new SalePointException(Constants.SALE_POINTS_NOT_FOUND + id, HttpStatus.NOT_FOUND));

            salePointCache.add(salePoint);
        }

        log.info(Constants.GET_SALE_POINT_SUCCESSFULLY);

        SalePointDTO salePointDTO = new SalePointDTO(salePoint.getId(), salePoint.getName());
        return new ResponseEntity<>(salePointDTO, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<SalePointDTO> createSalePoint(CreateSalePointRequest newSalePoint) {
        log.info(Constants.CREATING_SALE_POINT, newSalePoint);
        validateName(newSalePoint.name());

        SalePoint salePoint = new SalePoint();
        salePoint.setName(newSalePoint.name());

        SalePoint savedSalePoint = saveSalePoint(salePoint);

        log.info(Constants.SALE_POINT_CREATED_SUCCESSFULLY);

        SalePointDTO salePointDTO = new SalePointDTO(savedSalePoint.getId(), savedSalePoint.getName());
        return new ResponseEntity<>(salePointDTO, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<SalePointDTO> updateSalePoint(Long id, UpdateSalePointRequest updateSalePoint) {
        log.info(Constants.UPDATING_SALE_POINT, id);
        validateName(updateSalePoint.name());

        CacheManager<Long, SalePoint> salePointCache = getSalePointCache();

        SalePoint salePoint = salePointCache.getById(id);

        if(salePoint == null) {
            salePoint = salePointRepository.findById(id)
                    .orElseThrow(() -> new SalePointException(Constants.SALE_POINTS_NOT_FOUND + id, HttpStatus.NOT_FOUND));
        }

        salePoint.setName(updateSalePoint.name());
        SalePoint updatedSalePoint = saveSalePoint(salePoint);

        log.info(Constants.SALE_POINT_UPDATED_SUCCESSFULLY);

        SalePointDTO salePointDTO = new SalePointDTO(updatedSalePoint.getId(), updatedSalePoint.getName());
        return new ResponseEntity<>(salePointDTO, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> deleteSalePoint(Long id) {
        log.info(Constants.DELETING_SALE_POINT, id);

        if(!salePointRepository.existsById(id)) {
            return new ResponseEntity<>(Constants.SALE_POINTS_NOT_FOUND + id, HttpStatus.NOT_FOUND);
        }

        salePointRepository.deleteById(id);
        getSalePointCache().remove(id);

        log.info(Constants.SALE_POINT_DELETED_SUCCESSFULLY);

        return new ResponseEntity<>(Constants.SALE_POINTS_DELETED + id, HttpStatus.OK);
    }

    private void validateName(String name) {
        if(name == null || name.isBlank()) {
            throw new SalePointException(Constants.SALE_POINTS_NAME_NOT_NULL, HttpStatus.BAD_REQUEST);
        }
    }
}