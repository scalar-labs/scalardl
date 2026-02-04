package com.scalar.dl.ledger.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.ledger.contract.ContractEntry;
import com.scalar.dl.ledger.crypto.SecretEntry;
import com.scalar.dl.ledger.util.JacksonSerDe;
import com.scalar.dl.ledger.util.Time;
import com.scalar.dl.rpc.AssetProofRetrievalRequest;
import com.scalar.dl.rpc.CertificateRegistrationRequest;
import com.scalar.dl.rpc.ContractExecutionRequest;
import com.scalar.dl.rpc.ContractRegistrationRequest;
import com.scalar.dl.rpc.ContractsListingRequest;
import com.scalar.dl.rpc.ExecutionAbortRequest;
import com.scalar.dl.rpc.FunctionRegistrationRequest;
import com.scalar.dl.rpc.LedgerValidationRequest;
import com.scalar.dl.rpc.NamespaceCreationRequest;
import com.scalar.dl.rpc.NamespaceDroppingRequest;
import com.scalar.dl.rpc.NamespacesListingRequest;
import com.scalar.dl.rpc.SecretRegistrationRequest;
import com.scalar.dl.rpc.StateRetrievalRequest;
import java.util.Base64;
import java.util.List;

public class TypeConverter {
  private static final JacksonSerDe jacksonSerDe = new JacksonSerDe(new ObjectMapper());

  public static com.scalar.dl.ledger.model.CertificateRegistrationRequest convert(
      CertificateRegistrationRequest req) {
    return new com.scalar.dl.ledger.model.CertificateRegistrationRequest(
        req.getEntityId(), req.getKeyVersion(), req.getCertPem());
  }

  public static SecretEntry convert(SecretRegistrationRequest req) {
    return new SecretEntry(
        req.getEntityId(),
        req.getKeyVersion(),
        req.getSecretKey(),
        Time.getCurrentUtcTimeInMillis());
  }

  public static com.scalar.dl.ledger.model.ContractRegistrationRequest convert(
      ContractRegistrationRequest req) {
    return new com.scalar.dl.ledger.model.ContractRegistrationRequest(
        req.getContractId(),
        req.getContractBinaryName(),
        req.getContractByteCode().toByteArray(),
        req.getContractProperties().isEmpty() ? null : req.getContractProperties(),
        req.getEntityId(),
        req.getKeyVersion(),
        req.getSignature().toByteArray());
  }

  public static com.scalar.dl.ledger.model.FunctionRegistrationRequest convert(
      FunctionRegistrationRequest req) {
    return new com.scalar.dl.ledger.model.FunctionRegistrationRequest(
        req.getFunctionId(), req.getFunctionBinaryName(), req.getFunctionByteCode().toByteArray());
  }

  public static com.scalar.dl.ledger.model.ContractsListingRequest convert(
      ContractsListingRequest req) {
    return new com.scalar.dl.ledger.model.ContractsListingRequest(
        req.getContractId(),
        req.getEntityId(),
        req.getKeyVersion(),
        req.getSignature().toByteArray());
  }

  public static com.scalar.dl.ledger.model.ContractExecutionRequest convert(
      ContractExecutionRequest req) {
    return new com.scalar.dl.ledger.model.ContractExecutionRequest(
        req.getNonce(),
        req.getEntityId(),
        req.getKeyVersion(),
        req.getContractId(),
        req.getContractArgument(),
        req.getFunctionIdsList(),
        req.getFunctionArgument().isEmpty() ? null : req.getFunctionArgument(),
        req.getSignature().toByteArray(),
        req.getAuditorSignature().isEmpty() ? null : req.getAuditorSignature().toByteArray());
  }

  public static com.scalar.dl.ledger.model.LedgerValidationRequest convert(
      LedgerValidationRequest req) {
    return new com.scalar.dl.ledger.model.LedgerValidationRequest(
        req.getNamespace().isEmpty() ? null : req.getNamespace(),
        req.getAssetId(),
        req.getStartAge(),
        req.getEndAge(),
        req.getEntityId(),
        req.getKeyVersion(),
        req.getSignature().toByteArray());
  }

  public static com.scalar.dl.ledger.model.AssetProofRetrievalRequest convert(
      AssetProofRetrievalRequest req) {
    return new com.scalar.dl.ledger.model.AssetProofRetrievalRequest(
        req.getNamespace().isEmpty() ? null : req.getNamespace(),
        req.getAssetId(),
        req.getAge(),
        req.getEntityId(),
        req.getKeyVersion(),
        req.getSignature().toByteArray());
  }

  public static com.scalar.dl.ledger.model.StateRetrievalRequest convert(
      StateRetrievalRequest req) {
    return new com.scalar.dl.ledger.model.StateRetrievalRequest(req.getTransactionId());
  }

  public static com.scalar.dl.ledger.model.ExecutionAbortRequest convert(
      ExecutionAbortRequest req) {
    return new com.scalar.dl.ledger.model.ExecutionAbortRequest(
        req.getNonce(), req.getEntityId(), req.getKeyVersion(), req.getSignature().toByteArray());
  }

  public static com.scalar.dl.ledger.model.NamespaceCreationRequest convert(
      NamespaceCreationRequest req) {
    return new com.scalar.dl.ledger.model.NamespaceCreationRequest(req.getNamespace());
  }

  public static com.scalar.dl.ledger.model.NamespaceDroppingRequest convert(
      NamespaceDroppingRequest req) {
    return new com.scalar.dl.ledger.model.NamespaceDroppingRequest(req.getNamespace());
  }

  public static com.scalar.dl.ledger.model.NamespacesListingRequest convert(
      NamespacesListingRequest req) {
    return new com.scalar.dl.ledger.model.NamespacesListingRequest(req.getPattern());
  }

  public static String convert(List<ContractEntry> entries) {
    ObjectNode results = jacksonSerDe.getObjectMapper().createObjectNode();
    entries.forEach(
        e ->
            results.set(
                e.getId(),
                jacksonSerDe
                    .getObjectMapper()
                    .createObjectNode()
                    .put("contract_name", e.getBinaryName())
                    .put("entity_id", e.getEntityId())
                    .put("cert_version", e.getKeyVersion())
                    .put("contract_properties", e.getProperties().orElse(""))
                    .put("registered_at", e.getRegisteredAt())
                    .put("signature", Base64.getEncoder().encodeToString(e.getSignature()))));
    return jacksonSerDe.serialize(results);
  }

  public static String convertNamespaces(List<String> namespaces) {
    ArrayNode results = jacksonSerDe.getObjectMapper().createArrayNode();
    namespaces.forEach(results::add);
    return jacksonSerDe.serialize(
        jacksonSerDe.getObjectMapper().createObjectNode().set("namespaces", results));
  }
}
