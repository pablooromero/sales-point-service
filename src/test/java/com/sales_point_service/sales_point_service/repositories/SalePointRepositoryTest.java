package com.sales_point_service.sales_point_service.repositories;

import com.sales_point_service.sales_point_service.models.SalePoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class SalePointRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SalePointRepository salePointRepository;

    @Test
    @DisplayName("Cuando se guarda un SalePoint, debería poder ser encontrado por ID")
    void whenSaveSalePoint_thenFindById_shouldReturnSalePoint() {
        SalePoint salePoint = new SalePoint(null, "Test Point Alpha");
        SalePoint savedSalePoint = entityManager.persistAndFlush(salePoint);

        Optional<SalePoint> foundSalePointOptional = salePointRepository.findById(savedSalePoint.getId());

        assertTrue(foundSalePointOptional.isPresent());
        assertThat(foundSalePointOptional.get().getName()).isEqualTo("Test Point Alpha");
        assertThat(foundSalePointOptional.get().getId()).isEqualTo(savedSalePoint.getId());
    }

    @Test
    @DisplayName("findAll debería devolver todos los SalePoints guardados")
    void findAll_shouldReturnAllSavedSalePoints() {
        SalePoint sp1 = new SalePoint(null, "Point Gamma");
        SalePoint sp2 = new SalePoint(null, "Point Delta");
        entityManager.persist(sp1);
        entityManager.persist(sp2);
        entityManager.flush();

        List<SalePoint> salePoints = salePointRepository.findAll();

        assertThat(salePoints).hasSize(2);
        assertThat(salePoints).extracting(SalePoint::getName).containsExactlyInAnyOrder("Point Gamma", "Point Delta");
    }

    @Test
    @DisplayName("findById debería devolver Optional.empty si no se encuentra el SalePoint")
    void findById_whenNotFound_shouldReturnEmptyOptional() {
        Optional<SalePoint> foundSalePoint = salePointRepository.findById(999L);

        assertFalse(foundSalePoint.isPresent());
    }

    @Test
    @DisplayName("deleteById debería eliminar el SalePoint")
    void deleteById_shouldRemoveSalePoint() {
        SalePoint salePoint = new SalePoint(null, "Point Epsilon");
        SalePoint savedSalePoint = entityManager.persistAndFlush(salePoint);
        Long id = savedSalePoint.getId();

        salePointRepository.deleteById(id);
        entityManager.flush();
        entityManager.clear();

        Optional<SalePoint> deletedSalePoint = salePointRepository.findById(id);

        assertFalse(deletedSalePoint.isPresent());
    }

    @Test
    @DisplayName("existsById debería devolver true si el SalePoint existe")
    void existsById_whenExists_shouldReturnTrue() {
        SalePoint salePoint = new SalePoint(null, "Point Zeta");
        SalePoint savedSalePoint = entityManager.persistAndFlush(salePoint);

        boolean exists = salePointRepository.existsById(savedSalePoint.getId());

        assertTrue(exists);
    }

    @Test
    @DisplayName("existsById debería devolver false si el SalePoint no existe")
    void existsById_whenNotExists_shouldReturnFalse() {
        boolean exists = salePointRepository.existsById(999L);

        assertFalse(exists);
    }
}
