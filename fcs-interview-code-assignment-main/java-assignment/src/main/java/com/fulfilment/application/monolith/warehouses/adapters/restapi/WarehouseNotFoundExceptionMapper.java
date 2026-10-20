package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

@Provider
public class WarehouseNotFoundExceptionMapper implements ExceptionMapper<WarehouseNotFoundException> {

  @Override
  public Response toResponse(WarehouseNotFoundException exception) {
    return Response.status(404)
        .type(MediaType.APPLICATION_JSON)
        .entity(Map.of("code", 404, "error", exception.getMessage()))
        .build();
  }
}
