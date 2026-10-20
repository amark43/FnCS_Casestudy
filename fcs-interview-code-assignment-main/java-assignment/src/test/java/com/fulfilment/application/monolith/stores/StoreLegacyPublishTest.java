package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class StoreLegacyPublishTest {

  @Test
  public void publishesTheCommittedStoreToTheLegacyGateway() {
    String id =
        given()
            .contentType(ContentType.JSON)
            .body("{\"name\":\"REAL-LEGACY-STORE\",\"quantityProductsInStock\":8}")
            .when()
            .post("store")
            .then()
            .statusCode(201)
            .body(containsString("REAL-LEGACY-STORE"))
            .extract()
            .path("id")
            .toString();

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"REAL-LEGACY-UPDATED\",\"quantityProductsInStock\":9}")
        .when()
        .put("store/" + id)
        .then()
        .statusCode(200)
        .body(containsString("REAL-LEGACY-UPDATED"));
  }
}
