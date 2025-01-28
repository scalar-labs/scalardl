package com.scalar.dl.client.exception;

import com.scalar.dl.ledger.error.ScalarDlError;
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

  public ClientException(ScalarDlError error, Object... args) {
    this(error.buildMessage(args), error.getStatusCode());
  }

  public ClientException(ScalarDlError error, Throwable cause, Object... args) {
    this(error.buildMessage(args), cause, error.getStatusCode());
  }

  public StatusCode getStatusCode() {
    return code;
  }
}
