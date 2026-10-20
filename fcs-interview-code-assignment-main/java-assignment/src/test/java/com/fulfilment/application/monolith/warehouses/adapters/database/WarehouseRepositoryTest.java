package com.fulfilment.application.monolith.warehouses.adapters.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WarehouseRepositoryTest {

  @Inject WarehouseRepository warehouseRepository;

  @Test
  @TestTransaction
  public void createsUpdatesFindsAndRemovesAWarehouse() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.REPO";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 10;
    warehouse.stock = 2;
    warehouse.createdAt = LocalDateTime.of(2024, 1, 2, 3, 4);

    warehouseRepository.create(warehouse);

    assertNotNull(warehouse.id);
    assertEquals(10, warehouseRepository.getById(warehouse.id).capacity);
    assertEquals("MWH.REPO", warehouseRepository.findByBusinessUnitCode("MWH.REPO").businessUnitCode);
    assertTrue(
        warehouseRepository.getAll().stream()
            .anyMatch(existing -> "MWH.REPO".equals(existing.businessUnitCode)));

    warehouse.stock = 4;
    warehouse.archivedAt = LocalDateTime.of(2024, 2, 2, 0, 0);
    warehouseRepository.update(warehouse);

    assertNull(warehouseRepository.findByBusinessUnitCode("MWH.REPO"));
    assertEquals(4, warehouseRepository.getById(warehouse.id).stock);
    assertNotNull(warehouseRepository.getById(warehouse.id).archivedAt);

    warehouseRepository.remove(warehouse);
    warehouseRepository.remove(null);
    warehouseRepository.remove(new Warehouse());
    assertNull(warehouseRepository.getById(warehouse.id));

    Warehouse missing = new Warehouse();
    missing.id = 424242L;
    assertThrows(WarehouseNotFoundException.class, () -> warehouseRepository.update(missing));
  }

  @Test
  @TestTransaction
  public void assignsACreationTimestampWhenTheCallerDoesNot() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.REPO-2";
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 8;
    warehouse.stock = 1;

    warehouseRepository.create(warehouse);

    assertNotNull(warehouseRepository.getById(warehouse.id).createdAt);
  }
}
