package com.scalar.dl.client.service;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Guice;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.ClientMode;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.util.Common;
import com.scalar.dl.client.util.RequestSigner;
import com.scalar.dl.ledger.exception.ValidationException;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.util.Argument;
import com.scalar.dl.ledger.util.JacksonSerDe;
import com.scalar.dl.rpc.AssetProof;
import com.scalar.dl.rpc.CertificateRegistrationRequest;
import com.scalar.dl.rpc.ContractExecutionRequest;
import com.scalar.dl.rpc.ContractExecutionResponse;
import com.scalar.dl.rpc.ContractRegistrationRequest;
import com.scalar.dl.rpc.ContractsListingRequest;
import com.scalar.dl.rpc.ExecutionOrderingResponse;
import com.scalar.dl.rpc.ExecutionValidationRequest;
import com.scalar.dl.rpc.FunctionRegistrationRequest;
import com.scalar.dl.rpc.LedgerValidationRequest;
import com.scalar.dl.rpc.SecretRegistrationRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  static final String VALIDATE_LEDGER_ASSET_ID_KEY = "asset_id";
  static final String VALIDATE_LEDGER_START_AGE_KEY = "start_age";
  static final String VALIDATE_LEDGER_END_AGE_KEY = "end_age";
  private final ClientConfig config;
  private final AbstractLedgerClient client;
  private final AbstractAuditorClient auditorClient;
  private final RequestSigner signer;

  /**
   * Constructs a {@code ClientService} with the specified {@link ClientConfig}, {@link
   * AbstractLedgerClient} and {@link RequestSigner}. This constructor shouldn't be called
   * explicitly and should be called implicitly by {@link Guice}.
   *
   * @param config a configuration for the client
   * @param client a client for the ledger server
   * @param auditorClient a client for the auditor server
   * @param signer a request signer for requests
   */
  public ClientService(
      ClientConfig config,
      AbstractLedgerClient client,
      @Nullable AbstractAuditorClient auditorClient,
      @Nullable RequestSigner signer) {
    this.config = config;
    this.client = client;
    this.auditorClient = auditorClient;
    this.signer = signer;
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
        "Please enable digital signature authentication to call the method.");
    CertificateRegistrationRequest request =
        CertificateRegistrationRequest.newBuilder()
            .setEntityId(config.getDigitalSignatureIdentityConfig().getEntityId())
            .setKeyVersion(config.getDigitalSignatureIdentityConfig().getCertVersion())
            .setCertPem(config.getDigitalSignatureIdentityConfig().getCert())
            .build();

    registerToAuditor(request);
    client.register(request);
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

    registerToAuditor(request);
    client.register(request);
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
        "Please enable HMAC authentication to call the method.");
    SecretRegistrationRequest request =
        SecretRegistrationRequest.newBuilder()
            .setEntityId(config.getHmacIdentityConfig().getEntityId())
            .setKeyVersion(config.getHmacIdentityConfig().getSecretKeyVersion())
            .setSecretKey(config.getHmacIdentityConfig().getSecretKey())
            .build();

    registerToAuditor(request);
    client.register(request);
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

    registerToAuditor(request);
    client.register(request);
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
    checkArgument(id != null, "id cannot be null");
    checkArgument(name != null, "name cannot be null");
    checkArgument(functionBytes != null, "functionBytes cannot be null");

    FunctionRegistrationRequest request =
        FunctionRegistrationRequest.newBuilder()
            .setFunctionId(id)
            .setFunctionBinaryName(name)
            .setFunctionByteCode(ByteString.copyFrom(functionBytes))
            .build();

    client.register(request);
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
    checkArgument(id != null, "id cannot be null");
    checkArgument(name != null, "name cannot be null");
    checkArgument(functionPath != null, "functionPath cannot be null");

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

    client.register(request);
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
    checkArgument(contractPath != null, "contractPath cannot be null");
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
    checkArgument(id != null, "id cannot be null");
    checkArgument(name != null, "name cannot be null");
    checkArgument(contractBytes != null, "contractBytes cannot be null");

    ContractRegistrationRequest.Builder builder =
        ContractRegistrationRequest.newBuilder()
            .setEntityId(getEntityId())
            .setKeyVersion(getKeyVersion())
            .setContractId(id)
            .setContractBinaryName(name)
            .setContractByteCode(ByteString.copyFrom(contractBytes));
    if (properties != null) {
      builder.setContractProperties(properties);
    }
    ContractRegistrationRequest request = signer.sign(builder).build();

    registerToAuditor(request);
    client.register(request);
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

    registerToAuditor(request);
    client.register(request);
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
    if (id != null) {
      builder.setContractId(id);
    }
    ContractsListingRequest request = signer.sign(builder).build();

    return client.list(request);
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

    return client.list(request);
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
   * @param nonce a unique ID of the execution request
   * @param contractId an ID of the contract
   * @param contractArgument an argument of the contract
   * @return {@link ContractExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
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
   * @param nonce a unique ID of the execution request
   * @param contractId an ID of the contract
   * @param contractArgument an argument of the contract
   * @param functionArgument an argument of the function
   * @return {@link ContractExecutionResult}
   * @throws ClientException if a request fails for some reason
   * @deprecated This method will be removed in release 5.0.0.
   */
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
   * @param nonce a unique ID of the execution request
   * @param contractId an ID of the contract
   * @param contractArgument an argument of the contract
   * @param functionId an ID of the function
   * @param functionArgument an argument of the function
   * @return {@link ContractExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
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
   * @param nonce a unique ID of the execution request
   * @param contractId an ID of the contract
   * @param contractArgument an argument of the contract
   * @return {@link ContractExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
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
   * @param nonce a unique ID of the execution request
   * @param contractId an ID of the contract
   * @param contractArgument an argument of the contract
   * @param functionId an ID of the function
   * @param functionArgument an argument of the function
   * @return {@link ContractExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
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
   * @param nonce a unique ID of the execution request
   * @param contractId an ID of the contract
   * @param contractArgument an argument of the contract
   * @return {@link ContractExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
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
   * @param nonce a unique ID of the execution request
   * @param contractId an ID of the contract
   * @param contractArgument an argument of the contract
   * @param functionId an ID of the function
   * @param functionArgument an argument of the function
   * @return {@link ContractExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
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
    checkArgument(contractId != null, "contractId cannot be null");
    checkArgument(contractArgument != null, "contractArgument cannot be null");

    ContractExecutionRequest.Builder builder =
        ContractExecutionRequest.newBuilder()
            .setNonce(nonce)
            .setEntityId(getEntityId())
            .setKeyVersion(getKeyVersion())
            .setContractId(contractId)
            .setContractArgument(contractArgument);

    if (!functionIds.isEmpty()) {
      builder.setUseFunctionIds(true).addAllFunctionIds(functionIds);
    }
    if (functionArgument != null) {
      builder.setFunctionArgument(functionArgument);
    }
    ContractExecutionRequest request = signer.sign(builder).build();

    ContractExecutionRequest ordered = order(request);

    return client.execute(ordered, r -> validate(ordered, r));
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

    ContractExecutionRequest ordered = order(request);

    return client.execute(ordered, r -> validate(ordered, r));
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
   * Validates the specified asset between the specified ages in the ledger.
   *
   * @param assetId an asset ID
   * @param startAge an age to be validated from (inclusive)
   * @param endAge an age to be validated to (inclusive)
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateLedger(String assetId, int startAge, int endAge) {
    checkClientMode(ClientMode.CLIENT);
    checkArgument(assetId != null, "assetId cannot be null");
    checkArgument(endAge >= startAge && startAge >= 0, "invalid ages specified");

    if (config.isAuditorEnabled()) {
      return validateLedgerWithContractExecution(assetId, startAge, endAge);
    } else {
      LedgerValidationRequest.Builder builder =
          LedgerValidationRequest.newBuilder()
              .setEntityId(getEntityId())
              .setKeyVersion(getKeyVersion())
              .setAssetId(assetId)
              .setStartAge(startAge)
              .setEndAge(endAge);
      LedgerValidationRequest request = signer.sign(builder).build();

      return client.validate(request);
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
          "validateLedger with Auditor is not supported in the intermediary mode. "
              + "Please execute ValidateLedger contract simply for validating assets.");
    }

    LedgerValidationRequest request;
    try {
      request = LedgerValidationRequest.parseFrom(serializedBinary);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }

    return client.validate(request);
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
    checkArgument(config.getClientMode().equals(expected), "wrong mode specified.");
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

  private void registerToAuditor(CertificateRegistrationRequest request) {
    if (!config.isAuditorEnabled()) {
      return;
    }
    try {
      auditorClient.register(request);
    } catch (ClientException e) {
      if (!e.getStatusCode().equals(StatusCode.CERTIFICATE_ALREADY_REGISTERED)) {
        throw e;
      }
    }
  }

  private void registerToAuditor(SecretRegistrationRequest request) {
    if (!config.isAuditorEnabled()) {
      return;
    }
    try {
      auditorClient.register(request);
    } catch (ClientException e) {
      if (!e.getStatusCode().equals(StatusCode.SECRET_ALREADY_REGISTERED)) {
        throw e;
      }
    }
  }

  private void registerToAuditor(ContractRegistrationRequest request) {
    if (!config.isAuditorEnabled()) {
      return;
    }
    try {
      auditorClient.register(request);
    } catch (ClientException e) {
      if (!e.getStatusCode().equals(StatusCode.CONTRACT_ALREADY_REGISTERED)) {
        throw e;
      }
    }
  }

  private ContractExecutionRequest order(ContractExecutionRequest request) {
    if (!config.isAuditorEnabled()) {
      return request;
    }

    ExecutionOrderingResponse response = auditorClient.order(request);

    return ContractExecutionRequest.newBuilder(request)
        .setAuditorSignature(response.getSignature())
        .build();
  }

  private ContractExecutionResponse validate(
      ContractExecutionRequest request, ContractExecutionResponse ledgerResponse) {
    if (!config.isAuditorEnabled()) {
      return null;
    }
    ExecutionValidationRequest req =
        ExecutionValidationRequest.newBuilder()
            .setRequest(request)
            .addAllProofs(ledgerResponse.getProofsList())
            .build();

    ContractExecutionResponse auditorResponse = auditorClient.validate(req);

    validateResponses(ledgerResponse, auditorResponse);

    return auditorResponse;
  }

  private void validateResponses(
      ContractExecutionResponse ledgerResponse, ContractExecutionResponse auditorResponse) {
    Runnable throwError =
        () -> {
          throw new ValidationException(
              "The results from Ledger and Auditor don't match", StatusCode.INCONSISTENT_STATES);
        };

    if (!ledgerResponse.getContractResult().equals(auditorResponse.getContractResult())
        || ledgerResponse.getProofsCount() != auditorResponse.getProofsCount()) {
      throwError.run();
    }

    Map<String, AssetProof> map = new HashMap<>();
    ledgerResponse.getProofsList().forEach(p -> map.put(p.getAssetId(), p));
    auditorResponse
        .getProofsList()
        .forEach(
            p2 -> {
              AssetProof p1 = map.get(p2.getAssetId());
              if (p1 == null || p1.getAge() != p2.getAge() || !p1.getHash().equals(p2.getHash())) {
                throwError.run();
              }
            });
  }

  private LedgerValidationResult validateLedgerWithContractExecution(
      String assetId, int startAge, int endAge) {
    JsonObjectBuilder argumentBuilder =
        Json.createObjectBuilder().add(VALIDATE_LEDGER_ASSET_ID_KEY, assetId);
    argumentBuilder.add(VALIDATE_LEDGER_START_AGE_KEY, startAge);
    argumentBuilder.add(VALIDATE_LEDGER_END_AGE_KEY, endAge);

    ContractExecutionResult result =
        executeContract(
            config.getAuditorLinearizableValidationContractId(), argumentBuilder.build());

    return new LedgerValidationResult(
        StatusCode.OK, result.getLedgerProofs().get(0), result.getAuditorProofs().get(0));
  }

  @VisibleForTesting
  AbstractLedgerClient getLedgerClient() {
    return client;
  }

  @VisibleForTesting
  AbstractAuditorClient getAuditorClient() {
    return auditorClient;
  }

  @VisibleForTesting
  RequestSigner getRequestSigner() {
    return signer;
  }
}
