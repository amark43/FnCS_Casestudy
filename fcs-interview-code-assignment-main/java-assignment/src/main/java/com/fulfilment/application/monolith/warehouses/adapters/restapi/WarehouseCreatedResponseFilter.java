package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

/** The generated warehouse contract returns the created resource with HTTP 201. */
@ApplicationScoped
public class WarehouseCreatedResponseFilter {

  @ServerResponseFilter
  public void created(ContainerRequestContext request, ContainerResponseContext response) {
    String path = request.getUriInfo().getPath();
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    if ("POST".equalsIgnoreCase(request.getMethod())
        && "warehouse".equals(path)
        && response.getStatus() == 200) {
      response.setStatus(201);
    }
  }
}
