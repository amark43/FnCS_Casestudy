package com.fulfilment.application.monolith.fulfilment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class FulfilmentEndpointTest {

  @Test
  public void associatesListsAndDeletesWithinTheBusinessLimits() {
    long product = createProduct("FULFIL-TABLE");
    long secondProduct = createProduct("FULFIL-CHAIR");
    long thirdProduct = createProduct("FULFIL-LAMP");
    long fourthWarehouse = createWarehouse("MWH.FULFIL", "VETSBY-001");

    String first =
        given()
            .contentType(ContentType.JSON)
            .body(body(1, product, 2))
            .when()
            .post("fulfilment")
            .then()
            .statusCode(201)
            .body("storeId", equalTo(1))
            .body("productId", equalTo((int) product))
            .body("warehouseId", equalTo(2))
            .extract()
            .path("id")
            .toString();

    given()
        .contentType(ContentType.JSON)
        .body(body(1, product, 2))
        .when()
        .post("fulfilment")
        .then()
        .statusCode(400)
        .body(containsString("already fulfilled"));

    given()
        .contentType(ContentType.JSON)
        .body(body(1, product, 3))
        .when()
        .post("fulfilment")
        .then()
        .statusCode(201);

    given()
        .contentType(ContentType.JSON)
        .body(body(1, product, fourthWarehouse))
        .when()
        .post("fulfilment")
        .then()
        .statusCode(400)
        .body(containsString("at most 2 warehouses per store"));

    given()
        .contentType(ContentType.JSON)
        .body(body(1, secondProduct, 1))
        .when()
        .post("fulfilment")
        .then()
        .statusCode(201);

    given()
        .contentType(ContentType.JSON)
        .body(body(1, thirdProduct, fourthWarehouse))
        .when()
        .post("fulfilment")
        .then()
        .statusCode(400)
        .body(containsString("at most 3 warehouses"));

    given().when().get("fulfilment").then().statusCode(200).body(containsString("\"storeId\""));

    given()
        .contentType(ContentType.JSON)
        .body(body(1, 99999, 2))
        .when()
        .post("fulfilment")
        .then()
        .statusCode(404)
        .body(containsString("Product does not exist"));

    given()
        .contentType(ContentType.JSON)
        .body(body(99, product, 2))
        .when()
        .post("fulfilment")
        .then()
        .statusCode(404)
        .body(containsString("Store does not exist"));

    given()
        .contentType(ContentType.JSON)
        .body("{\"storeId\":1}")
        .when()
        .post("fulfilment")
        .then()
        .statusCode(400)
        .body(containsString("required"));

    given().when().delete("fulfilment/" + first).then().statusCode(204);
    given().when().delete("fulfilment/" + first).then().statusCode(404);
    given().when().delete("fulfilment/99999").then().statusCode(404);
  }

  @Test
  public void rejectsAnArchivedWarehouseAndASixthProductType() {
    long archived = createWarehouse("MWH.FULFIL-ARCH", "ZWOLLE-002");
    given().when().delete("warehouse/" + archived).then().statusCode(204);

    long product = createProduct("FULFIL-ARCH-PRODUCT");
    given()
        .contentType(ContentType.JSON)
        .body(body(2, product, archived))
        .when()
        .post("fulfilment")
        .then()
        .statusCode(404)
        .body(containsString("Warehouse does not exist"));

    long warehouse = createWarehouse("MWH.FULFIL-FULL", "AMSTERDAM-002");
    long firstType = 0;
    for (int index = 1; index <= 5; index++) {
      long type = createProduct("FULFIL-TYPE-" + index);
      if (index == 1) {
        firstType = type;
      }
      given()
          .contentType(ContentType.JSON)
          .body(body(2, type, warehouse))
          .when()
          .post("fulfilment")
          .then()
          .statusCode(201);
    }

    long sixth = createProduct("FULFIL-TYPE-6");
    given()
        .contentType(ContentType.JSON)
        .body(body(2, sixth, warehouse))
        .when()
        .post("fulfilment")
        .then()
        .statusCode(400)
        .body(containsString("at most 5 product types"));

    given()
        .contentType(ContentType.JSON)
        .body(body(3, firstType, warehouse))
        .when()
        .post("fulfilment")
        .then()
        .statusCode(201);
  }

  private long createProduct(String name) {
    Number id =
        given()
            .contentType(ContentType.JSON)
            .body("{\"name\":\"" + name + "\",\"stock\":1}")
            .when()
            .post("product")
            .then()
            .statusCode(201)
            .extract()
            .path("id");
    return id.longValue();
  }

  private long createWarehouse(String code, String location) {
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(
                "{\"businessUnitCode\":\""
                    + code
                    + "\",\"location\":\""
                    + location
                    + "\",\"capacity\":10,\"stock\":1}")
            .when()
            .post("warehouse")
            .then()
            .statusCode(201)
            .extract()
            .path("id");
    return Long.parseLong(id);
  }

  private String body(long storeId, long productId, long warehouseId) {
    return "{\"storeId\":" + storeId + ",\"productId\":" + productId + ",\"warehouseId\":" + warehouseId + "}";
  }
}
