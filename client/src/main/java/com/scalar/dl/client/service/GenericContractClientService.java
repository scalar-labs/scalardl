package com.scalar.dl.client.service;

import static com.google.common.base.Preconditions.checkArgument;
import static com.scalar.dl.genericcontracts.collection.Constants.COLLECTION_ID_PREFIX;
import static com.scalar.dl.genericcontracts.object.Constants.OBJECT_ID_PREFIX;

import com.fasterxml.jackson.databind.JsonNode;
import com.scalar.dl.client.error.ClientError;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.genericcontracts.AssetType;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.json.JsonObject;

/**
 * A thread-safe client that interacts with Ledger and Auditor components to register certificates,
 * register contracts, execute contracts, and validate data in a generic-contract environment.
 *
 * <h3>Usage Examples</h3>
 *
 * Here is a simple example to demonstrate how to use {@code GenericContractClientService}. {@code
 * GenericContractClientService} should always be created with {@link ClientServiceFactory}, which
 * reuses internal instances as much as possible for better performance and less resource usage.
 *
 * <pre>{@code
 * ClientServiceFactory factory = new ClientServiceFactory(); // the factory should be reused
 *
 * GenericContractClientService service =
 *         factory.createForGenericContract(new ClientConfig(new File(properties));
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
public class GenericContractClientService {
  private final ClientService clientService;

  /**
   * Constructs a {@code GenericContractClientService} with the specified {@link ClientService}.
   *
   * @param clientService a client service
   */
  public GenericContractClientService(ClientService clientService) {
    this.clientService = clientService;
  }

  /**
   * Registers the certificate specified in the given {@code ClientConfig} for digital signature
   * authentication.
   *
   * @throws ClientException if a request fails for some reason
   */
  public void registerCertificate() {
    clientService.registerCertificate();
  }

  /**
   * Registers the certificate specified with the serialized byte array of a {@code
   * CertificateRegistrationRequest} for digital signature authentication.
   *
   * @param serializedBinary a serialized byte array of {@code CertificateRegistrationRequest}.
   * @throws ClientException if a request fails for some reason
   */
  public void registerCertificate(byte[] serializedBinary) {
    clientService.registerCertificate(serializedBinary);
  }

  /**
   * Registers the secret key specified in the given {@code ClientConfig} for HMAC authentication.
   *
   * @throws ClientException if a request fails for some reason
   */
  public void registerSecret() {
    clientService.registerSecret();
  }

  /**
   * Registers the secret key specified with the serialized byte array of a {@code
   * SecretRegistrationRequest} for HMAC authentication.
   *
   * @param serializedBinary a serialized byte array of {@code SecretRegistrationRequest}.
   * @throws ClientException if a request fails for some reason
   */
  public void registerSecret(byte[] serializedBinary) {
    clientService.registerSecret(serializedBinary);
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
    clientService.registerFunction(id, name, functionBytes);
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
    clientService.registerFunction(id, name, functionPath);
  }

  /**
   * Registers the function with the specified serialized byte array of a {@code
   * FunctionRegistrationRequest}.
   *
   * @param serializedBinary a serialized byte array of {@code FunctionRegistrationRequest}.
   * @throws ClientException if a request fails for some reason
   */
  public void registerFunction(byte[] serializedBinary) {
    clientService.registerFunction(serializedBinary);
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
    clientService.registerContract(id, name, contractBytes);
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
    clientService.registerContract(id, name, contractPath);
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
    clientService.registerContract(id, name, contractBytes, properties);
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
    clientService.registerContract(id, name, contractPath, properties);
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
    clientService.registerContract(id, name, contractBytes, properties);
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
    clientService.registerContract(id, name, contractPath, properties);
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
    clientService.registerContract(id, name, contractPath, properties);
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
    clientService.registerContract(id, name, contractBytes, properties);
  }

  /**
   * Registers the contract with the specified serialized byte array of a {@code
   * ContractRegistrationRequest}.
   *
   * @param serializedBinary a serialized byte array of {@code ContractRegistrationRequest}.
   * @throws ClientException if a request fails for some reason
   */
  public void registerContract(byte[] serializedBinary) {
    clientService.registerContract(serializedBinary);
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
    return clientService.listContracts(id);
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
    return clientService.listContracts(serializedBinary);
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
    return clientService.executeContract(contractId, contractArgument);
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
    return clientService.executeContract(
        contractId, contractArgument, functionId, functionArgument);
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
    return clientService.executeContract(contractId, contractArgument);
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
    return clientService.executeContract(
        contractId, contractArgument, functionId, functionArgument);
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
    return clientService.executeContract(contractId, contractArgument);
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
    return clientService.executeContract(
        contractId, contractArgument, functionId, functionArgument);
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
    return clientService.executeContract(serializedBinary);
  }

  /**
   * Validates the specified asset in the ledger.
   *
   * @param type a type of the asset
   * @param keys a list of keys that uniquely identify the asset
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateLedger(AssetType type, List<String> keys) {
    return clientService.validateLedger(buildAssetId(type, keys));
  }

  /**
   * Validates the specified asset between the specified ages in the ledger.
   *
   * @param type a type of the asset
   * @param keys a list of keys that uniquely identify the asset
   * @param startAge an age to be validated from (inclusive)
   * @param endAge an age to be validated to (inclusive)
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateLedger(
      AssetType type, List<String> keys, int startAge, int endAge) {
    return clientService.validateLedger(buildAssetId(type, keys), startAge, endAge);
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
    return clientService.validateLedger(serializedBinary);
  }

  private String buildAssetId(AssetType type, List<String> keys) {
    switch (type) {
      case OBJECT:
        checkArgument(keys.size() == 1, ClientError.SERVICE_WRONG_KEYS_SPECIFIED.buildMessage());
        return OBJECT_ID_PREFIX + keys.get(0);
      case COLLECTION:
        checkArgument(keys.size() == 1, ClientError.SERVICE_WRONG_KEYS_SPECIFIED.buildMessage());
        return COLLECTION_ID_PREFIX + keys.get(0);
      default:
        throw new IllegalArgumentException(
            ClientError.SERVICE_WRONG_ASSET_TYPE_SPECIFIED.buildMessage());
    }
  }
}
