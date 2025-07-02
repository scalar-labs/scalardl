package com.scalar.dl.tablestore.client.error;

import com.scalar.dl.ledger.error.ScalarDlError;
import com.scalar.dl.ledger.service.StatusCode;

public enum TableStoreClientError implements ScalarDlError {

  //
  // Errors for INVALID_ARGUMENT(414)
  //
  SYNTAX_ERROR_IN_PARTIQL_PARSER(
      StatusCode.INVALID_ARGUMENT,
      "001",
      "Syntax error. Line=%d, Offset=%d, Length=%d, Code=%s",
      "",
      ""),
  SYNTAX_ERROR_INVALID_PRIMARY_KEY_SPECIFICATION(
      StatusCode.INVALID_ARGUMENT,
      "002",
      "Syntax error. The primary key column must be specified only once in a table.",
      "",
      ""),
  SYNTAX_ERROR_INVALID_COLUMN_CONSTRAINTS(
      StatusCode.INVALID_ARGUMENT,
      "003",
      "Syntax error. The specified column constraint is invalid.",
      "",
      ""),
  SYNTAX_ERROR_INVALID_DATA_TYPE(
      StatusCode.INVALID_ARGUMENT,
      "004",
      "Syntax error. The specified data type is invalid.",
      "",
      ""),
  SYNTAX_ERROR_INVALID_INSERT_STATEMENT(
      StatusCode.INVALID_ARGUMENT,
      "005",
      "Syntax error. The specified insert statement is invalid.",
      "",
      ""),
  SYNTAX_ERROR_INVALID_STATEMENT(
      StatusCode.INVALID_ARGUMENT,
      "006",
      "Syntax error. The specified statement is invalid.",
      "",
      ""),
  SYNTAX_ERROR_INVALID_EXPRESSION(
      StatusCode.INVALID_ARGUMENT,
      "007",
      "Syntax error. The specified expression is invalid. Expression: %s",
      "",
      ""),
  SYNTAX_ERROR_INVALID_LITERAL(
      StatusCode.INVALID_ARGUMENT,
      "008",
      "Syntax error. The specified literal is invalid. Literal: %s",
      "",
      ""),
  ;

  private static final String COMPONENT_NAME = "DL-TSC";

  private final StatusCode statusCode;
  private final String id;
  private final String message;
  private final String cause;
  private final String solution;

  TableStoreClientError(
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
