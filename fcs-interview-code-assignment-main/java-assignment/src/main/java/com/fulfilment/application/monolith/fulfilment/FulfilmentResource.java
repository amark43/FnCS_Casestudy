package com.fulfilment.application.monolith.fulfilment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("fulfilment")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class FulfilmentResource {

  @Inject FulfilmentUseCase fulfilmentUseCase;

  @GET
  public List<FulfilmentLink> list() {
    return fulfilmentUseCase.list();
  }

  @POST
  @Transactional
  public Response associate(FulfilmentLink request) {
    FulfilmentLink created = fulfilmentUseCase.associate(request);
    return Response.status(201).entity(created).build();
  }

  @DELETE
  @Path("{id}")
  @Transactional
  public Response remove(Long id) {
    fulfilmentUseCase.remove(id);
    return Response.status(204).build();
  }
}
