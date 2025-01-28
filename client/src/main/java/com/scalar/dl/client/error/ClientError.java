package com.scalar.dl.client.error;

import com.scalar.dl.ledger.error.ScalarDlError;
import com.scalar.dl.ledger.service.StatusCode;

public enum ClientError implements ScalarDlError {

  //
  // Errors for INCONSISTENT_STATES(305)
  //
  INCONSISTENT_RESULTS(
      StatusCode.INCONSISTENT_STATES,
      "001",
      "The results from Ledger and Auditor don't match.",
      "",
      ""),

  //
  // Errors for INVALID_ARGUMENT(414)
  //
  OPTION_ASSET_ID_IS_MALFORMED(
      StatusCode.INVALID_ARGUMENT,
      "001",
      "The specified option --asset-id is malformed. The format should be \"[assetId]\" or \"[assetId],[startAge],[endAge]\".",
      "",
      ""),
  OPTION_ASSET_ID_CONTAINS_INVALID_INTEGER(
      StatusCode.INVALID_ARGUMENT,
      "002",
      "The specified option --asset-id contains an invalid integer.",
      "",
      ""),
  CONFIG_INVALID_AUTHENTICATION_METHOD_FOR_CLIENT_MODE(
      StatusCode.INVALID_ARGUMENT,
      "003",
      "The authentication method for the client mode must be either digital-signature or hmac.",
      "",
      ""),
  CONFIG_INVALID_AUTHENTICATION_METHOD_FOR_INTERMEDIARY_MODE(
      StatusCode.INVALID_ARGUMENT,
      "004",
      "The authentication method for the intermediary mode must be pass-through.",
      "",
      ""),
  CONFIG_CERT_AND_KEY_REQUIRED_FOR_DIGITAL_SIGNATURE(
      StatusCode.INVALID_ARGUMENT,
      "005",
      "Both the certificate and the private key must be set to use digital signature.",
      "",
      ""),
  CONFIG_SECRET_KEY_REQUIRED_FOR_HMAC(
      StatusCode.INVALID_ARGUMENT,
      "006",
      "The secret key must be set to use HMAC authentication.",
      "",
      ""),
  CONFIG_ENTITY_ID_OR_CERT_HOLDER_ID_REQUIRED(
      StatusCode.INVALID_ARGUMENT, "007", "%s and %s are missing, but either is required.", "", ""),
  CONFIG_DIGITAL_SIGNATURE_AUTHENTICATION_NOT_CONFIGURED(
      StatusCode.INVALID_ARGUMENT,
      "008",
      "Digital signature authentication is not configured.",
      "",
      ""),
  CONFIG_HMAC_AUTHENTICATION_NOT_CONFIGURED(
      StatusCode.INVALID_ARGUMENT, "009", "HMAC authentication is not configured.", "", ""),
  CONFIG_VALIDATE_LEDGER_WITH_AUDITOR_NOT_SUPPORTED_WITH_INTERMEDIARY_MODE(
      StatusCode.INVALID_ARGUMENT,
      "010",
      "validateLedger with Auditor is not supported in the intermediary mode. Please execute the ValidateLedger contract to validate assets.",
      "",
      ""),
  CONFIG_WRONG_CLIENT_MODE_SPECIFIED(
      StatusCode.INVALID_ARGUMENT, "011", "The specified client mode is incorrect.", "", ""),
  SERVICE_CONTRACT_ID_CANNOT_BE_NULL(
      StatusCode.INVALID_ARGUMENT, "012", "The contract ID cannot be null.", "", ""),
  SERVICE_CONTRACT_NAME_CANNOT_BE_NULL(
      StatusCode.INVALID_ARGUMENT, "013", "The contract name cannot be null.", "", ""),
  SERVICE_CONTRACT_BYTES_CANNOT_BE_NULL(
      StatusCode.INVALID_ARGUMENT, "014", "The contractBytes cannot be null.", "", ""),
  SERVICE_CONTRACT_ARGUMENT_CANNOT_BE_NULL(
      StatusCode.INVALID_ARGUMENT, "015", "The contractArgument cannot be null.", "", ""),
  SERVICE_CONTRACT_PATH_CANNOT_BE_NULL(
      StatusCode.INVALID_ARGUMENT, "016", "The contractPath cannot be null.", "", ""),
  SERVICE_FUNCTION_ID_CANNOT_BE_NULL(
      StatusCode.INVALID_ARGUMENT, "017", "The function ID cannot be null.", "", ""),
  SERVICE_FUNCTION_NAME_CANNOT_BE_NULL(
      StatusCode.INVALID_ARGUMENT, "018", "The function name cannot be null.", "", ""),
  SERVICE_FUNCTION_BYTES_CANNOT_BE_NULL(
      StatusCode.INVALID_ARGUMENT, "019", "The functionBytes cannot be null.", "", ""),
  SERVICE_FUNCTION_PATH_CANNOT_BE_NULL(
      StatusCode.INVALID_ARGUMENT, "020", "The functionPath cannot be null.", "", ""),
  SERVICE_ASSET_ID_CANNOT_BE_NULL(
      StatusCode.INVALID_ARGUMENT, "021", "The asset ID cannot be null.", "", ""),
  SERVICE_INVALID_ASSET_AGES(
      StatusCode.INVALID_ARGUMENT, "022", "The specified asset ages are invalid.", "", ""),

  //
  // Errors for RUNTIME_ERROR(502)
  //
  READING_FILE_FAILED(
      StatusCode.RUNTIME_ERROR, "001", "Reading the file failed. File: %s; Details: %s", "", ""),
  CONFIGURING_SSL_FAILED(
      StatusCode.RUNTIME_ERROR, "002", "Configuring SSL failed. Details: %s", "", ""),
  SHUTTING_DOWN_CHANNEL_FAILED(
      StatusCode.RUNTIME_ERROR, "003", "Shutting down the channel failed. Details: %s", "", ""),
  PROCESSING_JSON_FAILED(
      StatusCode.RUNTIME_ERROR, "004", "Processing JSON failed. Details: %s", "", ""),
  ;

  private static final String COMPONENT_NAME = "DL-CLIENT";

  private final StatusCode statusCode;
  private final String id;
  private final String message;
  private final String cause;
  private final String solution;

  ClientError(StatusCode statusCode, String id, String message, String cause, String solution) {
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
