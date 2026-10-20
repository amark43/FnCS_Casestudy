package com.fulfilment.application.monolith.fulfilment;

import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class JpaFulfilmentCatalog implements FulfilmentCatalog {

  @Inject ProductRepository productRepository;
  @Inject WarehouseRepository warehouseRepository;

  @Override
  public boolean storeExists(Long storeId) {
    return Store.findById(storeId) != null;
  }

  @Override
  public boolean productExists(Long productId) {
    return productRepository.findById(productId) != null;
  }

  @Override
  public boolean activeWarehouseExists(Long warehouseId) {
    Warehouse warehouse = warehouseRepository.getById(warehouseId);
    return warehouse != null && warehouse.archivedAt == null;
  }
}
