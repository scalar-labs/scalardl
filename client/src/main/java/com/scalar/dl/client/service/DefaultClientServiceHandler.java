package com.scalar.dl.client.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.ledger.exception.ValidationException;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.service.StatusCode;
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
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.json.JsonObject;

public class DefaultClientServiceHandler implements ClientServiceHandler {
  private final AbstractLedgerClient client;
  private final AbstractAuditorClient auditorClient;

  /**
   * Constructs a {@code BaseClientService} with the specified {@link AbstractLedgerClient} and
   * {@link AbstractAuditorClient}.
   *
   * @param client a client for the ledger server
   * @param auditorClient a client for the auditor server
   */
  @Inject
  public DefaultClientServiceHandler(
      AbstractLedgerClient client, @Nullable AbstractAuditorClient auditorClient) {
    this.client = client;
    this.auditorClient = auditorClient;
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
    registerToAuditor(request);
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
    registerToAuditor(request);
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
    registerToAuditor(request);
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
    ContractExecutionRequest ordered = order(request);
    return client.execute(ordered, r -> validate(ordered, r));
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

  private void registerToAuditor(CertificateRegistrationRequest request) {
    if (auditorClient == null) {
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
    if (auditorClient == null) {
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
    if (auditorClient == null) {
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
    if (auditorClient == null) {
      return request;
    }

    ExecutionOrderingResponse response = auditorClient.order(request);

    return ContractExecutionRequest.newBuilder(request)
        .setAuditorSignature(response.getSignature())
        .build();
  }

  private ContractExecutionResponse validate(
      ContractExecutionRequest request, ContractExecutionResponse ledgerResponse) {
    if (auditorClient == null) {
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

  @VisibleForTesting
  AbstractLedgerClient getLedgerClient() {
    return client;
  }

  @VisibleForTesting
  AbstractAuditorClient getAuditorClient() {
    return auditorClient;
  }
}
