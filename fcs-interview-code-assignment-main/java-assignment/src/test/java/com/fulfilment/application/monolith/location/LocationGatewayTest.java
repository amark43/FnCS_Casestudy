package com.fulfilment.application.monolith.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class LocationGatewayTest {

  private final LocationGateway locationGateway = new LocationGateway();

  @Test
  public void testWhenResolveExistingLocationShouldReturn() {
    var location = locationGateway.resolveByIdentifier("ZWOLLE-001");

    assertEquals("ZWOLLE-001", location.identification);
    assertEquals(1, location.maxNumberOfWarehouses);
    assertEquals(40, location.maxCapacity);
  }

  @Test
  public void testWhenResolveUnknownLocationShouldReturnNull() {
    assertNull(locationGateway.resolveByIdentifier("UNKNOWN-001"));
    assertNull(locationGateway.resolveByIdentifier(null));
  }

  @Test
  public void testEverySeedLocationCanBeResolved() {
    String[] identifiers = {
      "ZWOLLE-002",
      "AMSTERDAM-001",
      "AMSTERDAM-002",
      "TILBURG-001",
      "HELMOND-001",
      "EINDHOVEN-001",
      "VETSBY-001"
    };

    for (String identifier : identifiers) {
      assertEquals(identifier, locationGateway.resolveByIdentifier(identifier).identification);
    }
  }
}
