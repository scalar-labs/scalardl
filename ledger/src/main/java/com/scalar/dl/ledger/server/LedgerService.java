package com.scalar.dl.ledger.server;

import static com.scalar.dl.ledger.server.TypeConverter.convert;

import com.google.inject.Inject;
import com.google.protobuf.Empty;
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.contract.ContractEntry;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.ExecutionAbortResult;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.proof.AssetProof;
import com.scalar.dl.ledger.service.LedgerValidationService;
import com.scalar.dl.ledger.service.ThrowableConsumer;
import com.scalar.dl.ledger.service.ThrowableFunction;
import com.scalar.dl.rpc.AssetProofRetrievalRequest;
import com.scalar.dl.rpc.AssetProofRetrievalResponse;
import com.scalar.dl.rpc.ContractExecutionRequest;
import com.scalar.dl.rpc.ContractExecutionResponse;
import com.scalar.dl.rpc.ContractRegistrationRequest;
import com.scalar.dl.rpc.ContractsListingRequest;
import com.scalar.dl.rpc.ContractsListingResponse;
import com.scalar.dl.rpc.ExecutionAbortRequest;
import com.scalar.dl.rpc.ExecutionAbortResponse;
import com.scalar.dl.rpc.LedgerGrpc;
import com.scalar.dl.rpc.LedgerValidationRequest;
import com.scalar.dl.rpc.LedgerValidationResponse;
import com.scalar.dl.rpc.TransactionState;
import io.grpc.stub.StreamObserver;
import java.util.List;

public class LedgerService extends LedgerGrpc.LedgerImplBase {
  private final com.scalar.dl.ledger.service.LedgerService ledger;
  private final LedgerValidationService validation;
  private final CommonService commonService;
  private final LedgerConfig config;

  @Inject
  public LedgerService(
      com.scalar.dl.ledger.service.LedgerService ledger,
      LedgerValidationService validation,
      CommonService commonService,
      LedgerConfig config) {
    this.ledger = ledger;
    this.validation = validation;
    this.commonService = commonService;
    this.config = config;
  }

  @Override
  public void registerContract(
      ContractRegistrationRequest request, StreamObserver<Empty> responseObserver) {
    ThrowableConsumer<ContractRegistrationRequest> f = r -> ledger.register(convert(r));

    commonService.serve(f, request, responseObserver);
  }

  @Override
  public void listContracts(
      ContractsListingRequest request, StreamObserver<ContractsListingResponse> responseObserver) {
    ThrowableFunction<ContractsListingRequest, ContractsListingResponse> f =
        r -> {
          List<ContractEntry> entries = ledger.list(convert(r));
          return ContractsListingResponse.newBuilder().setJson(convert(entries)).build();
        };

    commonService.serve(f, request, responseObserver);
  }

  @Override
  public void executeContract(
      ContractExecutionRequest request,
      StreamObserver<ContractExecutionResponse> responseObserver) {

    ThrowableFunction<ContractExecutionRequest, ContractExecutionResponse> f =
        r -> {
          ContractExecutionResult result = ledger.execute(convert(r));
          return CommonTypeConverter.convert(result);
        };

    commonService.serve(f, request, responseObserver);
  }

  @Override
  public void validateLedger(
      LedgerValidationRequest request, StreamObserver<LedgerValidationResponse> responseObserver) {
    ThrowableFunction<LedgerValidationRequest, LedgerValidationResponse> f =
        r -> {
          LedgerValidationResult result = validation.validate(convert(r));
          return CommonTypeConverter.convert(result);
        };

    commonService.serve(f, request, responseObserver);
  }

  @Override
  public void retrieveAssetProof(
      AssetProofRetrievalRequest request,
      StreamObserver<AssetProofRetrievalResponse> responseObserver) {
    ThrowableFunction<AssetProofRetrievalRequest, AssetProofRetrievalResponse> f =
        r -> {
          AssetProof proof = validation.retrieve(convert(r));
          return AssetProofRetrievalResponse.newBuilder()
              .setProof(CommonTypeConverter.convert(proof))
              .setLedgerName(config.getName())
              .build();
        };

    commonService.serve(f, request, responseObserver);
  }

  @Override
  public void abortExecution(
      ExecutionAbortRequest request, StreamObserver<ExecutionAbortResponse> responseObserver) {
    ThrowableFunction<ExecutionAbortRequest, ExecutionAbortResponse> f =
        r -> {
          ExecutionAbortResult result = ledger.abort(convert(r));
          return ExecutionAbortResponse.newBuilder()
              .setState(TransactionState.forNumber(result.getState().get()))
              .build();
        };

    commonService.serve(f, request, responseObserver);
  }
}
