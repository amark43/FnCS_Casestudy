package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

final class InMemoryWarehouseStore implements WarehouseStore {

  private final List<Warehouse> warehouses = new ArrayList<>();
  private long sequence = 1;

  @Override
  public List<Warehouse> getAll() {
    return warehouses.stream().map(this::copy).toList();
  }

  @Override
  public void create(Warehouse warehouse) {
    warehouse.id = sequence++;
    if (warehouse.createdAt == null) {
      warehouse.createdAt = LocalDateTime.now();
    }
    warehouses.add(copy(warehouse));
  }

  @Override
  public void update(Warehouse warehouse) {
    for (int index = 0; index < warehouses.size(); index++) {
      if (warehouse.id != null && warehouse.id.equals(warehouses.get(index).id)) {
        warehouses.set(index, copy(warehouse));
        return;
      }
    }
    throw new WarehouseNotFoundException("Warehouse with id " + warehouse.id + " does not exist");
  }

  @Override
  public void remove(Warehouse warehouse) {
    if (warehouse == null || warehouse.id == null) {
      return;
    }
    warehouses.removeIf(existing -> warehouse.id.equals(existing.id));
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    return warehouses.stream()
        .filter(warehouse -> buCode.equals(warehouse.businessUnitCode))
        .filter(warehouse -> warehouse.archivedAt == null)
        .findFirst()
        .map(this::copy)
        .orElse(null);
  }

  @Override
  public Warehouse getById(Long id) {
    return warehouses.stream()
        .filter(warehouse -> id.equals(warehouse.id))
        .findFirst()
        .map(this::copy)
        .orElse(null);
  }

  private Warehouse copy(Warehouse source) {
    Warehouse copy = new Warehouse();
    copy.id = source.id;
    copy.businessUnitCode = source.businessUnitCode;
    copy.location = source.location;
    copy.capacity = source.capacity;
    copy.stock = source.stock;
    copy.createdAt = source.createdAt;
    copy.archivedAt = source.archivedAt;
    return copy;
  }
}
