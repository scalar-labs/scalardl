package com.scalar.dl.ledger.service;

import com.google.inject.Inject;
import com.scalar.dl.ledger.contract.ContractEntry;
import com.scalar.dl.ledger.contract.ContractManager;
import com.scalar.dl.ledger.contract.ContractScanner;
import com.scalar.dl.ledger.crypto.CertificateEntry;
import com.scalar.dl.ledger.crypto.CertificateManager;
import com.scalar.dl.ledger.crypto.ClientKeyValidator;
import com.scalar.dl.ledger.crypto.SecretEntry;
import com.scalar.dl.ledger.crypto.SecretManager;
import com.scalar.dl.ledger.crypto.SignatureValidator;
import com.scalar.dl.ledger.model.CertificateRegistrationRequest;
import com.scalar.dl.ledger.model.ContractRegistrationRequest;
import com.scalar.dl.ledger.model.ContractsListingRequest;
import com.scalar.dl.ledger.model.NamespaceCreationRequest;
import com.scalar.dl.ledger.model.NamespaceDroppingRequest;
import com.scalar.dl.ledger.model.NamespacesListingRequest;
import com.scalar.dl.ledger.model.SecretRegistrationRequest;
import com.scalar.dl.ledger.namespace.NamespaceManager;
import com.scalar.dl.ledger.namespace.Namespaces;
import java.util.List;
import javax.annotation.concurrent.Immutable;

@Immutable
public class BaseService {
  private final CertificateManager certManager;
  private final SecretManager secretManager;
  private final ClientKeyValidator clientKeyValidator;
  private final ContractManager contractManager;
  private final NamespaceManager namespaceManager;

  @Inject
  public BaseService(
      CertificateManager certManager,
      SecretManager secretManager,
      ClientKeyValidator clientKeyValidator,
      ContractManager contractManager,
      NamespaceManager namespaceManager) {
    this.certManager = certManager;
    this.secretManager = secretManager;
    this.clientKeyValidator = clientKeyValidator;
    this.contractManager = contractManager;
    this.namespaceManager = namespaceManager;
  }

  public void register(CertificateRegistrationRequest request) {
    String namespace =
        request.getContextNamespace() == null ? Namespaces.DEFAULT : request.getContextNamespace();
    certManager.register(namespace, CertificateEntry.from(request));
  }

  public void register(SecretRegistrationRequest request) {
    String namespace =
        request.getContextNamespace() == null ? Namespaces.DEFAULT : request.getContextNamespace();
    secretManager.register(namespace, SecretEntry.from(request));
  }

  public void register(ContractRegistrationRequest request) {
    String namespace =
        request.getContextNamespace() == null ? Namespaces.DEFAULT : request.getContextNamespace();
    SignatureValidator validator =
        clientKeyValidator.getValidator(namespace, request.getEntityId(), request.getKeyVersion());
    request.validateWith(validator);

    contractManager.register(namespace, ContractEntry.from(request));
  }

  public List<ContractEntry> list(ContractsListingRequest request) {
    String namespace =
        request.getContextNamespace() == null ? Namespaces.DEFAULT : request.getContextNamespace();
    SignatureValidator validator =
        clientKeyValidator.getValidator(namespace, request.getEntityId(), request.getKeyVersion());
    request.validateWith(validator);

    return new ContractScanner(contractManager)
        .scan(namespace, request.getEntityId(), request.getKeyVersion(), request.getContractId());
  }

  public void create(NamespaceCreationRequest request) {
    namespaceManager.create(request.getNamespace());
  }

  public void drop(NamespaceDroppingRequest request) {
    namespaceManager.drop(request.getNamespace());
  }

  public List<String> list(NamespacesListingRequest request) {
    return namespaceManager.scan(request.getPattern());
  }
}
