package com.fulfilment.application.monolith.fulfilment;

/** A warehouse supplying one product to one store. */
public class FulfilmentLink {

  public Long id;
  public Long storeId;
  public Long productId;
  public Long warehouseId;

  public FulfilmentLink() {}

  public FulfilmentLink(Long storeId, Long productId, Long warehouseId) {
    this.storeId = storeId;
    this.productId = productId;
    this.warehouseId = warehouseId;
  }
}
