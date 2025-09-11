package com.scalar.dl.hashstore.client.error;

import com.scalar.dl.ledger.error.ScalarDlError;
import com.scalar.dl.ledger.service.StatusCode;

public enum HashStoreClientError implements ScalarDlError {

  //
  // Errors for INVALID_ARGUMENT(414)
  //
  PUT_MUST_HAVE_NAMESPACE_AND_TABLE(
      StatusCode.INVALID_ARGUMENT,
      "001",
      "Put operation for the mutable database must have the namespace and table.",
      "",
      ""),
  UNSUPPORTED_DATA_TYPE_FOR_MUTABLE_PUT(
      StatusCode.INVALID_ARGUMENT,
      "002",
      "Unsupported data type is specified in the put operation. Data type: %s",
      "",
      ""),
  ;

  private static final String COMPONENT_NAME = "DL-HASH-STORE";

  private final StatusCode statusCode;
  private final String id;
  private final String message;
  private final String cause;
  private final String solution;

  HashStoreClientError(
      StatusCode statusCode, String id, String message, String cause, String solution) {
    validate(COMPONENT_NAME, statusCode, id, message, cause, solution);

    this.statusCode = statusCode;
    this.id = id;
    this.message = message;
    this.cause = cause;
    this.solution = solution;
  }

  @Override
  public String getComponentName() {
    return COMPONENT_NAME;
  }

  @Override
  public StatusCode getStatusCode() {
    return statusCode;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public String getCause() {
    return cause;
  }

  @Override
  public String getSolution() {
    return solution;
  }
}
