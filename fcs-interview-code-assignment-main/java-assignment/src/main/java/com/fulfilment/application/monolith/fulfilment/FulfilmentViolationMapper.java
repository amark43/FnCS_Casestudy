package com.fulfilment.application.monolith.fulfilment;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

@Provider
public class FulfilmentViolationMapper implements ExceptionMapper<FulfilmentViolation> {

  @Override
  public Response toResponse(FulfilmentViolation exception) {
    return Response.status(exception.status)
        .type(MediaType.APPLICATION_JSON)
        .entity(Map.of("code", exception.status, "error", exception.getMessage()))
        .build();
  }
}
