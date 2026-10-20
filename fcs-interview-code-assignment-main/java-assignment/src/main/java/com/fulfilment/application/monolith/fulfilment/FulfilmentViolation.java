package com.fulfilment.application.monolith.fulfilment;

public class FulfilmentViolation extends RuntimeException {

  public final int status;

  public FulfilmentViolation(int status, String message) {
    super(message);
    this.status = status;
  }
}
