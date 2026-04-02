package com.scalar.dl.ledger.error;

import com.scalar.dl.ledger.service.StatusCode;

public enum LedgerError implements ScalarDlError {

  //
  // Errors for INVALID_HASH(300)
  //
  VALIDATION_FAILED_FOR_HASH(
      StatusCode.INVALID_HASH,
      "001",
      "Validation failed for the hash.",
      "",
      "Data or program tampering, or a software bug, may have occurred. Contact your system administrator to check for any signs of malicious activity."),

  //
  // Errors for INVALID_PREV_HASH(301)
  //
  VALIDATION_FAILED_FOR_PREV_HASH(
      StatusCode.INVALID_PREV_HASH,
      "001",
      "Validation failed for the previous hash.",
      "",
      "Data or program tampering, or a software bug, may have occurred. Contact your system administrator to check for any signs of malicious activity."),

  //
  // Errors for INVALID_CONTRACT(302)
  //
  VALIDATION_FAILED_FOR_CONTRACT(
      StatusCode.INVALID_CONTRACT,
      "001",
      "Validation failed for the contract.",
      "",
      "Data or program tampering, or a software bug, may have occurred. Contact your system administrator to check for any signs of malicious activity."),

  //
  // Errors for INVALID_OUTPUT(303)
  //
  VALIDATION_FAILED_FOR_OUTPUT(
      StatusCode.INVALID_OUTPUT,
      "001",
      "Validation failed for the output. Recomputed: %s; Stored: %s",
      "",
      "Data or program tampering, or a software bug, may have occurred. Contact your system administrator to check for any signs of malicious activity."),

  //
  // Errors for INVALID_NONCE(304)
  //
  VALIDATION_FAILED_FOR_NONCE(
      StatusCode.INVALID_NONCE,
      "001",
      "Validation failed for nonce. %s contains the nonce '%s' more than once.",
      "",
      "Data or program tampering, or a software bug, may have occurred. Contact your system administrator to check for any signs of malicious activity."),

  //
  // Errors for INCONSISTENT_STATES(305)
  //
  INCONSISTENT_ASSET_METADATA(
      StatusCode.INCONSISTENT_STATES,
      "001",
      "The specified asset and the asset metadata are inconsistent.",
      "",
      "Data or program tampering, or a software bug, may have occurred. Contact your system administrator to check for any signs of malicious activity."),
  INCONSISTENT_INPUT_DEPENDENCIES(
      StatusCode.INCONSISTENT_STATES,
      "002",
      "The asset specified by input dependencies is not found.",
      "",
      "Data or program tampering, or a software bug, may have occurred. Contact your system administrator to check for any signs of malicious activity."),

  //
  // Errors for INVALID_REQUEST(407)
  //
  CONTRACT_IS_NOT_ALLOWED_TO_BE_EXECUTED(
      StatusCode.INVALID_REQUEST,
      "001",
      "The specified contract class is not allowed to be executed.",
      "",
      "Verify the contract binary name and ensure it is listed in the configuration file specified by scalar.dl.ledger.executable_contracts."),
  INVALID_AUDITOR_CONFIGURATION(
      StatusCode.INVALID_REQUEST,
      "002",
      "A configuration mismatch is detected. Check the Auditor setting in the client or Ledger.",
      "",
      "Verify that the Auditor settings in the client and Ledger configurations are consistent."),
  AUDITOR_SIGNATURE_REQUIRED(
      StatusCode.INVALID_REQUEST,
      "003",
      "The Auditor signature must be included in the request when Auditor is enabled.",
      "",
      "Verify that the Auditor settings in the client and Ledger configurations are consistent."),
  AUDITOR_NOT_CONFIGURED(
      StatusCode.INVALID_REQUEST,
      "004",
      "%s must be enabled to make auditing work.",
      "",
      "Verify that the Auditor settings in the client and Ledger configurations are consistent."),
  FUNCTION_REGISTRATION_NOT_ALLOWED(
      StatusCode.INVALID_REQUEST,
      "005",
      "Registering a function via a non-privileged port is not allowed. Use a privileged port or enable %s.",
      "",
      "Use a privileged port for function registration, or enable the appropriate configuration property to allow function registration from non-privileged ports."),
  FUNCTION_OVERWRITE_NOT_ALLOWED(
      StatusCode.INVALID_REQUEST,
      "006",
      "Overwriting an existing function is not allowed. Enable %s to allow overwriting.",
      "",
      "Enable the function overwrite configuration property to allow overwriting existing functions."),

  //
  // Errors for ASSET_NOT_FOUND(409)
  //
  ASSET_NOT_FOUND(
      StatusCode.ASSET_NOT_FOUND,
      "001",
      "The specified asset is not found.",
      "",
      "Verify the asset ID and namespace are correct and the asset has been created."),

  //
  // Errors for INVALID_FUNCTION(412)
  //
  FUNCTION_IS_NOT_ALLOWED_TO_ACCESS_SPECIFIED_NAMESPACE(
      StatusCode.INVALID_FUNCTION,
      "001",
      "The function is not allowed to access the specified namespace.",
      "",
      "Functions cannot access system namespaces or namespaces with reserved prefixes. Disallowed namespaces: system, system_schema, system_auth, system_distributed, system_traces, coordinator. Disallowed namespace prefixes: scalar, auditor. Use a different namespace for your function operations."),
  OPERATION_FAILED_DUE_TO_ILLEGAL_ARGUMENT(
      StatusCode.INVALID_FUNCTION,
      "002",
      "The database operation in the function failed. Details: %s",
      "",
      "Verify that the arguments passed to the database operation are valid and correct."),

  //
  // Errors for INVALID_ARGUMENT(414)
  //
  CONFIG_CIPHER_KEY_REQUIRED_FOR_HMAC(
      StatusCode.INVALID_ARGUMENT,
      "001",
      "%s must be set if HMAC authentication is used.",
      "",
      "Set the cipher key configuration property for HMAC authentication."),
  CONFIG_PROOF_MUST_BE_ENABLED(
      StatusCode.INVALID_ARGUMENT,
      "002",
      "%s must be set to true if Auditor is enabled.",
      "",
      "Set the proof configuration property to true when Auditor is enabled."),
  CONFIG_INVALID_AUTHENTICATION_SETTING_BETWEEN_LEDGER_AUDITOR(
      StatusCode.INVALID_ARGUMENT,
      "003",
      "Authentication between Ledger and Auditor is not correctly configured. Set a private key with %s or %s if you use digital signature authentication with Auditor enabled.",
      "",
      "Set the private key configuration property for digital signature authentication between Ledger and Auditor."),
  CONFIG_INVALID_AUTHENTICATION_SETTING_BETWEEN_LEDGER_AUDITOR_HMAC(
      StatusCode.INVALID_ARGUMENT,
      "004",
      "Authentication between Ledger and Auditor is not correctly configured. Set %s if you use HMAC authentication with Auditor enabled.",
      "",
      "Set the required configuration property for HMAC authentication between Ledger and Auditor."),
  CONFIG_PRIVATE_KEY_PEM_OR_PATH_REQUIRED_FOR_PROOF_ENABLED(
      StatusCode.INVALID_ARGUMENT,
      "005",
      "Either %s or %s must be set if proof is enabled.",
      "",
      "Set either the private key PEM or path configuration property when proof is enabled."),
  CONFIG_TX_STATE_MANAGEMENT_MUST_BE_ENABLED_FOR_JDBC_TRANSACTION(
      StatusCode.INVALID_ARGUMENT,
      "006",
      "%s must be set to true when using the JDBC transaction manager in the Auditor mode.",
      "",
      "Set the transaction state management configuration property to true when using JDBC transaction manager in Auditor mode."),
  CONFIG_TX_STATE_MANAGEMENT_MUST_BE_DISABLED_FOR_CONSENSUS_COMMIT(
      StatusCode.INVALID_ARGUMENT,
      "007",
      "%s must be disabled when using the Consensus Commit transaction manager for performance reasons.",
      "",
      "Set the transaction state management configuration property to false when using Consensus Commit transaction manager."),
  CONFIG_GROUP_COMMIT_MUST_BE_DISABLED(
      StatusCode.INVALID_ARGUMENT,
      "008",
      "%s must be disabled because group commit is not supported.",
      "",
      "Set the group commit configuration property to false as it is not supported."),

  //
  // Errors for DATABASE_ERROR(500)
  //
  BINDING_FUNCTION_FAILED(
      StatusCode.DATABASE_ERROR,
      "001",
      "Binding the function failed. Details: %s",
      "",
      "Check the error details in the logs and verify your database configuration and connection."),
  UNBINDING_FUNCTION_FAILED(
      StatusCode.DATABASE_ERROR,
      "002",
      "Unbinding the function failed. Details: %s",
      "",
      "Check the error details in the logs and verify your database configuration and connection."),
  GETTING_FUNCTION_FAILED(
      StatusCode.DATABASE_ERROR,
      "003",
      "Getting the function failed. Details: %s",
      "",
      "Check the error details in the logs and verify your database configuration and connection."),
  STARTING_TRANSACTION_FAILED(
      StatusCode.DATABASE_ERROR,
      "004",
      "Starting a transaction failed. Details: %s",
      "",
      "Check the error details in the logs and verify your database configuration and connection."),
  GETTING_TRANSACTION_STATE_FAILED(
      StatusCode.DATABASE_ERROR,
      "005",
      "Getting the transaction state failed. Details: %s",
      "",
      "Check the error details in the logs and verify your database configuration and connection."),
  PUTTING_OR_COMMITTING_FAILED(
      StatusCode.DATABASE_ERROR,
      "006",
      "Putting or committing asset records failed. Details: %s",
      "",
      "Check the error details in the logs and verify your database configuration and connection."),
  ABORTING_TRANSACTION_FAILED(
      StatusCode.DATABASE_ERROR,
      "007",
      "Aborting the transaction failed. Details: %s",
      "",
      "Check the error details in the logs and verify your database configuration and connection."),
  RETRIEVING_ASSET_FAILED(
      StatusCode.DATABASE_ERROR,
      "008",
      "Retrieving the asset records failed. Details: %s",
      "",
      "Check the error details in the logs and verify your database configuration and connection."),
  RETRIEVING_ASSET_METADATA_FAILED(
      StatusCode.DATABASE_ERROR,
      "009",
      "Retrieving the asset metadata failed. Details: %s",
      "",
      "Check the error details in the logs and verify your database configuration and connection."),
  PUTTING_ASSET_METADATA_FAILED(
      StatusCode.DATABASE_ERROR,
      "010",
      "Putting the asset metadata failed. Details: %s",
      "",
      "Check the error details in the logs and verify your database configuration and connection."),
  OPERATION_FAILED_DUE_TO_DATABASE_ERROR(
      StatusCode.DATABASE_ERROR,
      "011",
      "The database operation in the function failed due to a database error. Details: %s",
      "",
      "Check the error details in the logs and verify your database configuration and connection."),

  //
  // Errors for UNKNOWN_TRANSACTION_STATUS(501)
  //
  UNKNOWN_ASSET_STATUS(
      StatusCode.UNKNOWN_TRANSACTION_STATUS,
      "001",
      "The asset status is unknown. Details: %s",
      "",
      "Check the asset status manually and review the error details in the logs."),

  //
  // Errors for CONFLICT(504)
  //
  TRANSACTION_ALREADY_COMMITTED_OR_ABORTED(
      StatusCode.CONFLICT,
      "001",
      "The transaction state has already been %s.",
      "",
      "Retry the operation."),
  RETRIEVING_ASSET_FAILED_DUE_TO_CONFLICT(
      StatusCode.CONFLICT,
      "002",
      "Retrieving the asset records failed due to a conflict. Details: %s",
      "",
      "Retry the operation."),
  PUTTING_ASSET_FAILED_DUE_TO_CONFLICT(
      StatusCode.CONFLICT,
      "003",
      "Putting the asset records failed due to a conflict. Details: %s",
      "",
      "Retry the operation."),
  COMMITTING_ASSET_FAILED_DUE_TO_CONFLICT(
      StatusCode.CONFLICT,
      "004",
      "Committing the asset records failed due to a conflict. Details: %s",
      "",
      "Retry the operation."),
  RETRIEVING_ASSET_METADATA_FAILED_DUE_TO_CONFLICT(
      StatusCode.CONFLICT,
      "005",
      "Retrieving the asset metadata failed due to a conflict. Details: %s",
      "",
      "Retry the operation."),
  PUTTING_ASSET_METADATA_FAILED_DUE_TO_CONFLICT(
      StatusCode.CONFLICT,
      "006",
      "Putting the asset metadata failed due to a conflict. Details: %s",
      "",
      "Retry the operation."),
  OPERATION_FAILED_DUE_TO_CONFLICT(
      StatusCode.CONFLICT,
      "007",
      "The database operation in the function failed due to a conflict. Details: %s",
      "",
      "Retry the operation."),
  ;

  private static final String COMPONENT_NAME = "DL-LEDGER";

  private final StatusCode statusCode;
  private final String id;
  private final String message;
  private final String cause;
  private final String solution;

  LedgerError(StatusCode statusCode, String id, String message, String cause, String solution) {
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
