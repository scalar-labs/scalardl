package com.scalar.dl.ledger.error;

import com.scalar.dl.ledger.service.StatusCode;

public enum CommonError implements ScalarDlError {

  //
  // Errors for INVALID_CONTRACT(302)
  //
  INVALID_CONTRACT_ID_FORMAT(
      StatusCode.INVALID_CONTRACT, "001", "The format of the contract ID is invalid.", "", "Data or program tampering, or a software bug, may have occurred. Contact your system administrator to check for any signs of malicious activity."),
  CONTRACT_VALIDATION_FAILED(
      StatusCode.INVALID_CONTRACT,
      "002",
      "Contract validation failed. A bug might exist, or tampering might have occurred.",
      "",
      "Data or program tampering, or a software bug, may have occurred. Contact your system administrator to check for any signs of malicious activity."),

  //
  // Errors for INCONSISTENT_STATES(305)
  //
  UNEXPECTED_RECORD_VALUE_OBSERVED(
      StatusCode.INCONSISTENT_STATES,
      "001",
      "An unexpected record value is observed. A bug might exist, or tampering might have occurred. Details: %s",
      "",
      "Data or program tampering, or a software bug, may have occurred. Contact your system administrator to check for any signs of malicious activity."),

  //
  // Errors for INVALID_SIGNATURE(400)
  //
  SIGNATURE_SIGNING_FAILED(
      StatusCode.INVALID_SIGNATURE, "001", "Signing failed. Details: %s", "", "Verify that your private key is valid and accessible. Check the error details for specific issues."),
  SIGNATURE_VALIDATION_FAILED(
      StatusCode.INVALID_SIGNATURE, "002", "Validating signature failed. Details: %s", "", "Verify that the certificate matches the private key used for signing and that both are valid."),
  REQUEST_SIGNATURE_VALIDATION_FAILED(
      StatusCode.INVALID_SIGNATURE, "003", "The request signature can't be validated.", "", "Verify that the certificate used for signing the request is registered and matches the private key."),
  PROOF_SIGNATURE_VALIDATION_FAILED(
      StatusCode.INVALID_SIGNATURE, "004", "The proof signature can't be validated.", "", "Verify that the proof configuration is correct and that the certificate used for signing is valid."),

  //
  // Errors for UNLOADABLE_KEY(401)
  //
  LOADING_KEY_FAILED(
      StatusCode.UNLOADABLE_KEY, "001", "Loading the key failed. Details: %s", "", "Verify that the key file exists at the specified path, is readable, and has the correct format."),
  LOADING_CERTIFICATE_FAILED(
      StatusCode.UNLOADABLE_KEY, "002", "Loading the certificate failed. Details: %s", "", "Verify that the certificate file exists at the specified path, is readable, and has the correct format."),
  CREATING_CIPHER_KEY_FAILED(
      StatusCode.UNLOADABLE_KEY, "003", "Creating a cipher key failed. Details: %s", "", "Verify that the cipher configuration is correct and that the key material is valid."),
  INVALID_PRIVATE_KEY(StatusCode.UNLOADABLE_KEY, "004", "Invalid private key. File: %s", "", "Provide a valid private key file in PEM format at the specified path."),
  INVALID_CERTIFICATE(StatusCode.UNLOADABLE_KEY, "005", "Invalid certificate. File: %s", "", "Provide a valid certificate file in PEM format at the specified path."),
  READING_PRIVATE_KEY_FAILED(
      StatusCode.UNLOADABLE_KEY,
      "006",
      "Reading the private key failed. File: %s; Details: %s",
      "",
      "Verify that the private key file exists, is readable, and has the correct permissions and format."),
  READING_CERTIFICATE_FAILED(
      StatusCode.UNLOADABLE_KEY,
      "007",
      "Reading the certificate failed. File: %s; Details: %s",
      "",
      "Verify that the certificate file exists, is readable, and has the correct permissions and format."),
  CREATING_KEY_STORE_FAILED(
      StatusCode.UNLOADABLE_KEY, "008", "Creating a key store failed. Details: %s", "", "Verify that the key store configuration is correct and that all required files are accessible."),

  //
  // Errors for UNLOADABLE_CONTRACT(402)
  //
  LOADING_CONTRACT_FAILED(
      StatusCode.UNLOADABLE_CONTRACT, "001", "Loading the contract failed. Details: %s", "", "Verify that the contract class is valid and all dependencies are available. Check the error details for specific issues."),

  //
  // Errors for CERTIFICATE_NOT_FOUND(403)
  //
  CERTIFICATE_NOT_FOUND(
      StatusCode.CERTIFICATE_NOT_FOUND, "001", "The specified certificate is not found.", "", "Before using the certificate, register it by using the register-cert command."),

  //
  // Errors for CONTRACT_NOT_FOUND(404)
  //
  CONTRACT_NOT_FOUND(
      StatusCode.CONTRACT_NOT_FOUND, "001", "The specified contract is not found.", "", "Before executing the contract, register it by using the register-contract command."),

  //
  // Errors for CERTIFICATE_ALREADY_REGISTERED(405)
  //
  CERTIFICATE_ALREADY_REGISTERED(
      StatusCode.CERTIFICATE_ALREADY_REGISTERED,
      "001",
      "The specified certificate is already registered.",
      "",
      "Use the existing certificate or register it with a new version number."),

  //
  // Errors for CONTRACT_ALREADY_REGISTERED(406)
  //
  CONTRACT_ALREADY_REGISTERED(
      StatusCode.CONTRACT_ALREADY_REGISTERED,
      "001",
      "The specified contract is already registered.",
      "",
      "Use the existing contract or register it with a different contract ID."),
  DIFFERENT_CLASS_WITH_SAME_NAME(
      StatusCode.CONTRACT_ALREADY_REGISTERED,
      "002",
      "The specified contract binary name has been already registered with a different byte code.",
      "",
      "Use a different contract ID or class name to register this version of the contract."),

  //
  // Errors for SECRET_ALREADY_REGISTERED(413)
  //
  SECRET_ALREADY_REGISTERED(
      StatusCode.SECRET_ALREADY_REGISTERED,
      "001",
      "The specified secret is already registered.",
      "",
      "Use the existing secret or register it with a new version number."),

  //
  // Errors for INVALID_ARGUMENT(414)
  //
  CONFIG_UTILS_INVALID_NUMBER_FORMAT(
      StatusCode.INVALID_ARGUMENT,
      "001",
      "The specified value of the property '%s' is not a number. Value: %s",
      "",
      "Set the property to a valid numeric value in your configuration."),
  CONFIG_UTILS_INVALID_BOOLEAN_FORMAT(
      StatusCode.INVALID_ARGUMENT,
      "002",
      "The specified value of the property '%s' is not a boolean. Value: %s",
      "",
      "Set the property to 'true' or 'false' in your configuration."),
  CONFIG_UTILS_READING_FILE_FAILED(
      StatusCode.INVALID_ARGUMENT, "003", "Reading the file failed. File: %s", "", "Verify that the file exists at the specified path and is readable."),
  LICENSE_CHECKER_CONFIG_LICENSE_KEY_REQUIRED(
      StatusCode.INVALID_ARGUMENT, "004", "Please set your license key to %s.", "", "Set your license key to the specified configuration property."),
  LICENSE_CHECKER_CONFIG_CERTIFICATE_PEM_OR_PATH_REQUIRED(
      StatusCode.INVALID_ARGUMENT,
      "005",
      "Please set your certificate for checking the corresponding license key to %s or %s.",
      "",
      "Set your certificate to one of the specified configuration properties."),
  LICENSE_CHECKER_INVALID_LICENSE_KEY(
      StatusCode.INVALID_ARGUMENT,
      "006",
      "The license key is not for the product '%s'. Please set the correct license key.",
      "",
      "Set the correct license key for the product in your configuration."),
  LICENSE_CHECKER_INVALID_LICENSE_TYPE(
      StatusCode.INVALID_ARGUMENT,
      "007",
      "The license type of the license key must be ENTERPRISE or TRIAL. Please set the correct license key.",
      "",
      "Set a valid ENTERPRISE or TRIAL license key in your configuration."),
  PORT_MUST_BE_GREATER_THAN_OR_EQUAL_TO_ZERO(
      StatusCode.INVALID_ARGUMENT,
      "008",
      "The port and privileged port must be greater than or equal to zero.",
      "",
      "Set the port and privileged port to valid values (>= 0) in your configuration."),
  PRIVATE_KEY_AND_CERT_REQUIRED(
      StatusCode.INVALID_ARGUMENT, "009", "The private key and certificate are required.", "", "Provide both the private key and certificate in your configuration."),
  CERT_VERSION_MUST_BE_GREATER_THAN_ZERO(
      StatusCode.INVALID_ARGUMENT,
      "010",
      "The certificate version must be greater than zero.",
      "",
      "Set the certificate version to a value greater than zero."),
  SECRET_KEY_REQUIRED(
      StatusCode.INVALID_ARGUMENT,
      "011",
      "A secret key is required for HMAC authentication.",
      "",
      "Provide a secret key in your configuration for HMAC authentication."),
  SECRET_VERSION_MUST_BE_GREATER_THAN_ZERO(
      StatusCode.INVALID_ARGUMENT,
      "012",
      "The secret version for HMAC authentication must be greater than zero.",
      "",
      "Set the secret version to a value greater than zero."),
  GRPC_DEADLINE_DURATION_MUST_BE_GREATER_THAN_OR_EQUAL_TO_ZERO(
      StatusCode.INVALID_ARGUMENT,
      "013",
      "The grpc deadline duration must be greater than or equal to zero.",
      "",
      "Set the gRPC deadline duration to a value greater than or equal to zero in your configuration."),
  GRPC_MAX_INBOUND_MESSAGE_SIZE_MUST_BE_GREATER_THAN_OR_EQUAL_TO_ZERO(
      StatusCode.INVALID_ARGUMENT,
      "014",
      "The grpc max inbound message size must be greater than or equal to zero.",
      "",
      "Set the gRPC max inbound message size to a value greater than or equal to zero in your configuration."),
  GRPC_MAX_INBOUND_METADATA_SIZE_MUST_BE_GREATER_THAN_OR_EQUAL_TO_ZERO(
      StatusCode.INVALID_ARGUMENT,
      "015",
      "The grpc max inbound metadata size must be greater than or equal to zero.",
      "",
      "Set the gRPC max inbound metadata size to a value greater than or equal to zero in your configuration."),
  INVALID_AUTHENTICATION_METHOD(
      StatusCode.INVALID_ARGUMENT,
      "016",
      "The authentication method name is invalid. Name: %s",
      "",
      "Set the authentication method to a valid value (like 'digital-signature' or 'hmac') in your configuration."),
  ILLEGAL_ARGUMENT_FORMAT(
      StatusCode.INVALID_ARGUMENT, "017", "The argument format is illegal.", "", "Provide the argument in the correct format. Check the documentation for the expected format."),
  UNSUPPORTED_DESERIALIZATION_TYPE(
      StatusCode.INVALID_ARGUMENT,
      "018",
      "The deserialization type is not supported. Type: %s",
      "",
      "Use a supported deserialization type. Check the documentation for valid types."),
  INVALID_NAMESPACE_NAME(
      StatusCode.INVALID_ARGUMENT, "019", "The namespace name is invalid. Name: %s", "", "Provide a valid namespace name that meets the naming requirements."),
  NAMESPACE_NOT_SUPPORTED_IN_DEPRECATED_CONTRACT(
      StatusCode.INVALID_ARGUMENT,
      "020",
      "Namespace-aware interfaces are not supported in deprecated contracts.",
      "",
      "Use the non-namespace-aware interfaces or migrate to the newer contract interfaces that support namespaces."),
  RESERVED_NAMESPACE(
      StatusCode.INVALID_ARGUMENT,
      "021",
      "The specified namespace is reserved and cannot be created or deleted. Name: %s",
      "",
      "Use a different namespace name that is not reserved. Reserved namespaces are managed by the system and cannot be modified."),

  //
  // Errors for SECRET_NOT_FOUND(415)
  //
  SECRET_NOT_FOUND(
      StatusCode.SECRET_NOT_FOUND, "001", "The specified secret is not found.", "", "Before using the secret, register it by using the register-secret command."),

  //
  // Errors for NAMESPACE_ALREADY_EXISTS(416)
  //
  NAMESPACE_ALREADY_EXISTS(
      StatusCode.NAMESPACE_ALREADY_EXISTS,
      "001",
      "The specified namespace already exists.",
      "",
      "Use the existing namespace or choose a different namespace name."),

  //
  // Errors for NAMESPACE_NOT_FOUND(417)
  //
  NAMESPACE_NOT_FOUND(
      StatusCode.NAMESPACE_NOT_FOUND,
      "001",
      "The specified namespace is not found. Namespace: %s",
      "",
      "Create the namespace first or verify that the namespace name is correct."),

  //
  // Errors for DATABASE_ERROR(500)
  //
  BINDING_CERTIFICATE_FAILED(
      StatusCode.DATABASE_ERROR, "001", "Binding the certificate failed. Details: %s", "", "Check the database connection and ensure the database is accessible. Review the error details for more information."),
  UNBINDING_CERTIFICATE_FAILED(
      StatusCode.DATABASE_ERROR, "002", "Unbinding the certificate failed. Details: %s", "", "Check the database connection and ensure the database is accessible. Review the error details for more information."),
  GETTING_CERTIFICATE_FAILED(
      StatusCode.DATABASE_ERROR, "003", "Getting the certificate failed. Details: %s", "", "Check the database connection and ensure the database is accessible. Review the error details for more information."),
  BINDING_SECRET_KEY_FAILED(
      StatusCode.DATABASE_ERROR, "004", "Binding the secret key failed. Details: %s", "", "Check the database connection and ensure the database is accessible. Review the error details for more information."),
  UNBINDING_SECRET_KEY_FAILED(
      StatusCode.DATABASE_ERROR, "005", "Unbinding the secret key failed. Details: %s", "", "Check the database connection and ensure the database is accessible. Review the error details for more information."),
  GETTING_SECRET_KEY_FAILED(
      StatusCode.DATABASE_ERROR, "006", "Getting the secret key failed. Details: %s", "", "Check the database connection and ensure the database is accessible. Review the error details for more information."),
  BINDING_CONTRACT_FAILED(
      StatusCode.DATABASE_ERROR, "007", "Binding the contract failed. Details: %s", "", "Check the database connection and ensure the database is accessible. Review the error details for more information."),
  GETTING_CONTRACT_FAILED(
      StatusCode.DATABASE_ERROR, "008", "Getting the contract failed. Details: %s", "", "Check the database connection and ensure the database is accessible. Review the error details for more information."),
  SCANNING_CONTRACT_FAILED(
      StatusCode.DATABASE_ERROR, "009", "Scanning the contracts failed. Details: %s", "", "Check the database connection and ensure the database is accessible. Review the error details for more information."),
  CREATING_NAMESPACE_TABLE_FAILED(
      StatusCode.DATABASE_ERROR, "010", "Creating the namespace table failed. Details: %s", "", "Check the database connection and ensure the database is accessible. Review the error details for more information."),
  CREATING_NAMESPACE_FAILED(
      StatusCode.DATABASE_ERROR, "011", "Creating the namespace failed. Details: %s", "", "Check the database connection and ensure the database is accessible. Review the error details for more information."),
  SCANNING_NAMESPACES_FAILED(
      StatusCode.DATABASE_ERROR, "012", "Scanning the namespaces failed. Details: %s", "", "Check the database connection and ensure the database is accessible. Review the error details for more information."),
  DROPPING_NAMESPACE_FAILED(
      StatusCode.DATABASE_ERROR, "013", "Dropping the namespace failed. Details: %s", "", "Check the database connection and ensure the database is accessible. Review the error details for more information."),

  //
  // Errors for RUNTIME_ERROR(502)
  //
  JSON_SERIALIZATION_FAILED(
      StatusCode.RUNTIME_ERROR,
      "001",
      "Serializing the specified json failed. Details: %s",
      "",
      "Check the error details and verify that the data structure is valid for JSON serialization."),
  JSON_DESERIALIZATION_FAILED(
      StatusCode.RUNTIME_ERROR,
      "002",
      "Deserializing the specified json string failed. Details: %s",
      "",
      "Check the error details and verify that the JSON string is valid and well-formed."),
  REQUIRED_FIELDS_ARE_NOT_GIVEN(
      StatusCode.RUNTIME_ERROR, "003", "The required fields are not specified.", "", "Provide all required fields in your request."),
  METADATA_NOT_AVAILABLE(
      StatusCode.RUNTIME_ERROR,
      "004",
      "The metadata is not available since the asset has not been committed yet.",
      "",
      "Commit the asset before accessing its metadata."),
  INVALID_TRANSACTION_STATE_SPECIFIED(
      StatusCode.RUNTIME_ERROR, "005", "The specified transaction state is invalid.", "", "Check the error details in the logs and verify the transaction state."),
  UNSUPPORTED_CONTRACT(
      StatusCode.RUNTIME_ERROR, "006", "The contract type or instance is not supported.", "", "Check the error details in the logs and verify that the contract type is supported."),
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
