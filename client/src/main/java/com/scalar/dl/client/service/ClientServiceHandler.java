package com.scalar.dl.client.service;

import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.rpc.CertificateRegistrationRequest;
import com.scalar.dl.rpc.ContractExecutionRequest;
import com.scalar.dl.rpc.ContractRegistrationRequest;
import com.scalar.dl.rpc.ContractsListingRequest;
import com.scalar.dl.rpc.FunctionRegistrationRequest;
import com.scalar.dl.rpc.LedgerValidationRequest;
import com.scalar.dl.rpc.NamespaceCreationRequest;
import com.scalar.dl.rpc.NamespacesListingRequest;
import com.scalar.dl.rpc.SecretRegistrationRequest;
import javax.json.JsonObject;

public interface ClientServiceHandler {

  /**
   * Registers the certificate with the specified {@code CertificateRegistrationRequest} for digital
   * signature authentication.
   *
   * @param request a {@code CertificateRegistrationRequest}.
   * @throws ClientException if a request fails for some reason
   */
  void registerCertificate(CertificateRegistrationRequest request);

  /**
   * Registers the secret key with the specified {@code SecretRegistrationRequest} for HMAC
   * authentication.
   *
   * @param request a {@code SecretRegistrationRequest}.
   * @throws ClientException if a request fails for some reason
   */
  void registerSecret(SecretRegistrationRequest request);

  /**
   * Registers the function with the specified {@code FunctionRegistrationRequest}.
   *
   * @param request a {@code FunctionRegistrationRequest}.
   * @throws ClientException if a request fails for some reason
   */
  void registerFunction(FunctionRegistrationRequest request);

  /**
   * Registers the contract with the specified {@code ContractRegistrationRequest}.
   *
   * @param request a {@code ContractRegistrationRequest}.
   * @throws ClientException if a request fails for some reason
   */
  void registerContract(ContractRegistrationRequest request);

  /**
   * Retrieves a list of contracts with the specified {@code ContractsListingRequest}.
   *
   * @param request a {@code ContractsListingRequest}.
   * @return {@link JsonObject}
   * @throws ClientException if a request fails for some reason
   */
  JsonObject listContracts(ContractsListingRequest request);

  /**
   * Executes the specified contract with the specified {@code ContractExecutionRequest}.
   *
   * @param request a {@code ContractExecutionRequest}.
   * @return {@link ContractExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
  ContractExecutionResult executeContract(ContractExecutionRequest request);

  /**
   * Validates the specified asset in the ledger with the specified {@code LedgerValidationRequest}.
   *
   * @param request a {@code LedgerValidationRequest}.
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  LedgerValidationResult validateLedger(LedgerValidationRequest request);

  void createNamespace(NamespaceCreationRequest request);

  /**
   * Retrieves a list of namespaces with the specified {@code NamespacesListingRequest}.
   *
   * @param request a {@code NamespacesListingRequest}.
   * @return JSON string containing namespace names
   * @throws ClientException if a request fails for some reason
   */
  String listNamespaces(NamespacesListingRequest request);
}
