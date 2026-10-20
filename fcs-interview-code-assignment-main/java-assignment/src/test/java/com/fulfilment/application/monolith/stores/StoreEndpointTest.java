package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class StoreEndpointTest {

  @InjectMock LegacyStoreManagerGateway legacyStoreManagerGateway;

  @Inject TransactionManager transactionManager;

  @Test
  public void listsAndReadsStores() {
    given().when().get("store").then().statusCode(200).body(containsString("Zwolle Store"));

    given().when().get("store/1").then().statusCode(200).body(containsString("Zwolle Store"));

    given()
        .when()
        .get("store/9999")
        .then()
        .statusCode(404)
        .body(containsString("does not exist"));
  }

  @Test
  public void createPersistsTheStoreAndNotifiesLegacyOnlyAfterCommit() {
    doAnswer(
            invocation -> {
              assertNotEquals(Status.STATUS_ACTIVE, transactionManager.getStatus());
              return null;
            })
        .when(legacyStoreManagerGateway)
        .createStoreOnLegacySystem(any());

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"STORE-CREATED\",\"quantityProductsInStock\":4}")
        .when()
        .post("store")
        .then()
        .statusCode(201)
        .body(containsString("STORE-CREATED"));

    verify(legacyStoreManagerGateway)
        .createStoreOnLegacySystem(
            argThat(
                store ->
                    "STORE-CREATED".equals(store.name) && store.quantityProductsInStock == 4));

    given().when().get("store").then().body(containsString("STORE-CREATED"));
  }

  @Test
  public void rejectsCreateWhenIdIsProvidedAndDoesNotCallLegacy() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"id\":80,\"name\":\"STORE-WITH-ID\",\"quantityProductsInStock\":1}")
        .when()
        .post("store")
        .then()
        .statusCode(422);

    verify(legacyStoreManagerGateway, never()).createStoreOnLegacySystem(any());
  }

  @Test
  public void updatesTheStoreAndNotifiesLegacyOnlyAfterCommit() {
    String createdId = createStore("STORE-BEFORE-UPDATE", 2);

    doAnswer(
            invocation -> {
              assertNotEquals(Status.STATUS_ACTIVE, transactionManager.getStatus());
              return null;
            })
        .when(legacyStoreManagerGateway)
        .updateStoreOnLegacySystem(any());

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"STORE-AFTER-UPDATE\",\"quantityProductsInStock\":9}")
        .when()
        .put("store/" + createdId)
        .then()
        .statusCode(200)
        .body(containsString("STORE-AFTER-UPDATE"));

    verify(legacyStoreManagerGateway)
        .updateStoreOnLegacySystem(
            argThat(
                store ->
                    "STORE-AFTER-UPDATE".equals(store.name)
                        && store.quantityProductsInStock == 9));

    given()
        .contentType(ContentType.JSON)
        .body("{\"quantityProductsInStock\":1}")
        .when()
        .put("store/" + createdId)
        .then()
        .statusCode(422);

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"MISSING-STORE\"}")
        .when()
        .put("store/9999")
        .then()
        .statusCode(404);
  }

  @Test
  public void patchesTheStoreAfterCommitAndDeletesIt() {
    String createdId = createStore("STORE-BEFORE-PATCH", 3);

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"STORE-AFTER-PATCH\",\"quantityProductsInStock\":11}")
        .when()
        .patch("store/" + createdId)
        .then()
        .statusCode(200)
        .body(containsString("STORE-AFTER-PATCH"));

    verify(legacyStoreManagerGateway)
        .updateStoreOnLegacySystem(
            argThat(store -> "STORE-AFTER-PATCH".equals(store.name)));

    given()
        .contentType(ContentType.JSON)
        .body("{\"quantityProductsInStock\":1}")
        .when()
        .patch("store/" + createdId)
        .then()
        .statusCode(422);

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"MISSING-STORE\"}")
        .when()
        .patch("store/9999")
        .then()
        .statusCode(404);

    given().when().delete("store/" + createdId).then().statusCode(204);
    given().when().delete("store/9999").then().statusCode(404);
    given().when().get("store").then().body(not(containsString("STORE-AFTER-PATCH")));
  }

  @Test
  public void keepsAZeroStockUnchangedOnPatch() {
    String createdId = createStore("STORE-ZERO-STOCK", 0);

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"STORE-ZERO-STOCK-PATCHED\",\"quantityProductsInStock\":15}")
        .when()
        .patch("store/" + createdId)
        .then()
        .statusCode(200)
        .body(containsString("\"quantityProductsInStock\":0"));
  }

  private String createStore(String name, int stock) {
    return given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"" + name + "\",\"quantityProductsInStock\":" + stock + "}")
        .when()
        .post("store")
        .then()
        .statusCode(201)
        .extract()
        .path("id")
        .toString();
  }
}
