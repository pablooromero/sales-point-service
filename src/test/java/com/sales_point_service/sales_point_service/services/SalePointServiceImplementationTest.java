package com.sales_point_service.sales_point_service.services;

import com.sales_point_service.sales_point_service.cache.CacheManagerFactory;
import com.sales_point_service.sales_point_service.cache.interfaces.CacheManager;
import com.sales_point_service.sales_point_service.dtos.CreateSalePointRequest;
import com.sales_point_service.sales_point_service.dtos.SalePointDTO;
import com.sales_point_service.sales_point_service.dtos.UpdateSalePointRequest;
import com.sales_point_service.sales_point_service.enums.CacheType;
import com.sales_point_service.sales_point_service.exceptions.SalePointException;
import com.sales_point_service.sales_point_service.models.SalePoint;
import com.sales_point_service.sales_point_service.repositories.SalePointRepository;
import com.sales_point_service.sales_point_service.services.implementations.SalePointServiceImplementation;
import com.sales_point_service.sales_point_service.utils.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalePointServiceImplementationTest {

    @Mock
    private SalePointRepository salePointRepository;

    @Mock
    private CacheManagerFactory cacheManagerFactory;

    @Mock
    private CacheManager<Long, SalePoint> salePointCache;

    @InjectMocks
    private SalePointServiceImplementation salePointService;

    private SalePoint salePoint1;
    private SalePointDTO salePointDTO1;

    @BeforeEach
    void setUp() {
        salePoint1 = new SalePoint(1L, "Point A");
        salePointDTO1 = new SalePointDTO(1L, "Point A");

        lenient().when(cacheManagerFactory.getCacheManager(CacheType.SALE_POINT))
                .thenReturn((CacheManager) salePointCache);
    }

    @Test
    @DisplayName("saveSalePoint - Debería guardar y cachear el punto de venta")
    void saveSalePoint_shouldSaveAndCacheSalePoint() {
        when(salePointRepository.save(any(SalePoint.class))).thenReturn(salePoint1);

        SalePoint result = salePointService.saveSalePoint(new SalePoint(null, "Point A"));

        assertNotNull(result);
        assertEquals(salePoint1.getName(), result.getName());
        verify(salePointRepository, times(1)).save(any(SalePoint.class));
        verify(salePointCache, times(1)).add(salePoint1);
        verify(cacheManagerFactory, times(1)).getCacheManager(CacheType.SALE_POINT);
    }

    @Test
    @DisplayName("getAllSalePoints - Debería devolver todos los puntos de venta (cache vacía)")
    void getAllSalePoints_whenCacheIsEmpty_shouldFetchFromRepositoryAndCache() {
        Set<SalePoint> salePointsFromRepo = new HashSet<>(Set.of(salePoint1, new SalePoint(2L, "Point B")));
        when(salePointCache.isEmpty()).thenReturn(true);
        when(salePointRepository.findAll()).thenReturn(List.copyOf(salePointsFromRepo));
        when(salePointCache.getAll()).thenReturn(salePointsFromRepo);

        ResponseEntity<Set<SalePointDTO>> response = salePointService.getAllSalePoints();

        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        assertTrue(response.getBody().stream().anyMatch(dto -> dto.name().equals("Point A")));
        verify(salePointRepository, times(1)).findAll();
        verify(salePointCache, times(salePointsFromRepo.size())).add(any(SalePoint.class));
        verify(cacheManagerFactory, times(1)).getCacheManager(CacheType.SALE_POINT);
    }

    @Test
    @DisplayName("getAllSalePoints - Debería devolver todos los puntos de venta (desde cache)")
    void getAllSalePoints_whenCacheIsNotEmpty_shouldFetchFromCache() {
        Set<SalePoint> salePointsFromCache = new HashSet<>(Set.of(salePoint1, new SalePoint(2L, "Point B")));
        when(salePointCache.isEmpty()).thenReturn(false);
        when(salePointCache.getAll()).thenReturn(salePointsFromCache);

        ResponseEntity<Set<SalePointDTO>> response = salePointService.getAllSalePoints();

        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        verify(salePointRepository, never()).findAll();
        verify(salePointCache, never()).add(any(SalePoint.class));
        verify(cacheManagerFactory, times(1)).getCacheManager(CacheType.SALE_POINT);
    }

    @Test
    @DisplayName("getSalePointById - Debería devolver desde cache si existe")
    void getSalePointById_whenInCache_shouldReturnFromCache() throws SalePointException {
        when(salePointCache.getById(1L)).thenReturn(salePoint1);

        ResponseEntity<SalePointDTO> response = salePointService.getSalePointById(1L);

        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(salePointDTO1.name(), response.getBody().name());
        verify(salePointRepository, never()).findById(anyLong());
        verify(cacheManagerFactory, times(1)).getCacheManager(CacheType.SALE_POINT);
    }

    @Test
    @DisplayName("getSalePointById - Debería devolver desde BD y cachear si no está en cache")
    void getSalePointById_whenNotInCacheButInDb_shouldFetchAndCache() throws SalePointException {
        when(salePointCache.getById(1L)).thenReturn(null);
        when(salePointRepository.findById(1L)).thenReturn(Optional.of(salePoint1));

        ResponseEntity<SalePointDTO> response = salePointService.getSalePointById(1L);

        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(salePointDTO1.name(), response.getBody().name());
        verify(salePointRepository, times(1)).findById(1L);
        verify(salePointCache, times(1)).add(salePoint1);
        verify(cacheManagerFactory, times(1)).getCacheManager(CacheType.SALE_POINT);
    }

    @Test
    @DisplayName("getSalePointById - Debería lanzar SalePointException si no se encuentra")
    void getSalePointById_whenNotFound_shouldThrowSalePointException() {
        when(salePointCache.getById(1L)).thenReturn(null);
        when(salePointRepository.findById(1L)).thenReturn(Optional.empty());

        SalePointException exception = assertThrows(SalePointException.class, () -> {
            salePointService.getSalePointById(1L);
        });
        assertEquals(Constants.SALE_POINTS_NOT_FOUND + 1L, exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        verify(salePointCache, never()).add(any(SalePoint.class));
        verify(cacheManagerFactory, times(1)).getCacheManager(CacheType.SALE_POINT);
    }

    @Test
    @DisplayName("createSalePoint - Debería crear un nuevo punto de venta exitosamente")
    void createSalePoint_withValidName_shouldCreateSuccessfully() throws SalePointException {
        CreateSalePointRequest request = new CreateSalePointRequest("New Point");
        SalePoint savedSalePoint = new SalePoint(2L, "New Point");
        when(salePointRepository.save(any(SalePoint.class))).thenReturn(savedSalePoint);

        ResponseEntity<SalePointDTO> response = salePointService.createSalePoint(request);

        assertNotNull(response.getBody());
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("New Point", response.getBody().name());
        assertEquals(2L, response.getBody().id());
        verify(salePointRepository, times(1)).save(any(SalePoint.class));
        verify(salePointCache, times(1)).add(savedSalePoint);
        verify(cacheManagerFactory, times(1)).getCacheManager(CacheType.SALE_POINT);
    }

    @Test
    @DisplayName("createSalePoint - Debería lanzar SalePointException con nombre nulo")
    void createSalePoint_withNullName_shouldThrowSalePointException() {
        CreateSalePointRequest request = new CreateSalePointRequest(null);

        SalePointException exception = assertThrows(SalePointException.class, () -> {
            salePointService.createSalePoint(request);
        });
        assertEquals(Constants.SALE_POINTS_NAME_NOT_NULL, exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
        verify(salePointRepository, never()).save(any(SalePoint.class));
        verify(salePointCache, never()).add(any(SalePoint.class));
        verify(cacheManagerFactory, never()).getCacheManager(CacheType.SALE_POINT);
    }

    @Test
    @DisplayName("createSalePoint - Debería lanzar SalePointException con nombre vacío")
    void createSalePoint_withBlankName_shouldThrowSalePointException() {
        CreateSalePointRequest request = new CreateSalePointRequest("   ");

        SalePointException exception = assertThrows(SalePointException.class, () -> {
            salePointService.createSalePoint(request);
        });
        assertEquals(Constants.SALE_POINTS_NAME_NOT_NULL, exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
        verify(cacheManagerFactory, never()).getCacheManager(CacheType.SALE_POINT);
    }


    @Test
    @DisplayName("updateSalePoint - Debería actualizar exitosamente (desde cache)")
    void updateSalePoint_whenInCache_shouldUpdateSuccessfully() throws SalePointException {
        UpdateSalePointRequest request = new UpdateSalePointRequest(1L, "Updated Point A");
        SalePoint originalSalePointInCache = new SalePoint(1L, "Point A");
        SalePoint updatedSalePointFromRepo = new SalePoint(1L, "Updated Point A");

        when(salePointCache.getById(1L)).thenReturn(originalSalePointInCache);
        when(salePointRepository.save(any(SalePoint.class))).thenReturn(updatedSalePointFromRepo);

        ResponseEntity<SalePointDTO> response = salePointService.updateSalePoint(1L, request);

        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Updated Point A", response.getBody().name());
        verify(salePointRepository, times(1)).save(any(SalePoint.class));
        verify(salePointCache, times(1)).add(updatedSalePointFromRepo);
        verify(cacheManagerFactory, times(2)).getCacheManager(CacheType.SALE_POINT);
    }

    @Test
    @DisplayName("updateSalePoint - Debería actualizar exitosamente (desde BD)")
    void updateSalePoint_whenInDbNotInCache_shouldUpdateSuccessfully() throws SalePointException {
        UpdateSalePointRequest request = new UpdateSalePointRequest(1L, "Updated Point A");
        SalePoint salePointFromDb = new SalePoint(1L, "Point A");
        SalePoint updatedSalePointFromRepo = new SalePoint(1L, "Updated Point A");

        when(salePointCache.getById(1L)).thenReturn(null);
        when(salePointRepository.findById(1L)).thenReturn(Optional.of(salePointFromDb));
        when(salePointRepository.save(any(SalePoint.class))).thenReturn(updatedSalePointFromRepo);

        ResponseEntity<SalePointDTO> response = salePointService.updateSalePoint(1L, request);

        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Updated Point A", response.getBody().name());
        verify(salePointRepository, times(1)).findById(1L);
        verify(salePointRepository, times(1)).save(any(SalePoint.class));
        verify(salePointCache, times(1)).add(updatedSalePointFromRepo);
        verify(cacheManagerFactory, times(2)).getCacheManager(CacheType.SALE_POINT);
    }


    @Test
    @DisplayName("updateSalePoint - Debería lanzar SalePointException si no se encuentra")
    void updateSalePoint_whenNotFound_shouldThrowSalePointException() {
        UpdateSalePointRequest request = new UpdateSalePointRequest(1L, "Updated Point A");
        when(salePointCache.getById(1L)).thenReturn(null);
        when(salePointRepository.findById(1L)).thenReturn(Optional.empty());

        SalePointException exception = assertThrows(SalePointException.class, () -> {
            salePointService.updateSalePoint(1L, request);
        });
        assertEquals(Constants.SALE_POINTS_NOT_FOUND + 1L, exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        verify(salePointRepository, never()).save(any(SalePoint.class));
        verify(salePointCache, never()).add(any(SalePoint.class));
        verify(cacheManagerFactory, times(1)).getCacheManager(CacheType.SALE_POINT);
    }

    @Test
    @DisplayName("updateSalePoint - Debería lanzar SalePointException con nombre inválido")
    void updateSalePoint_withInvalidName_shouldThrowSalePointException() {
        UpdateSalePointRequest request = new UpdateSalePointRequest(1L, "");

        SalePointException exception = assertThrows(SalePointException.class, () -> {
            salePointService.updateSalePoint(1L, request);
        });
        assertEquals(Constants.SALE_POINTS_NAME_NOT_NULL, exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
        verify(salePointRepository, never()).save(any(SalePoint.class));
        verify(cacheManagerFactory, never()).getCacheManager(CacheType.SALE_POINT);
    }

    @Test
    @DisplayName("deleteSalePoint - Debería eliminar exitosamente")
    void deleteSalePoint_whenExists_shouldDeleteSuccessfully() {
        when(salePointRepository.existsById(1L)).thenReturn(true);
        doNothing().when(salePointRepository).deleteById(1L);
        doNothing().when(salePointCache).remove(1L);

        ResponseEntity<String> response = salePointService.deleteSalePoint(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Constants.SALE_POINTS_DELETED + 1L, response.getBody());
        verify(salePointRepository, times(1)).existsById(1L);
        verify(salePointRepository, times(1)).deleteById(1L);
        verify(salePointCache, times(1)).remove(1L);
        verify(cacheManagerFactory, times(1)).getCacheManager(CacheType.SALE_POINT);
    }

    @Test
    @DisplayName("deleteSalePoint - Debería devolver NOT_FOUND si no existe")
    void deleteSalePoint_whenNotExists_shouldReturnNotFound() {
        when(salePointRepository.existsById(1L)).thenReturn(false);

        ResponseEntity<String> response = salePointService.deleteSalePoint(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(Constants.SALE_POINTS_NOT_FOUND + 1L, response.getBody());
        verify(salePointRepository, times(1)).existsById(1L);
        verify(salePointRepository, never()).deleteById(anyLong());
        verify(salePointCache, never()).remove(anyLong());
        verify(cacheManagerFactory, never()).getCacheManager(CacheType.SALE_POINT);
    }
}