package com.scalar.dl.client.service;

import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.rpc.CertificateRegistrationRequest;
import com.scalar.dl.rpc.ContractExecutionRequest;
import com.scalar.dl.rpc.ContractRegistrationRequest;
import com.scalar.dl.rpc.ContractsListingRequest;
import com.scalar.dl.rpc.FunctionRegistrationRequest;
import com.scalar.dl.rpc.LedgerValidationRequest;
import com.scalar.dl.rpc.SecretRegistrationRequest;
import javax.json.JsonObject;

public class GatewayClientServiceHandler implements ClientServiceHandler {
  private final AbstractGatewayClient client;

  /**
   * Constructs a {@code GatewayClientServiceHandler} with the specified {@link
   * AbstractGatewayClient}.
   *
   * @param client a client for the gateway server
   */
  public GatewayClientServiceHandler(AbstractGatewayClient client) {
    this.client = client;
  }

  /**
   * Registers the certificate with the specified {@code CertificateRegistrationRequest} for digital
   * signature authentication.
   *
   * @param request a {@code CertificateRegistrationRequest}.
   * @throws ClientException if a request fails for some reason
   */
  @Override
  public void registerCertificate(CertificateRegistrationRequest request) {
    client.register(request);
  }

  /**
   * Registers the secret key with the specified {@code SecretRegistrationRequest} for HMAC
   * authentication.
   *
   * @param request a {@code SecretRegistrationRequest}.
   * @throws ClientException if a request fails for some reason
   */
  @Override
  public void registerSecret(SecretRegistrationRequest request) {
    client.register(request);
  }

  /**
   * Registers the function with the specified {@code FunctionRegistrationRequest}.
   *
   * @param request a {@code FunctionRegistrationRequest}.
   * @throws ClientException if a request fails for some reason
   */
  @Override
  public void registerFunction(FunctionRegistrationRequest request) {
    client.register(request);
  }

  /**
   * Registers the contract with the specified {@code ContractRegistrationRequest}.
   *
   * @param request a {@code ContractRegistrationRequest}.
   * @throws ClientException if a request fails for some reason
   */
  @Override
  public void registerContract(ContractRegistrationRequest request) {
    client.register(request);
  }

  /**
   * Retrieves a list of contracts with the specified {@code ContractsListingRequest}.
   *
   * @param request a {@code ContractsListingRequest}.
   * @return {@link JsonObject}
   * @throws ClientException if a request fails for some reason
   */
  @Override
  public JsonObject listContracts(ContractsListingRequest request) {
    return client.list(request);
  }

  /**
   * Executes the specified contract with the specified {@code ContractExecutionRequest}.
   *
   * @param request a {@code ContractExecutionRequest}.
   * @return {@link ContractExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
  @Override
  public ContractExecutionResult executeContract(ContractExecutionRequest request) {
    return client.execute(request);
  }

  /**
   * Validates the specified asset in the ledger with the specified {@code LedgerValidationRequest}.
   *
   * @param request a {@code LedgerValidationRequest}.
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  @Override
  public LedgerValidationResult validateLedger(LedgerValidationRequest request) {
    return client.validate(request);
  }

  @VisibleForTesting
  AbstractGatewayClient getGatewayClient() {
    return client;
  }
}
