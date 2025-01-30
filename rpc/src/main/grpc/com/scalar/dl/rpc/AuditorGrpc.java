package com.scalar.dl.rpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.68.1)",
    comments = "Source: scalar.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class AuditorGrpc {

  private AuditorGrpc() {}

  public static final java.lang.String SERVICE_NAME = "rpc.Auditor";

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
    if ((getRegisterContractMethod = AuditorGrpc.getRegisterContractMethod) == null) {
      synchronized (AuditorGrpc.class) {
        if ((getRegisterContractMethod = AuditorGrpc.getRegisterContractMethod) == null) {
          AuditorGrpc.getRegisterContractMethod = getRegisterContractMethod =
              io.grpc.MethodDescriptor.<com.scalar.dl.rpc.ContractRegistrationRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RegisterContract"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.scalar.dl.rpc.ContractRegistrationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new AuditorMethodDescriptorSupplier("RegisterContract"))
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
    if ((getListContractsMethod = AuditorGrpc.getListContractsMethod) == null) {
      synchronized (AuditorGrpc.class) {
        if ((getListContractsMethod = AuditorGrpc.getListContractsMethod) == null) {
          AuditorGrpc.getListContractsMethod = getListContractsMethod =
              io.grpc.MethodDescriptor.<com.scalar.dl.rpc.ContractsListingRequest, com.scalar.dl.rpc.ContractsListingResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListContracts"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.scalar.dl.rpc.ContractsListingRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.scalar.dl.rpc.ContractsListingResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AuditorMethodDescriptorSupplier("ListContracts"))
              .build();
        }
      }
    }
    return getListContractsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.scalar.dl.rpc.ContractExecutionRequest,
      com.scalar.dl.rpc.ExecutionOrderingResponse> getOrderExecutionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "OrderExecution",
      requestType = com.scalar.dl.rpc.ContractExecutionRequest.class,
      responseType = com.scalar.dl.rpc.ExecutionOrderingResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.scalar.dl.rpc.ContractExecutionRequest,
      com.scalar.dl.rpc.ExecutionOrderingResponse> getOrderExecutionMethod() {
    io.grpc.MethodDescriptor<com.scalar.dl.rpc.ContractExecutionRequest, com.scalar.dl.rpc.ExecutionOrderingResponse> getOrderExecutionMethod;
    if ((getOrderExecutionMethod = AuditorGrpc.getOrderExecutionMethod) == null) {
      synchronized (AuditorGrpc.class) {
        if ((getOrderExecutionMethod = AuditorGrpc.getOrderExecutionMethod) == null) {
          AuditorGrpc.getOrderExecutionMethod = getOrderExecutionMethod =
              io.grpc.MethodDescriptor.<com.scalar.dl.rpc.ContractExecutionRequest, com.scalar.dl.rpc.ExecutionOrderingResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "OrderExecution"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.scalar.dl.rpc.ContractExecutionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.scalar.dl.rpc.ExecutionOrderingResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AuditorMethodDescriptorSupplier("OrderExecution"))
              .build();
        }
      }
    }
    return getOrderExecutionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.scalar.dl.rpc.ExecutionValidationRequest,
      com.scalar.dl.rpc.ContractExecutionResponse> getValidateExecutionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ValidateExecution",
      requestType = com.scalar.dl.rpc.ExecutionValidationRequest.class,
      responseType = com.scalar.dl.rpc.ContractExecutionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.scalar.dl.rpc.ExecutionValidationRequest,
      com.scalar.dl.rpc.ContractExecutionResponse> getValidateExecutionMethod() {
    io.grpc.MethodDescriptor<com.scalar.dl.rpc.ExecutionValidationRequest, com.scalar.dl.rpc.ContractExecutionResponse> getValidateExecutionMethod;
    if ((getValidateExecutionMethod = AuditorGrpc.getValidateExecutionMethod) == null) {
      synchronized (AuditorGrpc.class) {
        if ((getValidateExecutionMethod = AuditorGrpc.getValidateExecutionMethod) == null) {
          AuditorGrpc.getValidateExecutionMethod = getValidateExecutionMethod =
              io.grpc.MethodDescriptor.<com.scalar.dl.rpc.ExecutionValidationRequest, com.scalar.dl.rpc.ContractExecutionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ValidateExecution"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.scalar.dl.rpc.ExecutionValidationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.scalar.dl.rpc.ContractExecutionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AuditorMethodDescriptorSupplier("ValidateExecution"))
              .build();
        }
      }
    }
    return getValidateExecutionMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static AuditorStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuditorStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuditorStub>() {
        @java.lang.Override
        public AuditorStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuditorStub(channel, callOptions);
        }
      };
    return AuditorStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static AuditorBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuditorBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuditorBlockingStub>() {
        @java.lang.Override
        public AuditorBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuditorBlockingStub(channel, callOptions);
        }
      };
    return AuditorBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static AuditorFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuditorFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuditorFutureStub>() {
        @java.lang.Override
        public AuditorFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuditorFutureStub(channel, callOptions);
        }
      };
    return AuditorFutureStub.newStub(factory, channel);
  }

  /**
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
    default void orderExecution(com.scalar.dl.rpc.ContractExecutionRequest request,
        io.grpc.stub.StreamObserver<com.scalar.dl.rpc.ExecutionOrderingResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getOrderExecutionMethod(), responseObserver);
    }

    /**
     */
    default void validateExecution(com.scalar.dl.rpc.ExecutionValidationRequest request,
        io.grpc.stub.StreamObserver<com.scalar.dl.rpc.ContractExecutionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getValidateExecutionMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service Auditor.
   */
  public static abstract class AuditorImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return AuditorGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service Auditor.
   */
  public static final class AuditorStub
      extends io.grpc.stub.AbstractAsyncStub<AuditorStub> {
    private AuditorStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuditorStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuditorStub(channel, callOptions);
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
    public void orderExecution(com.scalar.dl.rpc.ContractExecutionRequest request,
        io.grpc.stub.StreamObserver<com.scalar.dl.rpc.ExecutionOrderingResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getOrderExecutionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void validateExecution(com.scalar.dl.rpc.ExecutionValidationRequest request,
        io.grpc.stub.StreamObserver<com.scalar.dl.rpc.ContractExecutionResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getValidateExecutionMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service Auditor.
   */
  public static final class AuditorBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<AuditorBlockingStub> {
    private AuditorBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuditorBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuditorBlockingStub(channel, callOptions);
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
    public com.scalar.dl.rpc.ExecutionOrderingResponse orderExecution(com.scalar.dl.rpc.ContractExecutionRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getOrderExecutionMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.scalar.dl.rpc.ContractExecutionResponse validateExecution(com.scalar.dl.rpc.ExecutionValidationRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getValidateExecutionMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service Auditor.
   */
  public static final class AuditorFutureStub
      extends io.grpc.stub.AbstractFutureStub<AuditorFutureStub> {
    private AuditorFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuditorFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuditorFutureStub(channel, callOptions);
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
    public com.google.common.util.concurrent.ListenableFuture<com.scalar.dl.rpc.ExecutionOrderingResponse> orderExecution(
        com.scalar.dl.rpc.ContractExecutionRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getOrderExecutionMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.scalar.dl.rpc.ContractExecutionResponse> validateExecution(
        com.scalar.dl.rpc.ExecutionValidationRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getValidateExecutionMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_REGISTER_CONTRACT = 0;
  private static final int METHODID_LIST_CONTRACTS = 1;
  private static final int METHODID_ORDER_EXECUTION = 2;
  private static final int METHODID_VALIDATE_EXECUTION = 3;

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
        case METHODID_ORDER_EXECUTION:
          serviceImpl.orderExecution((com.scalar.dl.rpc.ContractExecutionRequest) request,
              (io.grpc.stub.StreamObserver<com.scalar.dl.rpc.ExecutionOrderingResponse>) responseObserver);
          break;
        case METHODID_VALIDATE_EXECUTION:
          serviceImpl.validateExecution((com.scalar.dl.rpc.ExecutionValidationRequest) request,
              (io.grpc.stub.StreamObserver<com.scalar.dl.rpc.ContractExecutionResponse>) responseObserver);
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
          getOrderExecutionMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.scalar.dl.rpc.ContractExecutionRequest,
              com.scalar.dl.rpc.ExecutionOrderingResponse>(
                service, METHODID_ORDER_EXECUTION)))
        .addMethod(
          getValidateExecutionMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.scalar.dl.rpc.ExecutionValidationRequest,
              com.scalar.dl.rpc.ContractExecutionResponse>(
                service, METHODID_VALIDATE_EXECUTION)))
        .build();
  }

  private static abstract class AuditorBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    AuditorBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.scalar.dl.rpc.ScalarProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Auditor");
    }
  }

  private static final class AuditorFileDescriptorSupplier
      extends AuditorBaseDescriptorSupplier {
    AuditorFileDescriptorSupplier() {}
  }

  private static final class AuditorMethodDescriptorSupplier
      extends AuditorBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    AuditorMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (AuditorGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new AuditorFileDescriptorSupplier())
              .addMethod(getRegisterContractMethod())
              .addMethod(getListContractsMethod())
              .addMethod(getOrderExecutionMethod())
              .addMethod(getValidateExecutionMethod())
              .build();
        }
      }
    }
    return result;
  }
}
