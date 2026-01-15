package com.scalar.dl.ledger.error;

import com.scalar.dl.ledger.service.StatusCode;

public enum CommonLedgerError implements ScalarDlError {

  //
  // Errors for INVALID_SIGNATURE(400)
  //
  REQUEST_SIGNATURE_VALIDATION_FAILED(
      StatusCode.INVALID_SIGNATURE, "001", "The request signature can't be validated.", "", ""),
  AUDITOR_SIGNATURE_VALIDATION_FAILED(
      StatusCode.INVALID_SIGNATURE,
      "002",
      "The request signature from Auditor can't be validated.",
      "",
      ""),

  //
  // Errors for FUNCTION_NOT_FOUND(410)
  //
  FUNCTION_NOT_FOUND(
      StatusCode.FUNCTION_NOT_FOUND, "001", "The specified function is not found.", "", ""),

  //
  // Errors for UNLOADABLE_FUNCTION(411)
  //
  LOADING_FUNCTION_FAILED(
      StatusCode.UNLOADABLE_FUNCTION, "001", "Loading the function failed. Details: %s", "", ""),

  //
  // Errors for RUNTIME_ERROR(502)
  //
  UNSUPPORTED_FUNCTION(
      StatusCode.RUNTIME_ERROR, "001", "The function type or instance is not supported.", "", ""),
  ;

  private static final String COMPONENT_NAME = "DL-LEDGER";

  private final StatusCode statusCode;
  private final String id;
  private final String message;
  private final String cause;
  private final String solution;

  CommonLedgerError(
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
