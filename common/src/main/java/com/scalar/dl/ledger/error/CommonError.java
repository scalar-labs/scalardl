package com.scalar.dl.ledger.error;

import com.scalar.dl.ledger.service.StatusCode;

public enum CommonError implements ScalarDlError {

  //
  // Errors for INVALID_CONTRACT(302)
  //
  INVALID_CONTRACT_ID_FORMAT(
      StatusCode.INVALID_CONTRACT, "001", "The format of the contract ID is invalid.", "", ""),
  CONTRACT_VALIDATION_FAILED(
      StatusCode.INVALID_CONTRACT,
      "002",
      "Contract validation failed. A bug might exist, or tampering might have occurred.",
      "",
      ""),

  //
  // Errors for INCONSISTENT_STATES(305)
  //
  UNEXPECTED_RECORD_VALUE_OBSERVED(
      StatusCode.INCONSISTENT_STATES,
      "001",
      "An unexpected record value is observed. A bug might exist, or tampering might have occurred. Details: %s",
      "",
      ""),

  //
  // Errors for INVALID_SIGNATURE(400)
  //
  SIGNATURE_SIGNING_FAILED(
      StatusCode.INVALID_SIGNATURE, "001", "Signing failed. Details: %s", "", ""),
  SIGNATURE_VALIDATION_FAILED(
      StatusCode.INVALID_SIGNATURE, "002", "Validating signature failed. Details: %s", "", ""),
  REQUEST_SIGNATURE_VALIDATION_FAILED(
      StatusCode.INVALID_SIGNATURE, "003", "The request signature can't be validated.", "", ""),
  PROOF_SIGNATURE_VALIDATION_FAILED(
      StatusCode.INVALID_SIGNATURE, "004", "The proof signature can't be validated.", "", ""),

  //
  // Errors for UNLOADABLE_KEY(401)
  //
  LOADING_KEY_FAILED(
      StatusCode.UNLOADABLE_KEY, "001", "Loading the key failed. Details: %s", "", ""),
  LOADING_CERTIFICATE_FAILED(
      StatusCode.UNLOADABLE_KEY, "002", "Loading the certificate failed. Details: %s", "", ""),
  CREATING_CIPHER_KEY_FAILED(
      StatusCode.UNLOADABLE_KEY, "003", "Creating a cipher key failed. Details: %s", "", ""),
  INVALID_PRIVATE_KEY(StatusCode.UNLOADABLE_KEY, "004", "Invalid private key. File: %s", "", ""),
  INVALID_CERTIFICATE(StatusCode.UNLOADABLE_KEY, "005", "Invalid certificate. File: %s", "", ""),
  READING_PRIVATE_KEY_FAILED(
      StatusCode.UNLOADABLE_KEY,
      "006",
      "Reading the private key failed. File: %s; Details: %s",
      "",
      ""),
  READING_CERTIFICATE_FAILED(
      StatusCode.UNLOADABLE_KEY,
      "007",
      "Reading the certificate failed. File: %s; Details: %s",
      "",
      ""),
  CREATING_KEY_STORE_FAILED(
      StatusCode.UNLOADABLE_KEY, "008", "Creating a key store failed. Details: %s", "", ""),

  //
  // Errors for UNLOADABLE_CONTRACT(402)
  //
  LOADING_CONTRACT_FAILED(
      StatusCode.UNLOADABLE_CONTRACT, "001", "Loading the contract failed. Details: %s", "", ""),

  //
  // Errors for CERTIFICATE_NOT_FOUND(403)
  //
  CERTIFICATE_NOT_FOUND(
      StatusCode.CERTIFICATE_NOT_FOUND, "001", "The specified certificate is not found.", "", ""),

  //
  // Errors for CONTRACT_NOT_FOUND(404)
  //
  CONTRACT_NOT_FOUND(
      StatusCode.CONTRACT_NOT_FOUND, "001", "The specified contract is not found.", "", ""),

  //
  // Errors for CERTIFICATE_ALREADY_REGISTERED(405)
  //
  CERTIFICATE_ALREADY_REGISTERED(
      StatusCode.CERTIFICATE_ALREADY_REGISTERED,
      "001",
      "The specified certificate is already registered.",
      "",
      ""),

  //
  // Errors for CONTRACT_ALREADY_REGISTERED(406)
  //
  CONTRACT_ALREADY_REGISTERED(
      StatusCode.CONTRACT_ALREADY_REGISTERED,
      "001",
      "The specified contract is already registered.",
      "",
      ""),
  DIFFERENT_CLASS_WITH_SAME_NAME(
      StatusCode.CONTRACT_ALREADY_REGISTERED,
      "002",
      "The specified contract binary name has been already registered with a different byte code.",
      "",
      ""),

  //
  // Errors for SECRET_ALREADY_REGISTERED(413)
  //
  SECRET_ALREADY_REGISTERED(
      StatusCode.SECRET_ALREADY_REGISTERED,
      "001",
      "The specified secret is already registered.",
      "",
      ""),

  //
  // Errors for INVALID_ARGUMENT(414)
  //
  CONFIG_UTILS_INVALID_NUMBER_FORMAT(
      StatusCode.INVALID_ARGUMENT,
      "001",
      "The specified value of the property '%s' is not a number. Value: %s",
      "",
      ""),
  CONFIG_UTILS_INVALID_BOOLEAN_FORMAT(
      StatusCode.INVALID_ARGUMENT,
      "002",
      "The specified value of the property '%s' is not a boolean. Value: %s",
      "",
      ""),
  CONFIG_UTILS_READING_FILE_FAILED(
      StatusCode.INVALID_ARGUMENT, "003", "Reading the file failed. File: %s", "", ""),
  LICENSE_CHECKER_CONFIG_LICENSE_KEY_REQUIRED(
      StatusCode.INVALID_ARGUMENT, "004", "Please set your license key to %s.", "", ""),
  LICENSE_CHECKER_CONFIG_CERTIFICATE_PEM_OR_PATH_REQUIRED(
      StatusCode.INVALID_ARGUMENT,
      "005",
      "Please set your certificate for checking the corresponding license key to %s or %s.",
      "",
      ""),
  LICENSE_CHECKER_INVALID_LICENSE_KEY(
      StatusCode.INVALID_ARGUMENT,
      "006",
      "The license key is not for the product '%s'. Please set the correct license key.",
      "",
      ""),
  LICENSE_CHECKER_INVALID_LICENSE_TYPE(
      StatusCode.INVALID_ARGUMENT,
      "007",
      "The license type of the license key must be ENTERPRISE or TRIAL. Please set the correct license key.",
      "",
      ""),
  PORT_MUST_BE_GREATER_THAN_OR_EQUAL_TO_ZERO(
      StatusCode.INVALID_ARGUMENT,
      "008",
      "The port and privileged port must be greater than or equal to zero.",
      "",
      ""),
  PRIVATE_KEY_AND_CERT_REQUIRED(
      StatusCode.INVALID_ARGUMENT, "009", "The private key and certificate are required.", "", ""),
  CERT_VERSION_MUST_BE_GREATER_THAN_ZERO(
      StatusCode.INVALID_ARGUMENT,
      "010",
      "The certificate version must be greater than zero.",
      "",
      ""),
  SECRET_KEY_REQUIRED(
      StatusCode.INVALID_ARGUMENT,
      "011",
      "A secret key is required for HMAC authentication.",
      "",
      ""),
  SECRET_VERSION_MUST_BE_GREATER_THAN_ZERO(
      StatusCode.INVALID_ARGUMENT,
      "012",
      "The secret version for HMAC authentication must be greater than zero.",
      "",
      ""),
  GRPC_DEADLINE_DURATION_MUST_BE_GREATER_THAN_OR_EQUAL_TO_ZERO(
      StatusCode.INVALID_ARGUMENT,
      "013",
      "The grpc deadline duration must be greater than or equal to zero.",
      "",
      ""),
  GRPC_MAX_INBOUND_MESSAGE_SIZE_MUST_BE_GREATER_THAN_OR_EQUAL_TO_ZERO(
      StatusCode.INVALID_ARGUMENT,
      "014",
      "The grpc max inbound message size must be greater than or equal to zero.",
      "",
      ""),
  GRPC_MAX_INBOUND_METADATA_SIZE_MUST_BE_GREATER_THAN_OR_EQUAL_TO_ZERO(
      StatusCode.INVALID_ARGUMENT,
      "015",
      "The grpc max inbound metadata size must be greater than or equal to zero.",
      "",
      ""),
  INVALID_AUTHENTICATION_METHOD(
      StatusCode.INVALID_ARGUMENT,
      "016",
      "The authentication method name is invalid. Name: %s",
      "",
      ""),
  ILLEGAL_ARGUMENT_FORMAT(
      StatusCode.INVALID_ARGUMENT, "017", "The argument format is illegal.", "", ""),
  UNSUPPORTED_DESERIALIZATION_TYPE(
      StatusCode.INVALID_ARGUMENT,
      "018",
      "The deserialization type is not supported. Type: %s",
      "",
      ""),
  INVALID_NAMESPACE_NAME(
      StatusCode.INVALID_ARGUMENT, "019", "The namespace name is invalid. Name: %s", "", ""),
  NAMESPACE_NOT_SUPPORTED_IN_DEPRECATED_CONTRACT(
      StatusCode.INVALID_ARGUMENT,
      "020",
      "Namespace-aware interfaces are not supported in deprecated contracts.",
      "",
      ""),
  RESERVED_NAMESPACE(
      StatusCode.INVALID_ARGUMENT,
      "021",
      "The specified namespace is reserved and cannot be created or deleted. Name: %s",
      "",
      ""),

  //
  // Errors for SECRET_NOT_FOUND(415)
  //
  SECRET_NOT_FOUND(
      StatusCode.SECRET_NOT_FOUND, "001", "The specified secret is not found.", "", ""),

  //
  // Errors for NAMESPACE_ALREADY_EXISTS(416)
  //
  NAMESPACE_ALREADY_EXISTS(
      StatusCode.NAMESPACE_ALREADY_EXISTS,
      "001",
      "The specified namespace already exists.",
      "",
      ""),

  //
  // Errors for NAMESPACE_NOT_FOUND(417)
  //
  NAMESPACE_NOT_FOUND(
      StatusCode.NAMESPACE_NOT_FOUND,
      "001",
      "The specified namespace is not found. Namespace: %s",
      "",
      ""),

  //
  // Errors for DATABASE_ERROR(500)
  //
  BINDING_CERTIFICATE_FAILED(
      StatusCode.DATABASE_ERROR, "001", "Binding the certificate failed. Details: %s", "", ""),
  UNBINDING_CERTIFICATE_FAILED(
      StatusCode.DATABASE_ERROR, "002", "Unbinding the certificate failed. Details: %s", "", ""),
  GETTING_CERTIFICATE_FAILED(
      StatusCode.DATABASE_ERROR, "003", "Getting the certificate failed. Details: %s", "", ""),
  BINDING_SECRET_KEY_FAILED(
      StatusCode.DATABASE_ERROR, "004", "Binding the secret key failed. Details: %s", "", ""),
  UNBINDING_SECRET_KEY_FAILED(
      StatusCode.DATABASE_ERROR, "005", "Unbinding the secret key failed. Details: %s", "", ""),
  GETTING_SECRET_KEY_FAILED(
      StatusCode.DATABASE_ERROR, "006", "Getting the secret key failed. Details: %s", "", ""),
  BINDING_CONTRACT_FAILED(
      StatusCode.DATABASE_ERROR, "007", "Binding the contract failed. Details: %s", "", ""),
  GETTING_CONTRACT_FAILED(
      StatusCode.DATABASE_ERROR, "008", "Getting the contract failed. Details: %s", "", ""),
  SCANNING_CONTRACT_FAILED(
      StatusCode.DATABASE_ERROR, "009", "Scanning the contracts failed. Details: %s", "", ""),
  CREATING_NAMESPACE_TABLE_FAILED(
      StatusCode.DATABASE_ERROR, "010", "Creating the namespace table failed. Details: %s", "", ""),
  CREATING_NAMESPACE_FAILED(
      StatusCode.DATABASE_ERROR, "011", "Creating the namespace failed. Details: %s", "", ""),
  SCANNING_NAMESPACES_FAILED(
      StatusCode.DATABASE_ERROR, "012", "Scanning the namespaces failed. Details: %s", "", ""),

  //
  // Errors for RUNTIME_ERROR(502)
  //
  JSON_SERIALIZATION_FAILED(
      StatusCode.RUNTIME_ERROR,
      "001",
      "Serializing the specified json failed. Details: %s",
      "",
      ""),
  JSON_DESERIALIZATION_FAILED(
      StatusCode.RUNTIME_ERROR,
      "002",
      "Deserializing the specified json string failed. Details: %s",
      "",
      ""),
  REQUIRED_FIELDS_ARE_NOT_GIVEN(
      StatusCode.RUNTIME_ERROR, "003", "The required fields are not specified.", "", ""),
  METADATA_NOT_AVAILABLE(
      StatusCode.RUNTIME_ERROR,
      "004",
      "The metadata is not available since the asset has not been committed yet.",
      "",
      ""),
  INVALID_TRANSACTION_STATE_SPECIFIED(
      StatusCode.RUNTIME_ERROR, "005", "The specified transaction state is invalid.", "", ""),
  UNSUPPORTED_CONTRACT(
      StatusCode.RUNTIME_ERROR, "006", "The contract type or instance is not supported.", "", ""),
  ;

  private static final String COMPONENT_NAME = "DL-COMMON";

  private final StatusCode statusCode;
  private final String id;
  private final String message;
  private final String cause;
  private final String solution;

  CommonError(StatusCode statusCode, String id, String message, String cause, String solution) {
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
