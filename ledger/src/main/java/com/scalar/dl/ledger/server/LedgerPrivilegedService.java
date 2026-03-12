package com.scalar.dl.ledger.server;

import static com.scalar.dl.ledger.server.TypeConverter.convert;

import com.google.inject.Inject;
import com.google.protobuf.Empty;
import com.scalar.dl.ledger.model.StateRetrievalResult;
import com.scalar.dl.ledger.service.LedgerService;
import com.scalar.dl.ledger.service.ThrowableConsumer;
import com.scalar.dl.rpc.CertificateRegistrationRequest;
import com.scalar.dl.rpc.FunctionRegistrationRequest;
import com.scalar.dl.rpc.LedgerPrivilegedGrpc;
import com.scalar.dl.rpc.NamespaceCreationRequest;
import com.scalar.dl.rpc.NamespaceDroppingRequest;
import com.scalar.dl.rpc.NamespacesListingRequest;
import com.scalar.dl.rpc.NamespacesListingResponse;
import com.scalar.dl.rpc.SecretRegistrationRequest;
import com.scalar.dl.rpc.StateRetrievalRequest;
import com.scalar.dl.rpc.StateRetrievalResponse;
import com.scalar.dl.rpc.TransactionState;
import io.grpc.stub.StreamObserver;
import java.util.List;
import javax.annotation.concurrent.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
public class LedgerPrivilegedService extends LedgerPrivilegedGrpc.LedgerPrivilegedImplBase {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(LedgerPrivilegedService.class.getName());
  private final com.scalar.dl.ledger.service.LedgerService ledger;
  private final CommonService commonService;

  @Inject
  public LedgerPrivilegedService(LedgerService ledger, CommonService commonService) {
    this.ledger = ledger;
    this.commonService = commonService;
  }

  @Override
  public void registerCert(
      CertificateRegistrationRequest request, StreamObserver<Empty> responseObserver) {
    ThrowableConsumer<CertificateRegistrationRequest> f = r -> ledger.register(convert(r));
    commonService.serve(f, request, responseObserver);
  }

  @Override
  public void registerSecret(
      SecretRegistrationRequest request, StreamObserver<Empty> responseObserver) {
    ThrowableConsumer<SecretRegistrationRequest> f = r -> ledger.register(convert(r));
    commonService.serve(f, request, responseObserver);
  }

  @Override
  public void registerFunction(
      FunctionRegistrationRequest request, StreamObserver<Empty> responseObserver) {
    ThrowableConsumer<FunctionRegistrationRequest> f = r -> ledger.register(convert(r));
    commonService.serve(f, request, responseObserver);
  }

  @Override
  public void retrieveState(
      StateRetrievalRequest request, StreamObserver<StateRetrievalResponse> responseObserver) {
    try {
      StateRetrievalResult result = ledger.retrieve(convert(request));
      StateRetrievalResponse response =
          StateRetrievalResponse.newBuilder()
              .setState(TransactionState.forNumber(result.getState().get()))
              .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      responseObserver.onError(e);
    }
  }

  @Override
  public void createNamespace(
      NamespaceCreationRequest request, StreamObserver<Empty> responseObserver) {
    ThrowableConsumer<NamespaceCreationRequest> f = r -> ledger.create(convert(r));
    commonService.serve(f, request, responseObserver);
  }

  @Override
  public void dropNamespace(
      NamespaceDroppingRequest request, StreamObserver<Empty> responseObserver) {
    ThrowableConsumer<NamespaceDroppingRequest> f = r -> ledger.drop(convert(r));
    commonService.serve(f, request, responseObserver);
  }

  @Override
  public void listNamespaces(
      NamespacesListingRequest request,
      StreamObserver<NamespacesListingResponse> responseObserver) {
    try {
      List<String> namespaces = ledger.list(convert(request));
      NamespacesListingResponse response =
          NamespacesListingResponse.newBuilder()
              .setJson(TypeConverter.convertNamespaces(namespaces))
              .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      responseObserver.onError(e);
    }
  }
}
