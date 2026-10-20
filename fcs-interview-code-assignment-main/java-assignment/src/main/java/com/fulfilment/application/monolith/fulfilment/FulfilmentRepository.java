package com.fulfilment.application.monolith.fulfilment;

import java.util.List;

public interface FulfilmentRepository {

  List<FulfilmentLink> listLinks();

  FulfilmentLink getById(Long id);

  FulfilmentLink create(FulfilmentLink link);

  void delete(Long id);
}
