package com.scalar.dl.client.service;

import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.ledger.exception.LedgerException;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.service.ThrowableConsumer;
import com.scalar.dl.ledger.service.ThrowableFunction;
import com.scalar.dl.ledger.util.JsonpSerDe;
import com.scalar.dl.rpc.CertificateRegistrationRequest;
import com.scalar.dl.rpc.ContractRegistrationRequest;
import com.scalar.dl.rpc.ContractsListingRequest;
import com.scalar.dl.rpc.NamespaceCreationRequest;
import com.scalar.dl.rpc.NamespacesListingRequest;
import com.scalar.dl.rpc.SecretRegistrationRequest;
import com.scalar.dl.rpc.Status;
import io.grpc.Metadata;
import io.grpc.protobuf.ProtoUtils;
import javax.json.Json;
import javax.json.JsonObject;

public interface Client {
  Metadata.Key<Status> STATUS_TRAILER_KEY = ProtoUtils.keyForProto(Status.getDefaultInstance());

  void shutdown();

  void register(CertificateRegistrationRequest request);

  void register(SecretRegistrationRequest request);

  void register(ContractRegistrationRequest request);

  JsonObject list(ContractsListingRequest request);

  void create(NamespaceCreationRequest request);

  String list(NamespacesListingRequest request);

  default <T> void accept(ThrowableConsumer<T> f, T request) {
    try {
      f.accept(request);
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
  }

  default <T, R> R apply(ThrowableFunction<T, R> f, T request) {
    try {
      return f.apply(request);
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
    // Java compiler requires this line even though it won't come here
    return null;
  }

  default void throwExceptionWithStatusCode(Exception e) {
    StatusCode code = StatusCode.UNKNOWN_TRANSACTION_STATUS;
    String message = e.getMessage();
    if (e instanceof LedgerException) {
      code = ((LedgerException) e).getCode();
    }

    Metadata trailers = io.grpc.Status.trailersFromThrowable(e);
    if (trailers != null) {
      Status status = trailers.get(STATUS_TRAILER_KEY);
      if (status != null) {
        code = StatusCode.get(status.getCode());
        message = status.getMessage();
      }
    }
    throw new ClientException(message, e, code);
  }

  default JsonObject toJsonObject(String json) {
    return json == null || json.isEmpty()
        ? Json.createObjectBuilder().build()
        : new JsonpSerDe().deserialize(json);
  }
}
