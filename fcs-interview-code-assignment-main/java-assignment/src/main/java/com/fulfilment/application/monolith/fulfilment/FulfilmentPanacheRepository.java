package com.fulfilment.application.monolith.fulfilment;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
@Transactional
public class FulfilmentPanacheRepository
    implements FulfilmentRepository, PanacheRepository<FulfilmentAssignment> {

  @Override
  public List<FulfilmentLink> listLinks() {
    return listAll().stream().map(FulfilmentAssignment::toLink).toList();
  }

  @Override
  public FulfilmentLink getById(Long id) {
    FulfilmentAssignment assignment = findById(id);
    return assignment == null ? null : assignment.toLink();
  }

  @Override
  public FulfilmentLink create(FulfilmentLink link) {
    FulfilmentAssignment assignment = new FulfilmentAssignment();
    assignment.storeId = link.storeId;
    assignment.productId = link.productId;
    assignment.warehouseId = link.warehouseId;
    persistAndFlush(assignment);
    link.id = assignment.id;
    return link;
  }

  @Override
  public void delete(Long id) {
    FulfilmentAssignment assignment = findById(id);
    if (assignment != null) {
      delete(assignment);
    }
  }
}
