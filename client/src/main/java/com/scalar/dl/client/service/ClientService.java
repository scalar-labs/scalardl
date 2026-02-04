package com.scalar.dl.client.service;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.scalar.dl.client.validation.contract.v1_0_0.ValidateLedger.ASSET_ID_KEY;
import static com.scalar.dl.client.validation.contract.v1_0_0.ValidateLedger.END_AGE_KEY;
import static com.scalar.dl.client.validation.contract.v1_0_0.ValidateLedger.NAMESPACE_KEY;
import static com.scalar.dl.client.validation.contract.v1_0_0.ValidateLedger.START_AGE_KEY;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.ClientMode;
import com.scalar.dl.client.error.ClientError;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.util.Common;
import com.scalar.dl.client.util.RequestSigner;
import com.scalar.dl.client.validation.contract.v1_0_0.ValidateLedger;
import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.namespace.Namespaces;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.util.Argument;
import com.scalar.dl.ledger.util.JacksonSerDe;
import com.scalar.dl.rpc.CertificateRegistrationRequest;
import com.scalar.dl.rpc.ContractExecutionRequest;
import com.scalar.dl.rpc.ContractRegistrationRequest;
import com.scalar.dl.rpc.ContractsListingRequest;
import com.scalar.dl.rpc.FunctionRegistrationRequest;
import com.scalar.dl.rpc.LedgerValidationRequest;
import com.scalar.dl.rpc.NamespaceCreationRequest;
import com.scalar.dl.rpc.NamespaceDroppingRequest;
import com.scalar.dl.rpc.NamespacesListingRequest;
import com.scalar.dl.rpc.SecretRegistrationRequest;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * A thread-safe client that interacts with Ledger and Auditor components to register certificates,
 * register contracts, execute contracts, and validate data.
 *
 * <h3>Usage Examples</h3>
 *
 * Here is a simple example to demonstrate how to use {@code ClientService}. {@code ClientService}
 * should always be created with {@link ClientServiceFactory}, which reuses internal instances as
 * much as possible for better performance and less resource usage.
 *
 * <pre>{@code
 * ClientServiceFactory factory = new ClientServiceFactory(); // the factory should be reused
 *
 * ClientService service = factory.create(new ClientConfig(new File(properties));
 * try {
 *   JsonNode jsonArgument = ...; // create an application-specific argument
 *   ContractExecutionResult result = service.executeContract(contractId, jsonArgument);
 *   result.getContractResult().ifPresent(System.out::println);
 * } catch (ClientException e) {
 *   System.err.println(e.getStatusCode());
 *   System.err.println(e.getMessage());
 * }
 *
 * factory.close();
 * }</pre>
 */
@Immutable
public class ClientService implements AutoCloseable {
  private static final JacksonSerDe jacksonSerDe = new JacksonSerDe(new ObjectMapper());
  private final ClientConfig config;
  private final ClientServiceHandler handler;
  private final RequestSigner signer;

  /**
   * Constructs a {@code ClientService} with the specified {@link ClientConfig}, {@link
   * ClientServiceHandler} and {@link RequestSigner}.
   *
   * @param config a configuration for the client
   * @param handler a client service handler to interact the server(s)
   * @param signer a request signer for requests
   */
  public ClientService(
      ClientConfig config, ClientServiceHandler handler, @Nullable RequestSigner signer) {
    this.config = config;
    this.handler = handler;
    this.signer = signer;
  }

  /**
   * Bootstraps the ledger by registering the identity (certificate or secret key) and system
   * contracts if necessary, based on {@code ClientConfig}. The authentication method (digital
   * signature or HMAC) is determined by the configuration. If the identity or contract is already
   * registered, it is simply skipped without throwing an exception.
   *
   * @throws ClientException if a request fails for some reason
   */
  public void bootstrap() {
    try {
      if (config.getAuthenticationMethod().equals(AuthenticationMethod.DIGITAL_SIGNATURE)) {
        registerCertificate();
      } else {
        registerSecret();
      }
    } catch (ClientException e) {
      if (!e.getStatusCode().equals(StatusCode.CERTIFICATE_ALREADY_REGISTERED)
          && !e.getStatusCode().equals(StatusCode.SECRET_ALREADY_REGISTERED)) {
        throw e;
      }
    }

    registerValidateLedgerContract();
  }

  private void registerValidateLedgerContract() {
    if (config.isAuditorEnabled()
        && config.isDefaultAuditorLinearizableValidationContractIdUsed()) {
      Class<?> clazz = ValidateLedger.class;
      try {
        registerContract(
            config.getAuditorLinearizableValidationContractId(),
            clazz.getName(),
            Common.getClassBytes(clazz),
            (String) null);
      } catch (ClientException e) {
        if (!e.getStatusCode().equals(StatusCode.CONTRACT_ALREADY_REGISTERED)) {
          throw e;
        }
      }
    }
  }

  /**
   * Registers the certificate specified in the given {@code ClientConfig} for digital signature
   * authentication.
   *
   * @throws ClientException if a request fails for some reason
   */
  public void registerCertificate() {
    checkClientMode(ClientMode.CLIENT);
    checkState(
        config.getDigitalSignatureIdentityConfig() != null,
        ClientError.CONFIG_DIGITAL_SIGNATURE_AUTHENTICATION_NOT_CONFIGURED.buildMessage());
    CertificateRegistrationRequest.Builder builder =
        CertificateRegistrationRequest.newBuilder()
            .setEntityId(config.getDigitalSignatureIdentityConfig().getEntityId())
            .setKeyVersion(config.getDigitalSignatureIdentityConfig().getCertVersion())
            .setCertPem(config.getDigitalSignatureIdentityConfig().getCert());

    if (!config.getContextNamespace().equals(Namespaces.DEFAULT)) {
      builder.setContextNamespace(config.getContextNamespace());
    }

    handler.registerCertificate(builder.build());
  }

  /**
   * Registers the certificate specified with the serialized byte array of a {@code
   * CertificateRegistrationRequest} for digital signature authentication.
   *
   * @param serializedBinary a serialized byte array of {@code CertificateRegistrationRequest}.
   * @throws ClientException if a request fails for some reason
   */
  public void registerCertificate(byte[] serializedBinary) {
    checkClientMode(ClientMode.INTERMEDIARY);
    assert config.getDigitalSignatureIdentityConfig() == null
        && config.getHmacIdentityConfig() == null;
    CertificateRegistrationRequest request;
    try {
      request = CertificateRegistrationRequest.parseFrom(serializedBinary);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }

    handler.registerCertificate(request);
  }

  /**
   * Registers the secret key specified in the given {@code ClientConfig} for HMAC authentication.
   *
   * @throws ClientException if a request fails for some reason
   */
  public void registerSecret() {
    checkClientMode(ClientMode.CLIENT);
    checkState(
        config.getHmacIdentityConfig() != null,
        ClientError.CONFIG_HMAC_AUTHENTICATION_NOT_CONFIGURED.buildMessage());
    SecretRegistrationRequest.Builder builder =
        SecretRegistrationRequest.newBuilder()
            .setEntityId(config.getHmacIdentityConfig().getEntityId())
            .setKeyVersion(config.getHmacIdentityConfig().getSecretKeyVersion())
            .setSecretKey(config.getHmacIdentityConfig().getSecretKey());

    if (!config.getContextNamespace().equals(Namespaces.DEFAULT)) {
      builder.setContextNamespace(config.getContextNamespace());
    }

    handler.registerSecret(builder.build());
  }

  /**
   * Registers the secret key specified with the serialized byte array of a {@code
   * SecretRegistrationRequest} for HMAC authentication.
   *
   * @param serializedBinary a serialized byte array of {@code SecretRegistrationRequest}.
   * @throws ClientException if a request fails for some reason
   */
  public void registerSecret(byte[] serializedBinary) {
    checkClientMode(ClientMode.INTERMEDIARY);
    assert config.getDigitalSignatureIdentityConfig() == null
        && config.getHmacIdentityConfig() == null;
    SecretRegistrationRequest request;
    try {
      request = SecretRegistrationRequest.parseFrom(serializedBinary);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }

    handler.registerSecret(request);
  }

  /**
   * Registers the specified function. To execute the function, you need to use the corresponding
   * {@code executeContract} methods that match the type of the function.
   *
   * @param id an ID of the function
   * @param name the binary name of the function class
   * @param functionBytes the byte code of the function class
   * @throws ClientException if a request fails for some reason
   */
  public void registerFunction(String id, String name, byte[] functionBytes) {
    checkClientMode(ClientMode.CLIENT);
    checkArgument(id != null, ClientError.SERVICE_FUNCTION_ID_CANNOT_BE_NULL.buildMessage());
    checkArgument(name != null, ClientError.SERVICE_FUNCTION_NAME_CANNOT_BE_NULL.buildMessage());
    checkArgument(
        functionBytes != null, ClientError.SERVICE_FUNCTION_BYTES_CANNOT_BE_NULL.buildMessage());

    FunctionRegistrationRequest request =
        FunctionRegistrationRequest.newBuilder()
            .setFunctionId(id)
            .setFunctionBinaryName(name)
            .setFunctionByteCode(ByteString.copyFrom(functionBytes))
            .build();

    handler.registerFunction(request);
  }

  /**
   * Registers the specified function. To execute the function, you need to use the corresponding
   * {@code executeContract} methods that match the type of the function.
   *
   * @param id an ID of the function
   * @param name the binary name of the function class
   * @param functionPath the relative path of the function class
   * @throws ClientException if a request fails for some reason
   */
  public void registerFunction(String id, String name, String functionPath) {
    checkClientMode(ClientMode.CLIENT);
    checkArgument(id != null, ClientError.SERVICE_FUNCTION_ID_CANNOT_BE_NULL.buildMessage());
    checkArgument(name != null, ClientError.SERVICE_FUNCTION_NAME_CANNOT_BE_NULL.buildMessage());
    checkArgument(
        functionPath != null, ClientError.SERVICE_FUNCTION_PATH_CANNOT_BE_NULL.buildMessage());

    byte[] functionBytes = Common.fileToBytes(functionPath);
    registerFunction(id, name, functionBytes);
  }

  /**
   * Registers the function with the specified serialized byte array of a {@code
   * FunctionRegistrationRequest}.
   *
   * @param serializedBinary a serialized byte array of {@code FunctionRegistrationRequest}.
   * @throws ClientException if a request fails for some reason
   */
  public void registerFunction(byte[] serializedBinary) {
    checkClientMode(ClientMode.INTERMEDIARY);
    FunctionRegistrationRequest request;
    try {
      request = FunctionRegistrationRequest.parseFrom(serializedBinary);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }

    handler.registerFunction(request);
  }

  /**
   * Registers the specified contract for the certificate holder specified in {@code ClientConfig}.
   * To execute the contract, you need to use the corresponding {@code executeContract} methods that
   * match the type of the contract.
   *
   * @param id an ID of the contract
   * @param name the binary name of the contract class
   * @param contractBytes the byte code of the contract class
   * @param properties a contract properties
   * @throws ClientException if a request fails for some reason
   * @deprecated This method will be removed in release 5.0.0.
   */
  @Deprecated
  @SuppressWarnings("InlineMeSuggester")
  public void registerContract(
      String id, String name, byte[] contractBytes, Optional<JsonObject> properties) {
    registerContract(id, name, contractBytes, properties.map(Object::toString).orElse(null));
  }

  /**
   * Registers the specified contract for the certificate holder specified in {@code ClientConfig}.
   * To execute the contract, you need to use the corresponding {@code executeContract} methods that
   * match the type of the contract.
   *
   * @param id an ID for the contract
   * @param name the binary name of the contract class
   * @param contractPath the relative path of the contract class
   * @param properties a contract properties
   * @throws ClientException if a request fails for some reason
   * @deprecated This method will be removed in release 5.0.0.
   */
  @Deprecated
  @SuppressWarnings("InlineMeSuggester")
  public void registerContract(
      String id, String name, String contractPath, Optional<JsonObject> properties) {
    registerContract(id, name, contractPath, properties.map(Object::toString).orElse(null));
  }

  /**
   * Registers the specified contract for the certificate holder specified in {@code ClientConfig}.
   * To execute the contract, you need to use the corresponding {@code executeContract} methods that
   * match the type of the contract.
   *
   * @param id an ID for the contract
   * @param name the binary name of the contract class
   * @param contractBytes the byte array of the contract class
   * @throws ClientException if a request fails for some reason
   */
  public void registerContract(String id, String name, byte[] contractBytes) {
    registerContract(id, name, contractBytes, (String) null);
  }

  /**
   * Registers the specified contract for the certificate holder specified in {@code ClientConfig}.
   * To execute the contract, you need to use the corresponding {@code executeContract} methods that
   * match the type of the contract.
   *
   * @param id an ID for the contract
   * @param name the binary name of the contract class
   * @param contractPath the relative path of the contract class
   * @throws ClientException if a request fails for some reason
   */
  public void registerContract(String id, String name, String contractPath) {
    registerContract(id, name, contractPath, (String) null);
  }

  /**
   * Registers the specified contract for the certificate holder specified in {@code ClientConfig}.
   * To execute the contract, you need to use the corresponding {@code executeContract} methods that
   * match the type of the contract.
   *
   * @param id an ID for the contract
   * @param name the binary name of the contract class
   * @param contractBytes the byte array of the contract class
   * @param properties a contract properties
   * @throws ClientException if a request fails for some reason
   */
  public void registerContract(
      String id, String name, byte[] contractBytes, @Nullable JsonObject properties) {
    registerContract(id, name, contractBytes, properties != null ? properties.toString() : null);
  }

  /**
   * Registers the specified contract for the certificate holder specified in {@code ClientConfig}.
   * To execute the contract, you need to use the corresponding {@code executeContract} methods that
   * match the type of the contract.
   *
   * @param id an ID for the contract
   * @param name the binary name of the contract class
   * @param contractPath the relative path of the contract class
   * @param properties a contract properties
   * @throws ClientException if a request fails for some reason
   */
  public void registerContract(
      String id, String name, String contractPath, @Nullable JsonObject properties) {
    registerContract(id, name, contractPath, properties != null ? properties.toString() : null);
  }

  /**
   * Registers the specified contract for the certificate holder specified in {@code ClientConfig}.
   * To execute the contract, you need to use the corresponding {@code executeContract} methods that
   * match the type of the contract.
   *
   * @param id an ID for the contract
   * @param name the binary name of the contract class
   * @param contractBytes the byte array of the contract class
   * @param properties a contract properties
   * @throws ClientException if a request fails for some reason
   */
  public void registerContract(
      String id, String name, byte[] contractBytes, @Nullable JsonNode properties) {
    registerContract(
        id, name, contractBytes, properties != null ? jacksonSerDe.serialize(properties) : null);
  }

  /**
   * Registers the specified contract for the certificate holder specified in {@code ClientConfig}.
   * To execute the contract, you need to use the corresponding {@code executeContract} methods that
   * match the type of the contract.
   *
   * @param id an ID for the contract
   * @param name the binary name of the contract class
   * @param contractPath the relative path of the contract class
   * @param properties a contract properties
   * @throws ClientException if a request fails for some reason
   */
  public void registerContract(
      String id, String name, String contractPath, @Nullable JsonNode properties) {
    registerContract(
        id, name, contractPath, properties != null ? jacksonSerDe.serialize(properties) : null);
  }

  /**
   * Registers the specified contract for the certificate holder specified in {@code ClientConfig}.
   * To execute the contract, you need to use the corresponding {@code executeContract} methods that
   * match the type of the contract.
   *
   * @param id an ID for the contract
   * @param name the binary name of the contract class
   * @param contractPath the relative path of the contract class
   * @param properties a contract properties
   * @throws ClientException if a request fails for some reason
   */
  public void registerContract(
      String id, String name, String contractPath, @Nullable String properties) {
    checkArgument(
        contractPath != null, ClientError.SERVICE_CONTRACT_PATH_CANNOT_BE_NULL.buildMessage());
    byte[] contractBytes = Common.fileToBytes(contractPath);
    registerContract(id, name, contractBytes, properties);
  }

  /**
   * Registers the specified contract for the certificate holder specified in {@code ClientConfig}.
   * To execute the contract, you need to use the corresponding {@code executeContract} methods that
   * match the type of the contract.
   *
   * @param id an ID for the contract
   * @param name the binary name of the contract class
   * @param contractBytes the byte array of the contract class
   * @param properties a contract properties
   * @throws ClientException if a request fails for some reason
   */
  public void registerContract(
      String id, String name, byte[] contractBytes, @Nullable String properties) {
    checkClientMode(ClientMode.CLIENT);
    checkArgument(id != null, ClientError.SERVICE_CONTRACT_ID_CANNOT_BE_NULL.buildMessage());
    checkArgument(name != null, ClientError.SERVICE_CONTRACT_NAME_CANNOT_BE_NULL.buildMessage());
    checkArgument(
        contractBytes != null, ClientError.SERVICE_CONTRACT_BYTES_CANNOT_BE_NULL.buildMessage());

    ContractRegistrationRequest.Builder builder =
        ContractRegistrationRequest.newBuilder()
            .setEntityId(getEntityId())
            .setKeyVersion(getKeyVersion())
            .setContractId(id)
            .setContractBinaryName(name)
            .setContractByteCode(ByteString.copyFrom(contractBytes));

    if (!config.getContextNamespace().equals(Namespaces.DEFAULT)) {
      builder.setContextNamespace(config.getContextNamespace());
    }
    if (properties != null) {
      builder.setContractProperties(properties);
    }
    ContractRegistrationRequest request = signer.sign(builder).build();

    handler.registerContract(request);
  }

  /**
   * Registers the contract with the specified serialized byte array of a {@code
   * ContractRegistrationRequest}.
   *
   * @param serializedBinary a serialized byte array of {@code ContractRegistrationRequest}.
   * @throws ClientException if a request fails for some reason
   */
  public void registerContract(byte[] serializedBinary) {
    checkClientMode(ClientMode.INTERMEDIARY);
    ContractRegistrationRequest request;
    try {
      request = ContractRegistrationRequest.parseFrom(serializedBinary);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }

    handler.registerContract(request);
  }

  /**
   * Retrieves a list of contracts for the certificate holder specified in {@code ClientConfig}. If
   * specified with a contract ID, it will return the matching contract only.
   *
   * @param id a contract ID
   * @return {@link JsonObject}
   * @throws ClientException if a request fails for some reason
   */
  public JsonObject listContracts(String id) {
    checkClientMode(ClientMode.CLIENT);
    ContractsListingRequest.Builder builder =
        ContractsListingRequest.newBuilder()
            .setEntityId(getEntityId())
            .setKeyVersion(getKeyVersion());

    if (!config.getContextNamespace().equals(Namespaces.DEFAULT)) {
      builder.setContextNamespace(config.getContextNamespace());
    }
    if (id != null) {
      builder.setContractId(id);
    }
    ContractsListingRequest request = signer.sign(builder).build();

    return handler.listContracts(request);
  }

  /**
   * Retrieves a list of contracts with the specified serialized byte array of a {@code
   * ContractsListingRequest}.
   *
   * @param serializedBinary a serialized byte array of {@code ContractsListingRequest}.
   * @return {@link JsonObject}
   * @throws ClientException if a request fails for some reason
   */
  public JsonObject listContracts(byte[] serializedBinary) {
    checkClientMode(ClientMode.INTERMEDIARY);
    ContractsListingRequest request;
    try {
      request = ContractsListingRequest.parseFrom(serializedBinary);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }

    return handler.listContracts(request);
  }

  /**
   * Executes the specified contract (and functions) with the specified argument. The contract and
   * functions have to be based on JSONP, such as {@code JsonpBasedContract} and {@code
   * JsonpBasedFunction}.
   *
   * @param contractId an ID of the contract
   * @param contractArgument an argument of the contract
   * @return {@link ContractExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
  public ContractExecutionResult executeContract(String contractId, JsonObject contractArgument) {
    return executeContract(contractId, contractArgument, null, null);
  }

  /**
   * Executes the specified contract (and functions) with the specified argument. The contract and
   * functions have to be based on JSONP, such as {@code JsonpBasedContract} and {@code
   * JsonpBasedFunction}.
   *
   * <p><b>This method is only for internal use and will be removed in release 5.0.0. Users should
   * not depend on it. Instead, execute contracts without specifying a nonce by using other
   * methods.</b>
   *
   * @param nonce a unique ID of the execution request
   * @param contractId an ID of the contract
   * @param contractArgument an argument of the contract
   * @return {@link ContractExecutionResult}
   * @throws ClientException if a request fails for some reason
   * @deprecated This method will be removed in release 5.0.0.
   */
  @VisibleForTesting
  @Deprecated
  public ContractExecutionResult executeContract(
      String nonce, String contractId, JsonObject contractArgument) {
    return executeContract(nonce, contractId, contractArgument, null, null);
  }

  /**
   * Executes the specified contract (and functions) with the specified arguments. The contract and
   * functions have to be based on JSONP, such as {@code JsonpBasedContract} and {@code
   * JsonpBasedFunction}.
   *
   * <p><b>The contract argument can have a "_functions_" key to specify an array of function IDs to
   * execute with the specified contract, but this feature is deprecated and will be removed in
   * release 5.0.0.</b>
   *
   * @param contractId an ID of the contract
   * @param contractArgument an argument of the contract
   * @param functionArgument an argument of the function
   * @return {@link ContractExecutionResult}
   * @throws ClientException if a request fails for some reason
   * @deprecated This method will be removed in release 5.0.0.
   */
  @Deprecated
  public ContractExecutionResult executeContract(
      String contractId, JsonObject contractArgument, Optional<JsonObject> functionArgument) {
    String nonce = UUID.randomUUID().toString();
    return executeContract(nonce, contractId, contractArgument, functionArgument);
  }

  /**
   * Executes the specified contract (and functions) with the specified arguments. The contract and
   * functions have to be based on JSONP, such as {@code JsonpBasedContract} and {@code
   * JsonpBasedFunction}.
   *
   * <p><b>The contract argument can have a "_functions_" key to specify an array of function IDs to
   * execute with the specified contract, but this feature is deprecated and will be removed in
   * release 5.0.0.</b>
   *
   * <p><b>This method is only for internal use and will be removed in release 5.0.0. Users should
   * not depend on it. Instead, execute contracts without specifying a nonce by using other
   * methods.</b>
   *
   * @param nonce a unique ID of the execution request
   * @param contractId an ID of the contract
   * @param contractArgument an argument of the contract
   * @param functionArgument an argument of the function
   * @return {@link ContractExecutionResult}
   * @throws ClientException if a request fails for some reason
   * @deprecated This method will be removed in release 5.0.0.
   */
  @VisibleForTesting
  @Deprecated
  public ContractExecutionResult executeContract(
      String nonce,
      String contractId,
      JsonObject contractArgument,
      Optional<JsonObject> functionArgument) {
    List<String> functionIds = Argument.getFunctionIds(contractArgument);
    return executeContractInternal(
        nonce,
        contractId,
        Argument.format(contractArgument, nonce).toString(),
        functionIds,
        functionArgument.map(Object::toString).orElse(null));
  }

  /**
   * Executes the specified contract (and function) with the specified arguments. The contract and
   * functions have to be based on JSONP, such as {@code JsonpBasedContract} and {@code
   * JsonpBasedFunction}.
   *
   * @param contractId an ID of the contract
   * @param contractArgument an argument of the contract
   * @param functionId an ID of the function
   * @param functionArgument an argument of the function
   * @return {@link ContractExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
  public ContractExecutionResult executeContract(
      String contractId,
      JsonObject contractArgument,
      @Nullable String functionId,
      @Nullable JsonObject functionArgument) {
    String nonce = UUID.randomUUID().toString();
    return executeContract(nonce, contractId, contractArgument, functionId, functionArgument);
  }

  /**
   * Executes the specified contract (and functions) with the specified arguments. The contract and
   * functions have to be based on JSONP, such as {@code JsonpBasedContract} and {@code
   * JsonpBasedFunction}.
   *
   * <p><b>This method is only for internal use and will be removed in release 5.0.0. Users should
   * not depend on it. Instead, execute contracts without specifying a nonce by using other
   * methods.</b>
   *
   * @param nonce a unique ID of the execution request
   * @param contractId an ID of the contract
   * @param contractArgument an argument of the contract
   * @param functionId an ID of the function
   * @param functionArgument an argument of the function
   * @return {@link ContractExecutionResult}
   * @throws ClientException if a request fails for some reason
   * @deprecated This method will be removed in release 5.0.0.
   */
  @VisibleForTesting
  @Deprecated
  public ContractExecutionResult executeContract(
      String nonce,
      String contractId,
      JsonObject contractArgument,
      @Nullable String functionId,
      @Nullable JsonObject functionArgument) {
    return executeContractInternal(
        nonce,
        contractId,
        Argument.format(contractArgument, nonce, Collections.singletonList(functionId)),
        functionId != null ? Collections.singletonList(functionId) : Collections.emptyList(),
        functionArgument != null ? functionArgument.toString() : null);
  }

  /**
   * Executes the specified contract with the specified argument. The contract and functions have to
   * be based on Jackson, such as {@code JacksonBasedContract} and {@code JacksonBasedFunction}.
   *
   * @param contractId an ID of the contract
   * @param contractArgument an argument of the contract
   * @return {@link ContractExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
  public ContractExecutionResult executeContract(String contractId, JsonNode contractArgument) {
    String nonce = UUID.randomUUID().toString();
    return executeContract(nonce, contractId, contractArgument, null, null);
  }

  /**
   * Executes the specified contract with the specified argument. The contract has to be based on
   * Jackson (i.e., {@code JacksonBasedContract}).
   *
   * <p><b>This method is only for internal use and will be removed in release 5.0.0. Users should
   * not depend on it. Instead, execute contracts without specifying a nonce by using other
   * methods.</b>
   *
   * @param nonce a unique ID of the execution request
   * @param contractId an ID of the contract
   * @param contractArgument an argument of the contract
   * @return {@link ContractExecutionResult}
   * @throws ClientException if a request fails for some reason
   * @deprecated This method will be removed in release 5.0.0.
   */
  @VisibleForTesting
  @Deprecated
  public ContractExecutionResult executeContract(
      String nonce, String contractId, JsonNode contractArgument) {
    return executeContract(nonce, contractId, contractArgument, null, null);
  }

  /**
   * Executes the specified contract (and functions) with the specified arguments. The contract and
   * functions have to be based on Jackson, such as {@code JacksonBasedContract} and {@code
   * JacksonBasedFunction}.
   *
   * @param contractId an ID of the contract
   * @param contractArgument an argument of the contract
   * @param functionId an ID of the function
   * @param functionArgument an argument of the function
   * @return {@link ContractExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
  public ContractExecutionResult executeContract(
      String contractId,
      JsonNode contractArgument,
      @Nullable String functionId,
      @Nullable JsonNode functionArgument) {
    String nonce = UUID.randomUUID().toString();
    return executeContract(nonce, contractId, contractArgument, functionId, functionArgument);
  }

  /**
   * Executes the specified contract (and functions) with the specified arguments. The contract and
   * functions have to be based on Jackson, such as {@code JacksonBasedContract} and {@code
   * JacksonBasedFunction}.
   *
   * <p><b>This method is only for internal use and will be removed in release 5.0.0. Users should
   * not depend on it. Instead, execute contracts without specifying a nonce by using other
   * methods.</b>
   *
   * @param nonce a unique ID of the execution request
   * @param contractId an ID of the contract
   * @param contractArgument an argument of the contract
   * @param functionId an ID of the function
   * @param functionArgument an argument of the function
   * @return {@link ContractExecutionResult}
   * @throws ClientException if a request fails for some reason
   * @deprecated This method will be removed in release 5.0.0.
   */
  @VisibleForTesting
  @Deprecated
  public ContractExecutionResult executeContract(
      String nonce,
      String contractId,
      JsonNode contractArgument,
      @Nullable String functionId,
      @Nullable JsonNode functionArgument) {
    return executeContractInternal(
        nonce,
        contractId,
        Argument.format(contractArgument, nonce, Collections.singletonList(functionId)),
        functionId != null ? Collections.singletonList(functionId) : Collections.emptyList(),
        functionArgument != null ? jacksonSerDe.serialize(functionArgument) : null);
  }

  /**
   * Executes the specified contract with the specified argument. The contract has to be based on
   * String (i.e., {@code StringBasedContract}).
   *
   * @param contractId an ID of the contract
   * @param contractArgument an argument of the contract
   * @return {@link ContractExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
  public ContractExecutionResult executeContract(String contractId, String contractArgument) {
    String nonce = UUID.randomUUID().toString();
    return executeContract(nonce, contractId, contractArgument);
  }

  /**
   * Executes the specified contract with the specified argument. The contract has to be based on
   * String (i.e., {@code StringBasedContract}).
   *
   * <p><b>This method is only for internal use and will be removed in release 5.0.0. Users should
   * not depend on it. Instead, execute contracts without specifying a nonce by using other
   * methods.</b>
   *
   * @param nonce a unique ID of the execution request
   * @param contractId an ID of the contract
   * @param contractArgument an argument of the contract
   * @return {@link ContractExecutionResult}
   * @throws ClientException if a request fails for some reason
   * @deprecated This method will be removed in release 5.0.0.
   */
  @VisibleForTesting
  @Deprecated
  public ContractExecutionResult executeContract(
      String nonce, String contractId, String contractArgument) {
    return executeContractInternal(
        nonce,
        contractId,
        Argument.format(contractArgument, nonce, Collections.emptyList()),
        Collections.emptyList(),
        null);
  }

  /**
   * Executes the specified contract (and function) with the specified arguments. The contract and
   * functions have to be based on String, such as {@code StringBasedContract} and {@code
   * StringBasedFunction}.
   *
   * @param contractId an ID of the contract
   * @param contractArgument an argument of the contract
   * @param functionId an ID of the function
   * @param functionArgument an argument of the function
   * @return {@link ContractExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
  public ContractExecutionResult executeContract(
      String contractId,
      String contractArgument,
      @Nullable String functionId,
      @Nullable String functionArgument) {
    String nonce = UUID.randomUUID().toString();
    return executeContract(nonce, contractId, contractArgument, functionId, functionArgument);
  }

  /**
   * Executes the specified contract (and function) with the specified arguments. The contract and
   * functions have to be based on String, such as {@code StringBasedContract} and {@code
   * StringBasedFunction}.
   *
   * <p><b>This method is only for internal use and will be removed in release 5.0.0. Users should
   * not depend on it. Instead, execute contracts without specifying a nonce by using other
   * methods.</b>
   *
   * @param nonce a unique ID of the execution request
   * @param contractId an ID of the contract
   * @param contractArgument an argument of the contract
   * @param functionId an ID of the function
   * @param functionArgument an argument of the function
   * @return {@link ContractExecutionResult}
   * @throws ClientException if a request fails for some reason
   * @deprecated This method will be removed in release 5.0.0.
   */
  @VisibleForTesting
  @Deprecated
  public ContractExecutionResult executeContract(
      String nonce,
      String contractId,
      String contractArgument,
      @Nullable String functionId,
      @Nullable String functionArgument) {
    return executeContractInternal(
        nonce,
        contractId,
        Argument.format(contractArgument, nonce, Collections.singletonList(functionId)),
        functionId != null ? Collections.singletonList(functionId) : Collections.emptyList(),
        functionArgument);
  }

  private ContractExecutionResult executeContractInternal(
      String nonce,
      String contractId,
      String contractArgument,
      List<String> functionIds,
      @Nullable String functionArgument) {
    checkClientMode(ClientMode.CLIENT);
    checkArgument(
        contractId != null, ClientError.SERVICE_CONTRACT_ID_CANNOT_BE_NULL.buildMessage());
    checkArgument(
        contractArgument != null,
        ClientError.SERVICE_CONTRACT_ARGUMENT_CANNOT_BE_NULL.buildMessage());

    ContractExecutionRequest.Builder builder =
        ContractExecutionRequest.newBuilder()
            .setNonce(nonce)
            .setEntityId(getEntityId())
            .setKeyVersion(getKeyVersion())
            .setContractId(contractId)
            .setContractArgument(contractArgument);

    if (!config.getContextNamespace().equals(Namespaces.DEFAULT)) {
      builder.setContextNamespace(config.getContextNamespace());
    }
    if (!functionIds.isEmpty()) {
      builder.setUseFunctionIds(true).addAllFunctionIds(functionIds);
    }
    if (functionArgument != null) {
      builder.setFunctionArgument(functionArgument);
    }
    ContractExecutionRequest request = signer.sign(builder).build();

    return handler.executeContract(request);
  }

  /**
   * Executes the specified contract with the specified serialized byte array of a {@code
   * ContractExecutionRequest}.
   *
   * @param serializedBinary a serialized byte array of {@code ContractExecutionRequest}.
   * @return {@link ContractExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
  public ContractExecutionResult executeContract(byte[] serializedBinary) {
    checkClientMode(ClientMode.INTERMEDIARY);
    ContractExecutionRequest request;
    try {
      request = ContractExecutionRequest.parseFrom(serializedBinary);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }

    return handler.executeContract(request);
  }

  /**
   * Validates the specified asset in the ledger.
   *
   * @param assetId an asset ID
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateLedger(String assetId) {
    return validateLedger(assetId, 0, Integer.MAX_VALUE);
  }

  /**
   * Validates the specified asset in the specified namespace of the ledger.
   *
   * @param namespace a namespace
   * @param assetId an asset ID
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateLedger(String namespace, String assetId) {
    return validateLedger(namespace, assetId, 0, Integer.MAX_VALUE);
  }

  /**
   * Validates the specified asset between the specified ages in the ledger.
   *
   * @param assetId an asset ID
   * @param startAge an age to be validated from (inclusive)
   * @param endAge an age to be validated to (inclusive)
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateLedger(String assetId, int startAge, int endAge) {
    return validateLedger(null, assetId, startAge, endAge);
  }

  /**
   * Validates the specified asset between the specified ages in the specified namespace of the
   * ledger.
   *
   * @param namespace a namespace
   * @param assetId an asset ID
   * @param startAge an age to be validated from (inclusive)
   * @param endAge an age to be validated to (inclusive)
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateLedger(
      @Nullable String namespace, String assetId, int startAge, int endAge) {
    checkClientMode(ClientMode.CLIENT);
    checkArgument(assetId != null, ClientError.SERVICE_ASSET_ID_CANNOT_BE_NULL.buildMessage());
    checkArgument(
        endAge >= startAge && startAge >= 0, ClientError.SERVICE_INVALID_ASSET_AGES.buildMessage());

    if (config.isAuditorEnabled()) {
      return validateLedgerWithContractExecution(namespace, assetId, startAge, endAge);
    } else {
      LedgerValidationRequest.Builder builder =
          LedgerValidationRequest.newBuilder()
              .setEntityId(getEntityId())
              .setKeyVersion(getKeyVersion())
              .setAssetId(assetId)
              .setStartAge(startAge)
              .setEndAge(endAge);

      if (!config.getContextNamespace().equals(Namespaces.DEFAULT)) {
        builder.setContextNamespace(config.getContextNamespace());
      }
      if (namespace != null) {
        builder.setNamespace(namespace);
      }
      LedgerValidationRequest request = signer.sign(builder).build();

      return handler.validateLedger(request);
    }
  }

  /**
   * Validates the specified asset in the ledger with the specified serialized byte array of a
   * {@code LedgerValidationRequest}.
   *
   * @param serializedBinary a serialized byte array of {@code LedgerValidationRequest}.
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateLedger(byte[] serializedBinary) {
    checkClientMode(ClientMode.INTERMEDIARY);

    if (config.isAuditorEnabled()) {
      throw new UnsupportedOperationException(
          ClientError.CONFIG_VALIDATE_LEDGER_WITH_AUDITOR_NOT_SUPPORTED_WITH_INTERMEDIARY_MODE
              .buildMessage());
    }

    LedgerValidationRequest request;
    try {
      request = LedgerValidationRequest.parseFrom(serializedBinary);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }

    return handler.validateLedger(request);
  }

  /**
   * Creates the specified namespace.
   *
   * @param namespace a namespace name to create
   * @throws ClientException if a request fails for some reason
   */
  public void createNamespace(String namespace) {
    checkClientMode(ClientMode.CLIENT);
    checkArgument(
        namespace != null, ClientError.SERVICE_NAMESPACE_NAME_CANNOT_BE_NULL.buildMessage());
    NamespaceCreationRequest request =
        NamespaceCreationRequest.newBuilder().setNamespace(namespace).build();

    handler.createNamespace(request);
  }

  /**
   * Creates the namespace specified with the serialized byte array of a {@code
   * NamespaceCreationRequest}.
   *
   * @param serializedBinary a serialized byte array of {@code NamespaceCreationRequest}
   * @throws ClientException if a request fails for some reason
   */
  public void createNamespace(byte[] serializedBinary) {
    checkClientMode(ClientMode.INTERMEDIARY);
    NamespaceCreationRequest request;
    try {
      request = NamespaceCreationRequest.parseFrom(serializedBinary);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }

    handler.createNamespace(request);
  }

  /**
   * Drops the specified namespace.
   *
   * @param namespace a namespace name to drop
   * @throws ClientException if a request fails for some reason
   */
  public void dropNamespace(String namespace) {
    checkClientMode(ClientMode.CLIENT);
    checkArgument(
        namespace != null, ClientError.SERVICE_NAMESPACE_NAME_CANNOT_BE_NULL.buildMessage());
    NamespaceDroppingRequest request =
        NamespaceDroppingRequest.newBuilder().setNamespace(namespace).build();

    handler.dropNamespace(request);
  }

  /**
   * Drops the namespace specified with the serialized byte array of a {@code
   * NamespaceDroppingRequest}.
   *
   * @param serializedBinary a serialized byte array of {@code NamespaceDroppingRequest}
   * @throws ClientException if a request fails for some reason
   */
  public void dropNamespace(byte[] serializedBinary) {
    checkClientMode(ClientMode.INTERMEDIARY);
    NamespaceDroppingRequest request;
    try {
      request = NamespaceDroppingRequest.parseFrom(serializedBinary);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }

    handler.dropNamespace(request);
  }

  /**
   * Retrieves a list of all namespaces.
   *
   * @return JSON string containing namespace names
   * @throws ClientException if a request fails for some reason
   */
  public String listNamespaces() {
    return listNamespaces((String) null);
  }

  /**
   * Retrieves namespaces that contain the specified pattern.
   *
   * @param pattern a string pattern. If null or empty, returns all namespaces.
   * @return JSON string containing namespace names
   * @throws ClientException if a request fails for some reason
   */
  public String listNamespaces(@Nullable String pattern) {
    checkClientMode(ClientMode.CLIENT);
    NamespacesListingRequest.Builder builder = NamespacesListingRequest.newBuilder();
    if (pattern != null && !pattern.isEmpty()) {
      builder.setPattern(pattern);
    }
    return handler.listNamespaces(builder.build());
  }

  /**
   * Retrieves a list of namespaces with the specified serialized byte array of a {@code
   * NamespacesListingRequest}.
   *
   * @param serializedBinary a serialized byte array of {@code NamespacesListingRequest}
   * @return JSON string containing namespace names
   * @throws ClientException if a request fails for some reason
   */
  public String listNamespaces(byte[] serializedBinary) {
    checkClientMode(ClientMode.INTERMEDIARY);
    NamespacesListingRequest request;
    try {
      request = NamespacesListingRequest.parseFrom(serializedBinary);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }

    return handler.listNamespaces(request);
  }

  /**
   * It does nothing now. It's left here for backward compatibility.
   *
   * @deprecated This method will be removed in release 5.0.0.
   */
  @Deprecated
  @Override
  public void close() {
    // Does nothing. it's left here for backward compatibility
  }

  private void checkClientMode(ClientMode expected) {
    checkArgument(
        config.getClientMode().equals(expected),
        ClientError.CONFIG_WRONG_CLIENT_MODE_SPECIFIED.buildMessage());
  }

  private String getEntityId() {
    if (config.getDigitalSignatureIdentityConfig() != null) {
      return config.getDigitalSignatureIdentityConfig().getEntityId();
    } else if (config.getHmacIdentityConfig() != null) {
      return config.getHmacIdentityConfig().getEntityId();
    } else {
      throw new AssertionError(
          "Either digital signatures or HMAC is supposed to be configured to reach here.");
    }
  }

  private int getKeyVersion() {
    if (config.getDigitalSignatureIdentityConfig() != null) {
      return config.getDigitalSignatureIdentityConfig().getCertVersion();
    } else if (config.getHmacIdentityConfig() != null) {
      return config.getHmacIdentityConfig().getSecretKeyVersion();
    } else {
      throw new AssertionError(
          "Either digital signatures or HMAC is supposed to be configured to reach here.");
    }
  }

  private LedgerValidationResult validateLedgerWithContractExecution(
      @Nullable String namespace, String assetId, int startAge, int endAge) {
    JsonObjectBuilder argumentBuilder = Json.createObjectBuilder().add(ASSET_ID_KEY, assetId);
    argumentBuilder.add(START_AGE_KEY, startAge);
    argumentBuilder.add(END_AGE_KEY, endAge);

    if (namespace != null) {
      argumentBuilder.add(NAMESPACE_KEY, namespace);
    }

    ContractExecutionResult result =
        executeContract(
            config.getAuditorLinearizableValidationContractId(), argumentBuilder.build());

    return new LedgerValidationResult(
        StatusCode.OK, result.getLedgerProofs().get(0), result.getAuditorProofs().get(0));
  }

  @VisibleForTesting
  ClientServiceHandler getClientServiceHandler() {
    return handler;
  }

  @VisibleForTesting
  RequestSigner getRequestSigner() {
    return signer;
  }
}
