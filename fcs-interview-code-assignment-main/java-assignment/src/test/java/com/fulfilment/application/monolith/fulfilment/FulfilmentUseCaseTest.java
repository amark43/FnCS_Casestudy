package com.fulfilment.application.monolith.fulfilment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FulfilmentUseCaseTest {

  private InMemoryFulfilmentRepository repository;
  private RecordingCatalog catalog;
  private FulfilmentUseCase useCase;

  @BeforeEach
  public void setUp() {
    repository = new InMemoryFulfilmentRepository();
    catalog = new RecordingCatalog();
    useCase = new FulfilmentUseCase(repository, catalog);
  }

  @Test
  public void associatesAProductWithAWarehouseForAStore() {
    FulfilmentLink created = useCase.associate(link(1L, 10L, 100L));

    assertNotNull(created.id);
    assertEquals(1, useCase.list().size());
  }

  @Test
  public void rejectsMissingIdentifiersAndUnknownParties() {
    assertEquals(400, failure(null).status);
    assertEquals(400, failure(new FulfilmentLink()).status);
    catalog.storeExists = false;
    assertEquals(404, failure(link(1L, 10L, 100L)).status);
    catalog.storeExists = true;
    catalog.productExists = false;
    assertEquals(404, failure(link(1L, 10L, 100L)).status);
    catalog.productExists = true;
    catalog.warehouseActive = false;
    assertEquals(404, failure(link(1L, 10L, 100L)).status);
  }

  @Test
  public void rejectsADuplicateAssociation() {
    useCase.associate(link(1L, 10L, 100L));

    FulfilmentViolation violation = failure(link(1L, 10L, 100L));

    assertEquals(400, violation.status);
    assertEquals(
        "This product is already fulfilled by this warehouse for the store", violation.getMessage());
  }

  @Test
  public void allowsTwoWarehousesForOneProductAtOneStoreAndRejectsAThird() {
    useCase.associate(link(1L, 10L, 100L));
    useCase.associate(link(1L, 10L, 200L));

    FulfilmentViolation violation = failure(link(1L, 10L, 300L));

    assertEquals("A product can be fulfilled by at most 2 warehouses per store", violation.getMessage());
  }

  @Test
  public void limitsAStoreToThreeDistinctWarehouses() {
    useCase.associate(link(1L, 10L, 100L));
    useCase.associate(link(1L, 11L, 200L));
    useCase.associate(link(1L, 12L, 300L));

    FulfilmentViolation violation = failure(link(1L, 13L, 400L));

    assertEquals("A store can be fulfilled by at most 3 warehouses", violation.getMessage());
    FulfilmentLink reuse = useCase.associate(link(1L, 13L, 100L));
    assertEquals(100L, reuse.warehouseId);
  }

  @Test
  public void limitsAWarehouseToFiveProductTypes() {
    for (long productId = 1; productId <= 5; productId++) {
      useCase.associate(link(productId, productId, 100L));
    }

    FulfilmentViolation violation = failure(link(9L, 6L, 100L));

    assertEquals("A warehouse can store at most 5 product types", violation.getMessage());
    FulfilmentLink sameProductAnotherStore = useCase.associate(link(2L, 1L, 100L));
    assertEquals(1L, sameProductAnotherStore.productId);
  }

  @Test
  public void removingAnAssociationFreesTheSlot() {
    FulfilmentLink created = useCase.associate(link(1L, 10L, 100L));
    useCase.associate(link(1L, 10L, 200L));

    useCase.remove(created.id);

    useCase.associate(link(1L, 10L, 300L));
    assertEquals(2, useCase.list().size());
    assertEquals(404, failureOfRemove(999L).status);
    assertEquals(404, failureOfRemove(null).status);
  }

  private FulfilmentViolation failure(FulfilmentLink request) {
    return assertThrows(FulfilmentViolation.class, () -> useCase.associate(request));
  }

  private FulfilmentViolation failureOfRemove(Long id) {
    return assertThrows(FulfilmentViolation.class, () -> useCase.remove(id));
  }

  private FulfilmentLink link(Long storeId, Long productId, Long warehouseId) {
    return new FulfilmentLink(storeId, productId, warehouseId);
  }

  private static final class RecordingCatalog implements FulfilmentCatalog {
    boolean storeExists = true;
    boolean productExists = true;
    boolean warehouseActive = true;

    @Override
    public boolean storeExists(Long storeId) {
      return storeExists;
    }

    @Override
    public boolean productExists(Long productId) {
      return productExists;
    }

    @Override
    public boolean activeWarehouseExists(Long warehouseId) {
      return warehouseActive;
    }
  }

  private static final class InMemoryFulfilmentRepository implements FulfilmentRepository {
    private final List<FulfilmentLink> links = new ArrayList<>();
    private long sequence = 1;

    @Override
    public List<FulfilmentLink> listLinks() {
      return links.stream().map(this::copy).toList();
    }

    @Override
    public FulfilmentLink getById(Long id) {
      return links.stream().filter(link -> id.equals(link.id)).findFirst().map(this::copy).orElse(null);
    }

    @Override
    public FulfilmentLink create(FulfilmentLink link) {
      link.id = sequence++;
      links.add(copy(link));
      return link;
    }

    @Override
    public void delete(Long id) {
      links.removeIf(link -> id.equals(link.id));
    }

    private FulfilmentLink copy(FulfilmentLink source) {
      FulfilmentLink copy = new FulfilmentLink(source.storeId, source.productId, source.warehouseId);
      copy.id = source.id;
      return copy;
    }
  }
}
