package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.location.LocationGateway;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CreateWarehouseUseCaseTest {

  private InMemoryWarehouseStore warehouseStore;
  private CreateWarehouseUseCase useCase;

  @BeforeEach
  public void setUp() {
    warehouseStore = new InMemoryWarehouseStore();
    useCase = new CreateWarehouseUseCase(warehouseStore, new LocationGateway());
  }

  @Test
  public void createsWarehouseWhenLocationHasRoom() {
    Warehouse warehouse = warehouse("MWH.100", "HELMOND-001", 20, 5);

    useCase.create(warehouse);

    Warehouse stored = warehouseStore.findByBusinessUnitCode("MWH.100");
    assertNotNull(warehouse.id);
    assertNotNull(stored);
    assertEquals(20, stored.capacity);
    assertEquals(5, stored.stock);
    assertNull(stored.archivedAt);
    assertNotNull(stored.createdAt);
  }

  @Test
  public void rejectsMissingRequiredFields() {
    Warehouse warehouse = new Warehouse();

    WarehouseValidationException exception =
        assertThrows(WarehouseValidationException.class, () -> useCase.create(warehouse));

    assertEquals(
        "Business unit code, location, capacity and stock are required", exception.getMessage());
  }

  @Test
  public void rejectsBlankBusinessUnitCodeAndLocation() {
    Warehouse warehouse = warehouse("  ", " ", 10, 1);

    assertThrows(WarehouseValidationException.class, () -> useCase.create(warehouse));
  }

  @Test
  public void rejectsNonPositiveCapacityAndNegativeStock() {
    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.create(warehouse("MWH.101", "HELMOND-001", 0, 0)));
    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.create(warehouse("MWH.102", "HELMOND-001", 10, -1)));
  }

  @Test
  public void rejectsStockAboveCapacity() {
    WarehouseValidationException exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> useCase.create(warehouse("MWH.103", "VETSBY-001", 10, 11)));

    assertEquals("Warehouse capacity cannot handle the informed stock", exception.getMessage());
  }

  @Test
  public void rejectsUnknownLocation() {
    WarehouseValidationException exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> useCase.create(warehouse("MWH.104", "NOWHERE-001", 10, 1)));

    assertEquals("Location is not valid", exception.getMessage());
  }

  @Test
  public void rejectsDuplicatedBusinessUnitCode() {
    useCase.create(warehouse("MWH.105", "AMSTERDAM-002", 10, 1));

    WarehouseValidationException exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> useCase.create(warehouse("MWH.105", "ZWOLLE-002", 10, 1)));

    assertEquals("Business unit code already exists", exception.getMessage());
  }

  @Test
  public void rejectsWhenLocationAlreadyHoldsTheMaximumNumberOfWarehouses() {
    useCase.create(warehouse("MWH.106", "HELMOND-001", 10, 1));

    WarehouseValidationException exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> useCase.create(warehouse("MWH.107", "HELMOND-001", 10, 1)));

    assertEquals(
        "Maximum number of warehouses reached for this location", exception.getMessage());
  }

  @Test
  public void rejectsWhenCombinedCapacityExceedsTheLocationMaximum() {
    useCase.create(warehouse("MWH.108", "ZWOLLE-002", 30, 1));

    WarehouseValidationException exception =
        assertThrows(
            WarehouseValidationException.class,
            () -> useCase.create(warehouse("MWH.109", "ZWOLLE-002", 25, 1)));

    assertEquals(
        "Warehouse capacity exceeds the maximum capacity of the location", exception.getMessage());
  }

  @Test
  public void allowsASecondWarehouseWhenCapacityStillFits() {
    useCase.create(warehouse("MWH.110", "ZWOLLE-002", 20, 1));
    useCase.create(warehouse("MWH.111", "ZWOLLE-002", 20, 1));

    assertNotNull(warehouseStore.findByBusinessUnitCode("MWH.111"));
  }

  @Test
  public void archivedWarehouseDoesNotBlockANewBusinessUnitCodeOrLocationSlot() {
    Warehouse original = warehouse("MWH.112", "TILBURG-001", 10, 1);
    useCase.create(original);
    original.archivedAt = java.time.LocalDateTime.now();
    warehouseStore.update(original);

    useCase.create(warehouse("MWH.112", "TILBURG-001", 10, 1));

    assertNotNull(warehouseStore.findByBusinessUnitCode("MWH.112"));
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
