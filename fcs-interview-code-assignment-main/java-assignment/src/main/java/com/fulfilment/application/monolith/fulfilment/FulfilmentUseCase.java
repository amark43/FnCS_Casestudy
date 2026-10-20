package com.fulfilment.application.monolith.fulfilment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class FulfilmentUseCase {

  static final int MAX_WAREHOUSES_PER_PRODUCT_AND_STORE = 2;
  static final int MAX_WAREHOUSES_PER_STORE = 3;
  static final int MAX_PRODUCTS_PER_WAREHOUSE = 5;

  private final FulfilmentRepository fulfilmentRepository;
  private final FulfilmentCatalog fulfilmentCatalog;

  public FulfilmentUseCase(
      FulfilmentRepository fulfilmentRepository, FulfilmentCatalog fulfilmentCatalog) {
    this.fulfilmentRepository = fulfilmentRepository;
    this.fulfilmentCatalog = fulfilmentCatalog;
  }

  public List<FulfilmentLink> list() {
    return fulfilmentRepository.listLinks();
  }

  @Transactional
  public FulfilmentLink associate(FulfilmentLink request) {
    requireIdentifiers(request);
    requireKnownParties(request);

    List<FulfilmentLink> existing = fulfilmentRepository.listLinks();
    if (existing.stream().anyMatch(link -> sameTriple(link, request))) {
      throw new FulfilmentViolation(
          400, "This product is already fulfilled by this warehouse for the store");
    }

    long warehousesForProductAtStore =
        existing.stream()
            .filter(link -> sameStoreAndProduct(link, request))
            .map(link -> link.warehouseId)
            .distinct()
            .count();
    if (warehousesForProductAtStore >= MAX_WAREHOUSES_PER_PRODUCT_AND_STORE) {
      throw new FulfilmentViolation(
          400, "A product can be fulfilled by at most 2 warehouses per store");
    }

    boolean warehouseAlreadyServesStore =
        existing.stream().anyMatch(link -> sameStoreAndWarehouse(link, request));
    long warehousesForStore =
        existing.stream()
            .filter(link -> request.storeId.equals(link.storeId))
            .map(link -> link.warehouseId)
            .distinct()
            .count();
    if (!warehouseAlreadyServesStore && warehousesForStore >= MAX_WAREHOUSES_PER_STORE) {
      throw new FulfilmentViolation(400, "A store can be fulfilled by at most 3 warehouses");
    }

    boolean productAlreadyStored =
        existing.stream().anyMatch(link -> sameWarehouseAndProduct(link, request));
    long productsInWarehouse =
        existing.stream()
            .filter(link -> request.warehouseId.equals(link.warehouseId))
            .map(link -> link.productId)
            .distinct()
            .count();
    if (!productAlreadyStored && productsInWarehouse >= MAX_PRODUCTS_PER_WAREHOUSE) {
      throw new FulfilmentViolation(400, "A warehouse can store at most 5 product types");
    }

    return fulfilmentRepository.create(request);
  }

  @Transactional
  public void remove(Long id) {
    if (id == null || fulfilmentRepository.getById(id) == null) {
      throw new FulfilmentViolation(404, "Fulfilment association does not exist");
    }
    fulfilmentRepository.delete(id);
  }

  private void requireIdentifiers(FulfilmentLink request) {
    if (request == null || request.storeId == null || request.productId == null || request.warehouseId == null) {
      throw new FulfilmentViolation(400, "Store, product and warehouse are required");
    }
  }

  private void requireKnownParties(FulfilmentLink request) {
    if (!fulfilmentCatalog.storeExists(request.storeId)) {
      throw new FulfilmentViolation(404, "Store does not exist");
    }
    if (!fulfilmentCatalog.productExists(request.productId)) {
      throw new FulfilmentViolation(404, "Product does not exist");
    }
    if (!fulfilmentCatalog.activeWarehouseExists(request.warehouseId)) {
      throw new FulfilmentViolation(404, "Warehouse does not exist");
    }
  }

  private boolean sameTriple(FulfilmentLink link, FulfilmentLink request) {
    return sameStoreAndProduct(link, request) && request.warehouseId.equals(link.warehouseId);
  }

  private boolean sameStoreAndProduct(FulfilmentLink link, FulfilmentLink request) {
    return request.storeId.equals(link.storeId) && request.productId.equals(link.productId);
  }

  private boolean sameStoreAndWarehouse(FulfilmentLink link, FulfilmentLink request) {
    return request.storeId.equals(link.storeId) && request.warehouseId.equals(link.warehouseId);
  }

  private boolean sameWarehouseAndProduct(FulfilmentLink link, FulfilmentLink request) {
    return request.warehouseId.equals(link.warehouseId) && request.productId.equals(link.productId);
  }

}
