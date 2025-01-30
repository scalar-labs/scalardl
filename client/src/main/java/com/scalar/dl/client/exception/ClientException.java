package com.scalar.dl.client.exception;

import com.scalar.dl.ledger.service.StatusCode;

public class ClientException extends RuntimeException {
  private final StatusCode code;

  public ClientException(String message, StatusCode code) {
    super(message);
    this.code = code;
  }

  public ClientException(String message, Throwable cause, StatusCode code) {
    super(message, cause);
    this.code = code;
  }

  public StatusCode getStatusCode() {
    return code;
  }
}
