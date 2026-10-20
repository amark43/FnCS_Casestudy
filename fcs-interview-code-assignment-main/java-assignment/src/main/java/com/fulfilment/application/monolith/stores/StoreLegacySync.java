package com.fulfilment.application.monolith.stores;

/** Snapshot of a store change that must be published only after the database commit succeeds. */
public final class StoreLegacySync {

  public final boolean created;
  public final Store store;

  private StoreLegacySync(boolean created, Store store) {
    this.created = created;
    this.store = store;
  }

  public static StoreLegacySync created(Store source) {
    return new StoreLegacySync(true, copy(source));
  }

  public static StoreLegacySync updated(Store source) {
    return new StoreLegacySync(false, copy(source));
  }

  private static Store copy(Store source) {
    Store copy = new Store();
    copy.id = source.id;
    copy.name = source.name;
    copy.quantityProductsInStock = source.quantityProductsInStock;
    return copy;
  }
}
