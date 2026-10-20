package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  @Transactional
  public void replace(Warehouse newWarehouse) {
    WarehouseRules.requireIdentityAndCapacity(newWarehouse);

    Warehouse current = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (current == null) {
      throw new WarehouseNotFoundException(
          "Warehouse with business unit code " + newWarehouse.businessUnitCode + " does not exist");
    }

    if (!Objects.equals(newWarehouse.stock, current.stock)) {
      throw new WarehouseValidationException(
          "Stock of the new warehouse must match the stock of the warehouse being replaced");
    }
    if (newWarehouse.capacity < current.stock) {
      throw new WarehouseValidationException(
          "New warehouse capacity cannot accommodate the stock of the warehouse being replaced");
    }

    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
    List<Warehouse> activeAtLocation = activeAt(newWarehouse.location, current);
    WarehouseRules.requireFeasibleAtLocation(newWarehouse, location, activeAtLocation);

    current.archivedAt = LocalDateTime.now();
    warehouseStore.update(current);

    newWarehouse.createdAt = LocalDateTime.now();
    newWarehouse.archivedAt = null;
    warehouseStore.create(newWarehouse);
  }

  private List<Warehouse> activeAt(String location, Warehouse excluded) {
    return warehouseStore.getAll().stream()
        .filter(existing -> existing.archivedAt == null)
        .filter(existing -> location.equals(existing.location))
        .filter(existing -> !sameWarehouse(existing, excluded))
        .toList();
  }

  private boolean sameWarehouse(Warehouse left, Warehouse right) {
    if (left.id != null && right.id != null) {
      return left.id.equals(right.id);
    }
    return Objects.equals(left.businessUnitCode, right.businessUnitCode);
  }
}
