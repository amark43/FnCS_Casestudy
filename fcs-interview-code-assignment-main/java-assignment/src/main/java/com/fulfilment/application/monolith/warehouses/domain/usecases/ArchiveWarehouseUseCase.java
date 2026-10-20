package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

@ApplicationScoped
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

  private final WarehouseStore warehouseStore;

  public ArchiveWarehouseUseCase(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  @Override
  @Transactional
  public void archive(Warehouse warehouse) {
    if (warehouse == null) {
      throw new WarehouseNotFoundException("Warehouse does not exist");
    }

    Warehouse current =
        warehouse.id != null
            ? warehouseStore.getById(warehouse.id)
            : warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (current == null || current.archivedAt != null) {
      throw new WarehouseNotFoundException("Warehouse does not exist");
    }

    current.archivedAt = LocalDateTime.now();
    warehouseStore.update(current);
    warehouse.archivedAt = current.archivedAt;
  }
}
