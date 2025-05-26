package com.sales_point_service.sales_point_service.repositories;

import com.sales_point_service.sales_point_service.models.Cost;
import com.sales_point_service.sales_point_service.models.CostId;
import com.sales_point_service.sales_point_service.models.SalePoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CostRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CostRepository costRepository;

    private SalePoint salePoint1;
    private SalePoint salePoint2;
    private SalePoint salePoint3;

    @BeforeEach
    void setUp() {
        salePoint1 = entityManager.persistFlushFind(new SalePoint(null, "SP_A_CostTest"));
        salePoint2 = entityManager.persistFlushFind(new SalePoint(null, "SP_B_CostTest"));
        salePoint3 = entityManager.persistFlushFind(new SalePoint(null, "SP_C_CostTest"));
    }

    @Test
    @DisplayName("Cuando se guarda un Cost, debería poder ser encontrado por CostId")
    void whenSaveCost_thenFindById_shouldReturnCost() {
        CostId costId = new CostId(salePoint1.getId(), salePoint2.getId());
        Cost cost = new Cost(costId, 25.50, LocalDateTime.now(), LocalDateTime.now(), null);
        Cost savedCost = entityManager.persistAndFlush(cost);

        Optional<Cost> foundCostOptional = costRepository.findById(savedCost.getId());

        assertTrue(foundCostOptional.isPresent());
        assertThat(foundCostOptional.get().getCost()).isEqualTo(25.50);
        assertThat(foundCostOptional.get().getId().getFromId()).isEqualTo(salePoint1.getId());
        assertThat(foundCostOptional.get().getId().getToId()).isEqualTo(salePoint2.getId());
    }

    @Test
    @DisplayName("findAll debería devolver todos los Costs guardados")
    void findAll_shouldReturnAllSavedCosts() {
        CostId id1 = new CostId(salePoint1.getId(), salePoint2.getId());
        Cost cost1 = new Cost(id1, 10.0, LocalDateTime.now(), LocalDateTime.now(), null);
        entityManager.persist(cost1);

        CostId id2 = new CostId(salePoint2.getId(), salePoint3.getId());
        Cost cost2 = new Cost(id2, 15.0, LocalDateTime.now(), LocalDateTime.now(), null);
        entityManager.persist(cost2);
        entityManager.flush();

        List<Cost> costs = costRepository.findAll();

        assertThat(costs).hasSize(2);
        assertThat(costs).extracting(Cost::getCost).containsExactlyInAnyOrder(10.0, 15.0);
    }

    @Test
    @DisplayName("findById debería devolver Optional.empty si no se encuentra el Cost")
    void findById_whenNotFound_shouldReturnEmptyOptional() {
        CostId nonExistentCostId = new CostId(998L, 999L);

        Optional<Cost> foundCost = costRepository.findById(nonExistentCostId);

        assertFalse(foundCost.isPresent());
    }

    @Test
    @DisplayName("deleteById debería eliminar el Cost")
    void deleteById_shouldRemoveCost() {
        CostId costId = new CostId(salePoint1.getId(), salePoint3.getId());
        Cost cost = new Cost(costId, 30.0, LocalDateTime.now(), LocalDateTime.now(), null);
        entityManager.persistAndFlush(cost);

        costRepository.deleteById(costId);
        entityManager.flush();
        entityManager.clear();

        Optional<Cost> deletedCost = costRepository.findById(costId);

        assertFalse(deletedCost.isPresent());
    }
}
