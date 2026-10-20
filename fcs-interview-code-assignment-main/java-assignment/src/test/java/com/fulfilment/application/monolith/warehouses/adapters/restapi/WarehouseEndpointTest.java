package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WarehouseEndpointTest {

  @Test
  public void listsAndReadsTheSeededWarehouses() {
    given()
        .when()
        .get("warehouse")
        .then()
        .statusCode(200)
        .body(
            containsString("MWH.001"),
            containsString("MWH.012"),
            containsString("MWH.023"),
            containsString("ZWOLLE-001"));

    given()
        .when()
        .get("warehouse/1")
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("ZWOLLE-001"));

    given().when().get("warehouse/9999").then().statusCode(404);
    given().when().get("warehouse/not-a-number").then().statusCode(400);
  }

  @Test
  public void createsGetsAndArchivesAWarehouse() {
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(body("MWH.HEL", "HELMOND-001", 20, 4))
            .when()
            .post("warehouse")
            .then()
            .statusCode(201)
            .body("businessUnitCode", equalTo("MWH.HEL"))
            .body("location", equalTo("HELMOND-001"))
            .body("capacity", equalTo(20))
            .body("stock", equalTo(4))
            .extract()
            .path("id");

    given().when().get("warehouse/" + id).then().statusCode(200).body(containsString("MWH.HEL"));

    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.HEL2", "HELMOND-001", 10, 1))
        .when()
        .post("warehouse")
        .then()
        .statusCode(400)
        .body(containsString("Maximum number of warehouses"));

    given().when().delete("warehouse/" + id).then().statusCode(204);
    given().when().get("warehouse").then().body(not(containsString("MWH.HEL")));
    given().when().get("warehouse/" + id).then().statusCode(200).body(containsString("MWH.HEL"));
    given().when().delete("warehouse/" + id).then().statusCode(404);
    given().when().delete("warehouse/9999").then().statusCode(404);
    given().when().delete("warehouse/bad-id").then().statusCode(400);
  }

  @Test
  public void rejectsInvalidCreateRequests() {
    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.BAD-LOC", "NOWHERE-001", 10, 1))
        .when()
        .post("warehouse")
        .then()
        .statusCode(400)
        .body(containsString("Location is not valid"));

    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.001", "EINDHOVEN-001", 10, 1))
        .when()
        .post("warehouse")
        .then()
        .statusCode(400)
        .body(containsString("already exists"));

    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.STOCK", "VETSBY-001", 10, 12))
        .when()
        .post("warehouse")
        .then()
        .statusCode(400)
        .body(containsString("cannot handle the informed stock"));

    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.CAP", "VETSBY-001", 95, 1))
        .when()
        .post("warehouse")
        .then()
        .statusCode(400)
        .body(containsString("maximum capacity"));

    given()
        .contentType(ContentType.JSON)
        .body("{\"location\":\"VETSBY-001\"}")
        .when()
        .post("warehouse")
        .then()
        .statusCode(400)
        .body(containsString("required"));

    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.FULL", "ZWOLLE-001", 10, 1))
        .when()
        .post("warehouse")
        .then()
        .statusCode(400)
        .body(containsString("Maximum number of warehouses"));
  }

  @Test
  public void rejectsASecondWarehouseWhenCombinedCapacityDoesNotFit() {
    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.Z1", "ZWOLLE-002", 30, 2))
        .when()
        .post("warehouse")
        .then()
        .statusCode(201);

    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.Z2", "ZWOLLE-002", 25, 2))
        .when()
        .post("warehouse")
        .then()
        .statusCode(400)
        .body(containsString("maximum capacity"));
  }

  @Test
  public void replacesTheActiveWarehouseAndKeepsItsStock() {
    String originalId =
        given()
            .contentType(ContentType.JSON)
            .body(body("MWH.REP", "EINDHOVEN-001", 20, 6))
            .when()
            .post("warehouse")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.OTHER", "EINDHOVEN-001", 25, 6))
        .when()
        .post("warehouse/MWH.REP/replacement")
        .then()
        .statusCode(400)
        .body(containsString("must match the warehouse being replaced"));

    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.REP", "EINDHOVEN-001", 25, 5))
        .when()
        .post("warehouse/MWH.REP/replacement")
        .then()
        .statusCode(400)
        .body(containsString("must match the stock"));

    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.REP", "EINDHOVEN-001", 5, 6))
        .when()
        .post("warehouse/MWH.REP/replacement")
        .then()
        .statusCode(400)
        .body(containsString("cannot accommodate"));

    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.REP", "NOWHERE-001", 25, 6))
        .when()
        .post("warehouse/MWH.REP/replacement")
        .then()
        .statusCode(400)
        .body(containsString("Location is not valid"));

    String replacementId =
        given()
            .contentType(ContentType.JSON)
            .body(body("MWH.REP", "EINDHOVEN-001", 28, 6))
            .when()
            .post("warehouse/MWH.REP/replacement")
            .then()
            .statusCode(200)
            .body("businessUnitCode", equalTo("MWH.REP"))
            .body("capacity", equalTo(28))
            .body("stock", equalTo(6))
            .extract()
            .path("id");

    given()
        .when()
        .get("warehouse")
        .then()
        .body(containsString("MWH.REP"), containsString("EINDHOVEN-001"));
    given().when().get("warehouse/" + originalId).then().statusCode(200);
    given()
        .when()
        .get("warehouse/" + replacementId)
        .then()
        .statusCode(200)
        .body(containsString("\"capacity\":28"));

    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.MISSING", "EINDHOVEN-001", 10, 1))
        .when()
        .post("warehouse/MWH.MISSING/replacement")
        .then()
        .statusCode(404);
  }

  @Test
  public void replacementUsesThePathBusinessUnitCodeWhenTheBodyOmitsIt() {
    given()
        .contentType(ContentType.JSON)
        .body(body("MWH.PATH", "AMSTERDAM-002", 15, 3))
        .when()
        .post("warehouse")
        .then()
        .statusCode(201);

    given()
        .contentType(ContentType.JSON)
        .body("{\"location\":\"AMSTERDAM-002\",\"capacity\":18,\"stock\":3}")
        .when()
        .post("warehouse/MWH.PATH/replacement")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("MWH.PATH"))
        .body("capacity", equalTo(18));
  }

  private String body(String code, String location, int capacity, int stock) {
    return "{\"businessUnitCode\":\""
        + code
        + "\",\"location\":\""
        + location
        + "\",\"capacity\":"
        + capacity
        + ",\"stock\":"
        + stock
        + "}";
  }
}
