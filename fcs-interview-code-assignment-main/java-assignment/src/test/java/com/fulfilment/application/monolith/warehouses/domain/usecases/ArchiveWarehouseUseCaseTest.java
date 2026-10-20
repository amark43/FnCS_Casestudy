package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.location.LocationGateway;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArchiveWarehouseUseCaseTest {

  private InMemoryWarehouseStore warehouseStore;
  private ArchiveWarehouseUseCase useCase;

  @BeforeEach
  public void setUp() {
    warehouseStore = new InMemoryWarehouseStore();
    CreateWarehouseUseCase createWarehouseUseCase =
        new CreateWarehouseUseCase(warehouseStore, new LocationGateway());
    createWarehouseUseCase.create(warehouse("MWH.300", "HELMOND-001", 15, 3));
    useCase = new ArchiveWarehouseUseCase(warehouseStore);
  }

  @Test
  public void archivesAnActiveWarehouseById() {
    Warehouse active = warehouseStore.findByBusinessUnitCode("MWH.300");

    useCase.archive(active);

    assertNull(warehouseStore.findByBusinessUnitCode("MWH.300"));
    assertNotNull(warehouseStore.getById(active.id).archivedAt);
    assertNotNull(active.archivedAt);
  }

  @Test
  public void archivesAnActiveWarehouseByBusinessUnitCodeWhenIdIsMissing() {
    Warehouse reference = new Warehouse();
    reference.businessUnitCode = "MWH.300";

    useCase.archive(reference);

    assertNull(warehouseStore.findByBusinessUnitCode("MWH.300"));
  }

  @Test
  public void rejectsMissingOrAlreadyArchivedWarehouses() {
    assertThrows(WarehouseNotFoundException.class, () -> useCase.archive(null));

    Warehouse missing = new Warehouse();
    missing.id = 999L;
    assertThrows(WarehouseNotFoundException.class, () -> useCase.archive(missing));

    Warehouse active = warehouseStore.findByBusinessUnitCode("MWH.300");
    useCase.archive(active);
    assertThrows(WarehouseNotFoundException.class, () -> useCase.archive(active));
  }

  @Test
  public void removeDeletesTheStoredWarehouse() {
    Warehouse active = warehouseStore.findByBusinessUnitCode("MWH.300");
    warehouseStore.remove(null);
    warehouseStore.remove(new Warehouse());
    warehouseStore.remove(active);

    assertNull(warehouseStore.getById(active.id));
  }

  private Warehouse warehouse(String code, String location, int capacity, int stock) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = code;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }
}
