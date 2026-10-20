package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ProductEndpointTest {

  @Test
  public void testCrudProduct() {
    final String path = "product";

    // List all, should have all 3 products the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));

    // Delete the TONSTAD:
    given().when().delete(path + "/1").then().statusCode(204);

    // List all, TONSTAD should be missing now:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(not(containsString("TONSTAD")), containsString("KALLAX"), containsString("BESTÅ"));
  }

  @Test
  public void createsUpdatesAndRejectsInvalidProducts() {
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(
                "{\"name\":\"LACK\",\"description\":\"table\",\"price\":12.50,\"stock\":4}")
            .when()
            .post(path())
            .then()
            .statusCode(201)
            .body(containsString("LACK"))
            .extract()
            .path("id")
            .toString();

    given().when().get(path() + "/" + id).then().statusCode(200).body(containsString("LACK"));

    given()
        .contentType(ContentType.JSON)
        .body(
            "{\"name\":\"LACK-UPDATED\",\"description\":\"side table\",\"price\":15.00,\"stock\":6}")
        .when()
        .put(path() + "/" + id)
        .then()
        .statusCode(200)
        .body(containsString("LACK-UPDATED"));

    given()
        .contentType(ContentType.JSON)
        .body("{\"description\":\"missing name\"}")
        .when()
        .put(path() + "/" + id)
        .then()
        .statusCode(422);

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"MISSING-PRODUCT\"}")
        .when()
        .put(path() + "/9999")
        .then()
        .statusCode(404);
    given().when().get(path() + "/9999").then().statusCode(404);
    given().when().delete(path() + "/9999").then().statusCode(404);

    given()
        .contentType(ContentType.JSON)
        .body("{\"id\":50,\"name\":\"WITH-ID\"}")
        .when()
        .post(path())
        .then()
        .statusCode(422);
  }

  private String path() {
    return "product";
  }
}
