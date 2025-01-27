package com.scalar.dl.rpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * Ledger service definition.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.68.1)",
    comments = "Source: scalar.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class LedgerGrpc {

  private LedgerGrpc() {}

  public static final java.lang.String SERVICE_NAME = "rpc.Ledger";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.scalar.dl.rpc.ContractRegistrationRequest,
      com.google.protobuf.Empty> getRegisterContractMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RegisterContract",
      requestType = com.scalar.dl.rpc.ContractRegistrationRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.scalar.dl.rpc.ContractRegistrationRequest,
      com.google.protobuf.Empty> getRegisterContractMethod() {
    io.grpc.MethodDescriptor<com.scalar.dl.rpc.ContractRegistrationRequest, com.google.protobuf.Empty> getRegisterContractMethod;
    if ((getRegisterContractMethod = LedgerGrpc.getRegisterContractMethod) == null) {
      synchronized (LedgerGrpc.class) {
        if ((getRegisterContractMethod = LedgerGrpc.getRegisterContractMethod) == null) {
          LedgerGrpc.getRegisterContractMethod = getRegisterContractMethod =
              io.grpc.MethodDescriptor.<com.scalar.dl.rpc.ContractRegistrationRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RegisterContract"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.scalar.dl.rpc.ContractRegistrationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new LedgerMethodDescriptorSupplier("RegisterContract"))
              .build();
        }
      }
    }
    return getRegisterContractMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.scalar.dl.rpc.ContractsListingRequest,
      com.scalar.dl.rpc.ContractsListingResponse> getListContractsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListContracts",
      requestType = com.scalar.dl.rpc.ContractsListingRequest.class,
      responseType = com.scalar.dl.rpc.ContractsListingResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.scalar.dl.rpc.ContractsListingRequest,
      com.scalar.dl.rpc.ContractsListingResponse> getListContractsMethod() {
    io.grpc.MethodDescriptor<com.scalar.dl.rpc.ContractsListingRequest, com.scalar.dl.rpc.ContractsListingResponse> getListContractsMethod;
    if ((getListContractsMethod = LedgerGrpc.getListContractsMethod) == null) {
      synchronized (LedgerGrpc.class) {
        if ((getListContractsMethod = LedgerGrpc.getListContractsMethod) == null) {
          LedgerGrpc.getListContractsMethod = getListContractsMethod =
              io.grpc.MethodDescriptor.<com.scalar.dl.rpc.ContractsListingRequest, com.scalar.dl.rpc.ContractsListingResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListContracts"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.scalar.dl.rpc.ContractsListingRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.scalar.dl.rpc.ContractsListingResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LedgerMethodDescriptorSupplier("ListContracts"))
              .build();
        }
      }
    }
    return getListContractsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.scalar.dl.rpc.ContractExecutionRequest,
      com.scalar.dl.rpc.ContractExecutionResponse> getExecuteContractMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ExecuteContract",
      requestType = com.scalar.dl.rpc.ContractExecutionRequest.class,
      responseType = com.scalar.dl.rpc.ContractExecutionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.scalar.dl.rpc.ContractExecutionRequest,
      com.scalar.dl.rpc.ContractExecutionResponse> getExecuteContractMethod() {
    io.grpc.MethodDescriptor<com.scalar.dl.rpc.ContractExecutionRequest, com.scalar.dl.rpc.ContractExecutionResponse> getExecuteContractMethod;
    if ((getExecuteContractMethod = LedgerGrpc.getExecuteContractMethod) == null) {
      synchronized (LedgerGrpc.class) {
        if ((getExecuteContractMethod = LedgerGrpc.getExecuteContractMethod) == null) {
          LedgerGrpc.getExecuteContractMethod = getExecuteContractMethod =
              io.grpc.MethodDescriptor.<com.scalar.dl.rpc.ContractExecutionRequest, com.scalar.dl.rpc.ContractExecutionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ExecuteContract"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.scalar.dl.rpc.ContractExecutionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.scalar.dl.rpc.ContractExecutionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LedgerMethodDescriptorSupplier("ExecuteContract"))
              .build();
        }
      }
    }
    return getExecuteContractMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.scalar.dl.rpc.LedgerValidationRequest,
      com.scalar.dl.rpc.LedgerValidationResponse> getValidateLedgerMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ValidateLedger",
      requestType = com.scalar.dl.rpc.LedgerValidationRequest.class,
      responseType = com.scalar.dl.rpc.LedgerValidationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.scalar.dl.rpc.LedgerValidationRequest,
      com.scalar.dl.rpc.LedgerValidationResponse> getValidateLedgerMethod() {
    io.grpc.MethodDescriptor<com.scalar.dl.rpc.LedgerValidationRequest, com.scalar.dl.rpc.LedgerValidationResponse> getValidateLedgerMethod;
    if ((getValidateLedgerMethod = LedgerGrpc.getValidateLedgerMethod) == null) {
      synchronized (LedgerGrpc.class) {
        if ((getValidateLedgerMethod = LedgerGrpc.getValidateLedgerMethod) == null) {
          LedgerGrpc.getValidateLedgerMethod = getValidateLedgerMethod =
              io.grpc.MethodDescriptor.<com.scalar.dl.rpc.LedgerValidationRequest, com.scalar.dl.rpc.LedgerValidationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ValidateLedger"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.scalar.dl.rpc.LedgerValidationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.scalar.dl.rpc.LedgerValidationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LedgerMethodDescriptorSupplier("ValidateLedger"))
              .build();
        }
      }
    }
    return getValidateLedgerMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.scalar.dl.rpc.AssetProofRetrievalRequest,
      com.scalar.dl.rpc.AssetProofRetrievalResponse> getRetrieveAssetProofMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RetrieveAssetProof",
      requestType = com.scalar.dl.rpc.AssetProofRetrievalRequest.class,
      responseType = com.scalar.dl.rpc.AssetProofRetrievalResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.scalar.dl.rpc.AssetProofRetrievalRequest,
      com.scalar.dl.rpc.AssetProofRetrievalResponse> getRetrieveAssetProofMethod() {
    io.grpc.MethodDescriptor<com.scalar.dl.rpc.AssetProofRetrievalRequest, com.scalar.dl.rpc.AssetProofRetrievalResponse> getRetrieveAssetProofMethod;
    if ((getRetrieveAssetProofMethod = LedgerGrpc.getRetrieveAssetProofMethod) == null) {
      synchronized (LedgerGrpc.class) {
        if ((getRetrieveAssetProofMethod = LedgerGrpc.getRetrieveAssetProofMethod) == null) {
          LedgerGrpc.getRetrieveAssetProofMethod = getRetrieveAssetProofMethod =
              io.grpc.MethodDescriptor.<com.scalar.dl.rpc.AssetProofRetrievalRequest, com.scalar.dl.rpc.AssetProofRetrievalResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RetrieveAssetProof"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.scalar.dl.rpc.AssetProofRetrievalRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.scalar.dl.rpc.AssetProofRetrievalResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LedgerMethodDescriptorSupplier("RetrieveAssetProof"))
              .build();
        }
      }
    }
    return getRetrieveAssetProofMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.scalar.dl.rpc.ExecutionAbortRequest,
      com.scalar.dl.rpc.ExecutionAbortResponse> getAbortExecutionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AbortExecution",
      requestType = com.scalar.dl.rpc.ExecutionAbortRequest.class,
      responseType = com.scalar.dl.rpc.ExecutionAbortResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.scalar.dl.rpc.ExecutionAbortRequest,
      com.scalar.dl.rpc.ExecutionAbortResponse> getAbortExecutionMethod() {
    io.grpc.MethodDescriptor<com.scalar.dl.rpc.ExecutionAbortRequest, com.scalar.dl.rpc.ExecutionAbortResponse> getAbortExecutionMethod;
    if ((getAbortExecutionMethod = LedgerGrpc.getAbortExecutionMethod) == null) {
      synchronized (LedgerGrpc.class) {
        if ((getAbortExecutionMethod = LedgerGrpc.getAbortExecutionMethod) == null) {
          LedgerGrpc.getAbortExecutionMethod = getAbortExecutionMethod =
              io.grpc.MethodDescriptor.<com.scalar.dl.rpc.ExecutionAbortRequest, com.scalar.dl.rpc.ExecutionAbortResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AbortExecution"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.scalar.dl.rpc.ExecutionAbortRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.scalar.dl.rpc.ExecutionAbortResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LedgerMethodDescriptorSupplier("AbortExecution"))
              .build();
        }
      }
    }
    return getAbortExecutionMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static LedgerStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LedgerStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LedgerStub>() {
        @java.lang.Override
        public LedgerStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LedgerStub(channel, callOptions);
        }
      };
    return LedgerStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static LedgerBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LedgerBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LedgerBlockingStub>() {
        @java.lang.Override
        public LedgerBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LedgerBlockingStub(channel, callOptions);
        }
      };
    return LedgerBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static LedgerFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LedgerFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LedgerFutureStub>() {
        @java.lang.Override
        public LedgerFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LedgerFutureStub(channel, callOptions);
        }
      };
    return LedgerFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * Ledger service definition.
   * </pre>
   */
  public interface AsyncService {

    /**
     */
    default void registerContract(com.scalar.dl.rpc.ContractRegistrationRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRegisterContractMethod(), responseObserver);
    }

    /**
     */
    default void listContracts(com.scalar.dl.rpc.ContractsListingRequest request,
        io.grpc.stub.StreamObserver<com.scalar.dl.rpc.ContractsListingResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListContractsMethod(), responseObserver);
    }

    /**
     */
    default void executeContract(com.scalar.dl.rpc.ContractExecutionRequest request,
        io.grpc.stub.StreamObserver<com.scalar.dl.rpc.ContractExecutionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getExecuteContractMethod(), responseObserver);
    }

    /**
     */
    default void validateLedger(com.scalar.dl.rpc.LedgerValidationRequest request,
        io.grpc.stub.StreamObserver<com.scalar.dl.rpc.LedgerValidationResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getValidateLedgerMethod(), responseObserver);
    }

    /**
     */
    default void retrieveAssetProof(com.scalar.dl.rpc.AssetProofRetrievalRequest request,
        io.grpc.stub.StreamObserver<com.scalar.dl.rpc.AssetProofRetrievalResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRetrieveAssetProofMethod(), responseObserver);
    }

    /**
     */
    default void abortExecution(com.scalar.dl.rpc.ExecutionAbortRequest request,
        io.grpc.stub.StreamObserver<com.scalar.dl.rpc.ExecutionAbortResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAbortExecutionMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service Ledger.
   * <pre>
   * Ledger service definition.
   * </pre>
   */
  public static abstract class LedgerImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return LedgerGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service Ledger.
   * <pre>
   * Ledger service definition.
   * </pre>
   */
  public static final class LedgerStub
      extends io.grpc.stub.AbstractAsyncStub<LedgerStub> {
    private LedgerStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LedgerStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LedgerStub(channel, callOptions);
    }

    /**
     */
    public void registerContract(com.scalar.dl.rpc.ContractRegistrationRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRegisterContractMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void listContracts(com.scalar.dl.rpc.ContractsListingRequest request,
        io.grpc.stub.StreamObserver<com.scalar.dl.rpc.ContractsListingResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListContractsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void executeContract(com.scalar.dl.rpc.ContractExecutionRequest request,
        io.grpc.stub.StreamObserver<com.scalar.dl.rpc.ContractExecutionResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getExecuteContractMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void validateLedger(com.scalar.dl.rpc.LedgerValidationRequest request,
        io.grpc.stub.StreamObserver<com.scalar.dl.rpc.LedgerValidationResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getValidateLedgerMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void retrieveAssetProof(com.scalar.dl.rpc.AssetProofRetrievalRequest request,
        io.grpc.stub.StreamObserver<com.scalar.dl.rpc.AssetProofRetrievalResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRetrieveAssetProofMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void abortExecution(com.scalar.dl.rpc.ExecutionAbortRequest request,
        io.grpc.stub.StreamObserver<com.scalar.dl.rpc.ExecutionAbortResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAbortExecutionMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service Ledger.
   * <pre>
   * Ledger service definition.
   * </pre>
   */
  public static final class LedgerBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<LedgerBlockingStub> {
    private LedgerBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LedgerBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LedgerBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.google.protobuf.Empty registerContract(com.scalar.dl.rpc.ContractRegistrationRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRegisterContractMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.scalar.dl.rpc.ContractsListingResponse listContracts(com.scalar.dl.rpc.ContractsListingRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListContractsMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.scalar.dl.rpc.ContractExecutionResponse executeContract(com.scalar.dl.rpc.ContractExecutionRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getExecuteContractMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.scalar.dl.rpc.LedgerValidationResponse validateLedger(com.scalar.dl.rpc.LedgerValidationRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getValidateLedgerMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.scalar.dl.rpc.AssetProofRetrievalResponse retrieveAssetProof(com.scalar.dl.rpc.AssetProofRetrievalRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRetrieveAssetProofMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.scalar.dl.rpc.ExecutionAbortResponse abortExecution(com.scalar.dl.rpc.ExecutionAbortRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAbortExecutionMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service Ledger.
   * <pre>
   * Ledger service definition.
   * </pre>
   */
  public static final class LedgerFutureStub
      extends io.grpc.stub.AbstractFutureStub<LedgerFutureStub> {
    private LedgerFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LedgerFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LedgerFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> registerContract(
        com.scalar.dl.rpc.ContractRegistrationRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRegisterContractMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.scalar.dl.rpc.ContractsListingResponse> listContracts(
        com.scalar.dl.rpc.ContractsListingRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListContractsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.scalar.dl.rpc.ContractExecutionResponse> executeContract(
        com.scalar.dl.rpc.ContractExecutionRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getExecuteContractMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.scalar.dl.rpc.LedgerValidationResponse> validateLedger(
        com.scalar.dl.rpc.LedgerValidationRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getValidateLedgerMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.scalar.dl.rpc.AssetProofRetrievalResponse> retrieveAssetProof(
        com.scalar.dl.rpc.AssetProofRetrievalRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRetrieveAssetProofMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.scalar.dl.rpc.ExecutionAbortResponse> abortExecution(
        com.scalar.dl.rpc.ExecutionAbortRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAbortExecutionMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_REGISTER_CONTRACT = 0;
  private static final int METHODID_LIST_CONTRACTS = 1;
  private static final int METHODID_EXECUTE_CONTRACT = 2;
  private static final int METHODID_VALIDATE_LEDGER = 3;
  private static final int METHODID_RETRIEVE_ASSET_PROOF = 4;
  private static final int METHODID_ABORT_EXECUTION = 5;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_REGISTER_CONTRACT:
          serviceImpl.registerContract((com.scalar.dl.rpc.ContractRegistrationRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_LIST_CONTRACTS:
          serviceImpl.listContracts((com.scalar.dl.rpc.ContractsListingRequest) request,
              (io.grpc.stub.StreamObserver<com.scalar.dl.rpc.ContractsListingResponse>) responseObserver);
          break;
        case METHODID_EXECUTE_CONTRACT:
          serviceImpl.executeContract((com.scalar.dl.rpc.ContractExecutionRequest) request,
              (io.grpc.stub.StreamObserver<com.scalar.dl.rpc.ContractExecutionResponse>) responseObserver);
          break;
        case METHODID_VALIDATE_LEDGER:
          serviceImpl.validateLedger((com.scalar.dl.rpc.LedgerValidationRequest) request,
              (io.grpc.stub.StreamObserver<com.scalar.dl.rpc.LedgerValidationResponse>) responseObserver);
          break;
        case METHODID_RETRIEVE_ASSET_PROOF:
          serviceImpl.retrieveAssetProof((com.scalar.dl.rpc.AssetProofRetrievalRequest) request,
              (io.grpc.stub.StreamObserver<com.scalar.dl.rpc.AssetProofRetrievalResponse>) responseObserver);
          break;
        case METHODID_ABORT_EXECUTION:
          serviceImpl.abortExecution((com.scalar.dl.rpc.ExecutionAbortRequest) request,
              (io.grpc.stub.StreamObserver<com.scalar.dl.rpc.ExecutionAbortResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getRegisterContractMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.scalar.dl.rpc.ContractRegistrationRequest,
              com.google.protobuf.Empty>(
                service, METHODID_REGISTER_CONTRACT)))
        .addMethod(
          getListContractsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.scalar.dl.rpc.ContractsListingRequest,
              com.scalar.dl.rpc.ContractsListingResponse>(
                service, METHODID_LIST_CONTRACTS)))
        .addMethod(
          getExecuteContractMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.scalar.dl.rpc.ContractExecutionRequest,
              com.scalar.dl.rpc.ContractExecutionResponse>(
                service, METHODID_EXECUTE_CONTRACT)))
        .addMethod(
          getValidateLedgerMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.scalar.dl.rpc.LedgerValidationRequest,
              com.scalar.dl.rpc.LedgerValidationResponse>(
                service, METHODID_VALIDATE_LEDGER)))
        .addMethod(
          getRetrieveAssetProofMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.scalar.dl.rpc.AssetProofRetrievalRequest,
              com.scalar.dl.rpc.AssetProofRetrievalResponse>(
                service, METHODID_RETRIEVE_ASSET_PROOF)))
        .addMethod(
          getAbortExecutionMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.scalar.dl.rpc.ExecutionAbortRequest,
              com.scalar.dl.rpc.ExecutionAbortResponse>(
                service, METHODID_ABORT_EXECUTION)))
        .build();
  }

  private static abstract class LedgerBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    LedgerBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.scalar.dl.rpc.ScalarProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Ledger");
    }
  }

  private static final class LedgerFileDescriptorSupplier
      extends LedgerBaseDescriptorSupplier {
    LedgerFileDescriptorSupplier() {}
  }

  private static final class LedgerMethodDescriptorSupplier
      extends LedgerBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    LedgerMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (LedgerGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new LedgerFileDescriptorSupplier())
              .addMethod(getRegisterContractMethod())
              .addMethod(getListContractsMethod())
              .addMethod(getExecuteContractMethod())
              .addMethod(getValidateLedgerMethod())
              .addMethod(getRetrieveAssetProofMethod())
              .addMethod(getAbortExecutionMethod())
              .build();
        }
      }
    }
    return result;
  }
}
