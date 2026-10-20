package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import java.util.List;

final class WarehouseRules {

  private WarehouseRules() {}

  static void requireIdentityAndCapacity(Warehouse warehouse) {
    if (warehouse == null
        || isBlank(warehouse.businessUnitCode)
        || isBlank(warehouse.location)
        || warehouse.capacity == null
        || warehouse.stock == null) {
      throw new WarehouseValidationException(
          "Business unit code, location, capacity and stock are required");
    }
    if (warehouse.capacity <= 0) {
      throw new WarehouseValidationException("Capacity must be greater than zero");
    }
    if (warehouse.stock < 0) {
      throw new WarehouseValidationException("Stock cannot be negative");
    }
  }

  static void requireStockWithinCapacity(Warehouse warehouse) {
    if (warehouse.stock > warehouse.capacity) {
      throw new WarehouseValidationException(
          "Warehouse capacity cannot handle the informed stock");
    }
  }

  static void requireFeasibleAtLocation(
      Warehouse warehouse, Location location, List<Warehouse> activeAtLocation) {
    if (location == null) {
      throw new WarehouseValidationException("Location is not valid");
    }
    if (activeAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new WarehouseValidationException(
          "Maximum number of warehouses reached for this location");
    }
    int usedCapacity =
        activeAtLocation.stream().mapToInt(existing -> existing.capacity == null ? 0 : existing.capacity).sum();
    if (usedCapacity + warehouse.capacity > location.maxCapacity) {
      throw new WarehouseValidationException(
          "Warehouse capacity exceeds the maximum capacity of the location");
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
