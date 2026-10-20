package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.location.LocationGateway;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReplaceWarehouseUseCaseTest {

  private InMemoryWarehouseStore warehouseStore;
  private CreateWarehouseUseCase createWarehouseUseCase;
  private ReplaceWarehouseUseCase useCase;

  @BeforeEach
  public void setUp() {
    warehouseStore = new InMemoryWarehouseStore();
    LocationGateway locationGateway = new LocationGateway();
    createWarehouseUseCase = new CreateWarehouseUseCase(warehouseStore, locationGateway);
    useCase = new ReplaceWarehouseUseCase(warehouseStore, locationGateway);
  }

  @Test
  public void archivesTheCurrentWarehouseAndCreatesTheReplacement() {
    createWarehouseUseCase.create(warehouse("MWH.200", "EINDHOVEN-001", 20, 8));

    Warehouse replacement = warehouse("MWH.200", "EINDHOVEN-001", 30, 8);
    useCase.replace(replacement);

    Warehouse active = warehouseStore.findByBusinessUnitCode("MWH.200");
    assertEquals(replacement.id, active.id);
    assertEquals(30, active.capacity);
    assertEquals(8, active.stock);
    assertEquals(1, warehouseStore.getAll().stream().filter(w -> w.archivedAt != null).count());
  }

  @Test
  public void rejectsUnknownBusinessUnitCode() {
    WarehouseNotFoundException exception =
        assertThrows(
            WarehouseNotFoundException.class,
            () -> useCase.replace(warehouse("MWH.MISSING", "EINDHOVEN-001", 20, 1)));

    assertEquals(
        "Warehouse with business unit code MWH.MISSING does not exist", exception.getMessage());
  }

  @Test
  public void rejectsStockThatDoesNotMatchThePreviousWarehouse() {
    createWarehouseUseCase.create(warehouse("MWH.201", "EINDHOVEN-001", 20, 8));

    WarehouseValidationException exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> useCase.replace(warehouse("MWH.201", "EINDHOVEN-001", 20, 7)));

    assertEquals(
        "Stock of the new warehouse must match the stock of the warehouse being replaced",
        exception.getMessage());
    assertNotNull(warehouseStore.findByBusinessUnitCode("MWH.201"));
    assertEquals(20, warehouseStore.findByBusinessUnitCode("MWH.201").capacity);
  }

  @Test
  public void rejectsCapacityThatCannotHoldThePreviousStock() {
    createWarehouseUseCase.create(warehouse("MWH.202", "VETSBY-001", 20, 8));

    WarehouseValidationException exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> useCase.replace(warehouse("MWH.202", "VETSBY-001", 7, 8)));

    assertEquals(
        "New warehouse capacity cannot accommodate the stock of the warehouse being replaced",
        exception.getMessage());
  }

  @Test
  public void rejectsReplacementAtAnInvalidOrFullLocation() {
    createWarehouseUseCase.create(warehouse("MWH.203", "ZWOLLE-002", 10, 2));
    createWarehouseUseCase.create(warehouse("MWH.204", "HELMOND-001", 10, 2));

    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.replace(warehouse("MWH.203", "NOWHERE-001", 10, 2)));
    WarehouseValidationException full =
        assertThrows(
            WarehouseValidationException.class,
            () -> useCase.replace(warehouse("MWH.203", "HELMOND-001", 10, 2)));

    assertEquals("Maximum number of warehouses reached for this location", full.getMessage());
  }

  @Test
  public void rejectsReplacementThatWouldExceedLocationCapacity() {
    createWarehouseUseCase.create(warehouse("MWH.205", "AMSTERDAM-002", 20, 4));
    createWarehouseUseCase.create(warehouse("MWH.206", "AMSTERDAM-002", 40, 4));

    WarehouseValidationException exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> useCase.replace(warehouse("MWH.205", "AMSTERDAM-002", 40, 4)));

    assertEquals(
        "Warehouse capacity exceeds the maximum capacity of the location", exception.getMessage());
  }

  @Test
  public void allowsReplacementWhenTheArchivedWarehouseFreesItsOwnCapacity() {
    createWarehouseUseCase.create(warehouse("MWH.207", "ZWOLLE-002", 40, 6));

    useCase.replace(warehouse("MWH.207", "ZWOLLE-002", 45, 6));

    Warehouse active = warehouseStore.findByBusinessUnitCode("MWH.207");
    assertEquals(45, active.capacity);
    assertNull(active.archivedAt);
  }

  @Test
  public void rejectsIncompleteReplacement() {
    assertThrows(WarehouseValidationException.class, () -> useCase.replace(new Warehouse()));
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
