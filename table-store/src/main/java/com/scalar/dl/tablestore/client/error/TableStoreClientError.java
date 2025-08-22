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
  SYNTAX_ERROR_INVALID_UPDATE_TARGET(
      StatusCode.INVALID_ARGUMENT,
      "009",
      "Syntax error. The specified format of the update target column is invalid.",
      "",
      ""),
  SYNTAX_ERROR_INVALID_TABLE(
      StatusCode.INVALID_ARGUMENT,
      "010",
      "Syntax error. The specified table is invalid. Table: %s",
      "",
      ""),
  SYNTAX_ERROR_INVALID_COLUMN(
      StatusCode.INVALID_ARGUMENT,
      "011",
      "Syntax error. The specified column is invalid. Column: %s",
      "",
      ""),
  SYNTAX_ERROR_INVALID_CONDITION(
      StatusCode.INVALID_ARGUMENT,
      "012",
      "Syntax error. The specified condition is invalid. Condition: %s",
      "",
      ""),
  SYNTAX_ERROR_INVALID_JOIN_CONDITION(
      StatusCode.INVALID_ARGUMENT,
      "013",
      "Syntax error. The specified join condition is invalid. Condition: %s",
      "",
      ""),
  SYNTAX_ERROR_INVALID_JOIN_TYPE(
      StatusCode.INVALID_ARGUMENT,
      "014",
      "Syntax error. The specified join type is invalid.",
      "",
      ""),
  SYNTAX_ERROR_INVALID_PROJECTION(
      StatusCode.INVALID_ARGUMENT,
      "015",
      "Syntax error. The specified projection is invalid. Projection: %s",
      "",
      ""),
  SYNTAX_ERROR_INVALID_LIMIT_CLAUSE(
      StatusCode.INVALID_ARGUMENT,
      "016",
      "Syntax error. The specified limit clause is invalid.",
      "",
      ""),
  SYNTAX_ERROR_INVALID_SELECT_STATEMENT(
      StatusCode.INVALID_ARGUMENT,
      "017",
      "Syntax error. The specified select statement is invalid.",
      "",
      ""),
  SYNTAX_ERROR_WITH_NOT_SUPPORTED(
      StatusCode.INVALID_ARGUMENT,
      "018",
      "Syntax error. The specified 'WITH' clause is not supported.",
      "",
      ""),
  SYNTAX_ERROR_ORDER_BY_NOT_SUPPORTED(
      StatusCode.INVALID_ARGUMENT,
      "019",
      "Syntax error. The specified 'ORDER BY' clause is not supported.",
      "",
      ""),
  SYNTAX_ERROR_OFFSET_NOT_SUPPORTED(
      StatusCode.INVALID_ARGUMENT,
      "020",
      "Syntax error. The specified 'OFFSET' clause is not supported.",
      "",
      ""),
  SYNTAX_ERROR_LET_NOT_SUPPORTED(
      StatusCode.INVALID_ARGUMENT,
      "021",
      "Syntax error. The specified 'LET' clause is not supported.",
      "",
      ""),
  SYNTAX_ERROR_EXCLUDE_NOT_SUPPORTED(
      StatusCode.INVALID_ARGUMENT,
      "022",
      "Syntax error. The specified 'EXCLUDE' clause is not supported.",
      "",
      ""),
  SYNTAX_ERROR_GROUP_BY_NOT_SUPPORTED(
      StatusCode.INVALID_ARGUMENT,
      "023",
      "Syntax error. The specified 'GROUP BY' clause is not supported.",
      "",
      ""),
  SYNTAX_ERROR_HAVING_NOT_SUPPORTED(
      StatusCode.INVALID_ARGUMENT,
      "024",
      "Syntax error. The specified 'HAVING' clause is not supported.",
      "",
      ""),
  SYNTAX_ERROR_CROSS_AND_IMPLICIT_JOIN_NOT_SUPPORTED(
      StatusCode.INVALID_ARGUMENT,
      "025",
      "Syntax error. The cross join and implicit join using comma-separated tables are not supported. Use a 'JOIN' clause instead.",
      "",
      ""),
  SYNTAX_ERROR_SET_QUANTIFIER_NOT_SUPPORTED(
      StatusCode.INVALID_ARGUMENT,
      "026",
      "Syntax error. The specified set quantifier is not supported.",
      "",
      ""),
  LIMIT_CLAUSE_NOT_SUPPORTED(
      StatusCode.INVALID_ARGUMENT,
      "027",
      "The limit clause is not supported except in the history query.",
      "",
      ""),
  MULTIPLE_STATEMENTS_NOT_SUPPORTED(
      StatusCode.INVALID_ARGUMENT, "028", "Multiple statements are not supported.", "", ""),
  ;

  private static final String COMPONENT_NAME = "DL-TABLE-STORE";

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
