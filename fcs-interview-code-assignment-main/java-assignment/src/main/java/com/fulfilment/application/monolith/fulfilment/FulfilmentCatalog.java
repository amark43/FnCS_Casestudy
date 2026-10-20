package com.fulfilment.application.monolith.fulfilment;

public interface FulfilmentCatalog {

  boolean storeExists(Long storeId);

  boolean productExists(Long productId);

  boolean activeWarehouseExists(Long warehouseId);
}
