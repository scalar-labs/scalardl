package com.scalar.dl.rpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.68.1)",
    comments = "Source: scalar.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class LedgerPrivilegedGrpc {

  private LedgerPrivilegedGrpc() {}

  public static final java.lang.String SERVICE_NAME = "rpc.LedgerPrivileged";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.scalar.dl.rpc.CertificateRegistrationRequest,
      com.google.protobuf.Empty> getRegisterCertMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RegisterCert",
      requestType = com.scalar.dl.rpc.CertificateRegistrationRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.scalar.dl.rpc.CertificateRegistrationRequest,
      com.google.protobuf.Empty> getRegisterCertMethod() {
    io.grpc.MethodDescriptor<com.scalar.dl.rpc.CertificateRegistrationRequest, com.google.protobuf.Empty> getRegisterCertMethod;
    if ((getRegisterCertMethod = LedgerPrivilegedGrpc.getRegisterCertMethod) == null) {
      synchronized (LedgerPrivilegedGrpc.class) {
        if ((getRegisterCertMethod = LedgerPrivilegedGrpc.getRegisterCertMethod) == null) {
          LedgerPrivilegedGrpc.getRegisterCertMethod = getRegisterCertMethod =
              io.grpc.MethodDescriptor.<com.scalar.dl.rpc.CertificateRegistrationRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RegisterCert"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.scalar.dl.rpc.CertificateRegistrationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new LedgerPrivilegedMethodDescriptorSupplier("RegisterCert"))
              .build();
        }
      }
    }
    return getRegisterCertMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.scalar.dl.rpc.SecretRegistrationRequest,
      com.google.protobuf.Empty> getRegisterSecretMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RegisterSecret",
      requestType = com.scalar.dl.rpc.SecretRegistrationRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.scalar.dl.rpc.SecretRegistrationRequest,
      com.google.protobuf.Empty> getRegisterSecretMethod() {
    io.grpc.MethodDescriptor<com.scalar.dl.rpc.SecretRegistrationRequest, com.google.protobuf.Empty> getRegisterSecretMethod;
    if ((getRegisterSecretMethod = LedgerPrivilegedGrpc.getRegisterSecretMethod) == null) {
      synchronized (LedgerPrivilegedGrpc.class) {
        if ((getRegisterSecretMethod = LedgerPrivilegedGrpc.getRegisterSecretMethod) == null) {
          LedgerPrivilegedGrpc.getRegisterSecretMethod = getRegisterSecretMethod =
              io.grpc.MethodDescriptor.<com.scalar.dl.rpc.SecretRegistrationRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RegisterSecret"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.scalar.dl.rpc.SecretRegistrationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new LedgerPrivilegedMethodDescriptorSupplier("RegisterSecret"))
              .build();
        }
      }
    }
    return getRegisterSecretMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.scalar.dl.rpc.FunctionRegistrationRequest,
      com.google.protobuf.Empty> getRegisterFunctionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RegisterFunction",
      requestType = com.scalar.dl.rpc.FunctionRegistrationRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.scalar.dl.rpc.FunctionRegistrationRequest,
      com.google.protobuf.Empty> getRegisterFunctionMethod() {
    io.grpc.MethodDescriptor<com.scalar.dl.rpc.FunctionRegistrationRequest, com.google.protobuf.Empty> getRegisterFunctionMethod;
    if ((getRegisterFunctionMethod = LedgerPrivilegedGrpc.getRegisterFunctionMethod) == null) {
      synchronized (LedgerPrivilegedGrpc.class) {
        if ((getRegisterFunctionMethod = LedgerPrivilegedGrpc.getRegisterFunctionMethod) == null) {
          LedgerPrivilegedGrpc.getRegisterFunctionMethod = getRegisterFunctionMethod =
              io.grpc.MethodDescriptor.<com.scalar.dl.rpc.FunctionRegistrationRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RegisterFunction"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.scalar.dl.rpc.FunctionRegistrationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new LedgerPrivilegedMethodDescriptorSupplier("RegisterFunction"))
              .build();
        }
      }
    }
    return getRegisterFunctionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.scalar.dl.rpc.StateRetrievalRequest,
      com.scalar.dl.rpc.StateRetrievalResponse> getRetrieveStateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RetrieveState",
      requestType = com.scalar.dl.rpc.StateRetrievalRequest.class,
      responseType = com.scalar.dl.rpc.StateRetrievalResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.scalar.dl.rpc.StateRetrievalRequest,
      com.scalar.dl.rpc.StateRetrievalResponse> getRetrieveStateMethod() {
    io.grpc.MethodDescriptor<com.scalar.dl.rpc.StateRetrievalRequest, com.scalar.dl.rpc.StateRetrievalResponse> getRetrieveStateMethod;
    if ((getRetrieveStateMethod = LedgerPrivilegedGrpc.getRetrieveStateMethod) == null) {
      synchronized (LedgerPrivilegedGrpc.class) {
        if ((getRetrieveStateMethod = LedgerPrivilegedGrpc.getRetrieveStateMethod) == null) {
          LedgerPrivilegedGrpc.getRetrieveStateMethod = getRetrieveStateMethod =
              io.grpc.MethodDescriptor.<com.scalar.dl.rpc.StateRetrievalRequest, com.scalar.dl.rpc.StateRetrievalResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RetrieveState"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.scalar.dl.rpc.StateRetrievalRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.scalar.dl.rpc.StateRetrievalResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LedgerPrivilegedMethodDescriptorSupplier("RetrieveState"))
              .build();
        }
      }
    }
    return getRetrieveStateMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static LedgerPrivilegedStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LedgerPrivilegedStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LedgerPrivilegedStub>() {
        @java.lang.Override
        public LedgerPrivilegedStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LedgerPrivilegedStub(channel, callOptions);
        }
      };
    return LedgerPrivilegedStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static LedgerPrivilegedBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LedgerPrivilegedBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LedgerPrivilegedBlockingStub>() {
        @java.lang.Override
        public LedgerPrivilegedBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LedgerPrivilegedBlockingStub(channel, callOptions);
        }
      };
    return LedgerPrivilegedBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static LedgerPrivilegedFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LedgerPrivilegedFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LedgerPrivilegedFutureStub>() {
        @java.lang.Override
        public LedgerPrivilegedFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LedgerPrivilegedFutureStub(channel, callOptions);
        }
      };
    return LedgerPrivilegedFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void registerCert(com.scalar.dl.rpc.CertificateRegistrationRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRegisterCertMethod(), responseObserver);
    }

    /**
     */
    default void registerSecret(com.scalar.dl.rpc.SecretRegistrationRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRegisterSecretMethod(), responseObserver);
    }

    /**
     */
    default void registerFunction(com.scalar.dl.rpc.FunctionRegistrationRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRegisterFunctionMethod(), responseObserver);
    }

    /**
     */
    default void retrieveState(com.scalar.dl.rpc.StateRetrievalRequest request,
        io.grpc.stub.StreamObserver<com.scalar.dl.rpc.StateRetrievalResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRetrieveStateMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service LedgerPrivileged.
   */
  public static abstract class LedgerPrivilegedImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return LedgerPrivilegedGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service LedgerPrivileged.
   */
  public static final class LedgerPrivilegedStub
      extends io.grpc.stub.AbstractAsyncStub<LedgerPrivilegedStub> {
    private LedgerPrivilegedStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LedgerPrivilegedStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LedgerPrivilegedStub(channel, callOptions);
    }

    /**
     */
    public void registerCert(com.scalar.dl.rpc.CertificateRegistrationRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRegisterCertMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void registerSecret(com.scalar.dl.rpc.SecretRegistrationRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRegisterSecretMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void registerFunction(com.scalar.dl.rpc.FunctionRegistrationRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRegisterFunctionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void retrieveState(com.scalar.dl.rpc.StateRetrievalRequest request,
        io.grpc.stub.StreamObserver<com.scalar.dl.rpc.StateRetrievalResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRetrieveStateMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service LedgerPrivileged.
   */
  public static final class LedgerPrivilegedBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<LedgerPrivilegedBlockingStub> {
    private LedgerPrivilegedBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LedgerPrivilegedBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LedgerPrivilegedBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.google.protobuf.Empty registerCert(com.scalar.dl.rpc.CertificateRegistrationRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRegisterCertMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty registerSecret(com.scalar.dl.rpc.SecretRegistrationRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRegisterSecretMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty registerFunction(com.scalar.dl.rpc.FunctionRegistrationRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRegisterFunctionMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.scalar.dl.rpc.StateRetrievalResponse retrieveState(com.scalar.dl.rpc.StateRetrievalRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRetrieveStateMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service LedgerPrivileged.
   */
  public static final class LedgerPrivilegedFutureStub
      extends io.grpc.stub.AbstractFutureStub<LedgerPrivilegedFutureStub> {
    private LedgerPrivilegedFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LedgerPrivilegedFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LedgerPrivilegedFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> registerCert(
        com.scalar.dl.rpc.CertificateRegistrationRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRegisterCertMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> registerSecret(
        com.scalar.dl.rpc.SecretRegistrationRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRegisterSecretMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> registerFunction(
        com.scalar.dl.rpc.FunctionRegistrationRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRegisterFunctionMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.scalar.dl.rpc.StateRetrievalResponse> retrieveState(
        com.scalar.dl.rpc.StateRetrievalRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRetrieveStateMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_REGISTER_CERT = 0;
  private static final int METHODID_REGISTER_SECRET = 1;
  private static final int METHODID_REGISTER_FUNCTION = 2;
  private static final int METHODID_RETRIEVE_STATE = 3;

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
        case METHODID_REGISTER_CERT:
          serviceImpl.registerCert((com.scalar.dl.rpc.CertificateRegistrationRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_REGISTER_SECRET:
          serviceImpl.registerSecret((com.scalar.dl.rpc.SecretRegistrationRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_REGISTER_FUNCTION:
          serviceImpl.registerFunction((com.scalar.dl.rpc.FunctionRegistrationRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_RETRIEVE_STATE:
          serviceImpl.retrieveState((com.scalar.dl.rpc.StateRetrievalRequest) request,
              (io.grpc.stub.StreamObserver<com.scalar.dl.rpc.StateRetrievalResponse>) responseObserver);
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
          getRegisterCertMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.scalar.dl.rpc.CertificateRegistrationRequest,
              com.google.protobuf.Empty>(
                service, METHODID_REGISTER_CERT)))
        .addMethod(
          getRegisterSecretMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.scalar.dl.rpc.SecretRegistrationRequest,
              com.google.protobuf.Empty>(
                service, METHODID_REGISTER_SECRET)))
        .addMethod(
          getRegisterFunctionMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.scalar.dl.rpc.FunctionRegistrationRequest,
              com.google.protobuf.Empty>(
                service, METHODID_REGISTER_FUNCTION)))
        .addMethod(
          getRetrieveStateMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.scalar.dl.rpc.StateRetrievalRequest,
              com.scalar.dl.rpc.StateRetrievalResponse>(
                service, METHODID_RETRIEVE_STATE)))
        .build();
  }

  private static abstract class LedgerPrivilegedBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    LedgerPrivilegedBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.scalar.dl.rpc.ScalarProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("LedgerPrivileged");
    }
  }

  private static final class LedgerPrivilegedFileDescriptorSupplier
      extends LedgerPrivilegedBaseDescriptorSupplier {
    LedgerPrivilegedFileDescriptorSupplier() {}
  }

  private static final class LedgerPrivilegedMethodDescriptorSupplier
      extends LedgerPrivilegedBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    LedgerPrivilegedMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (LedgerPrivilegedGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new LedgerPrivilegedFileDescriptorSupplier())
              .addMethod(getRegisterCertMethod())
              .addMethod(getRegisterSecretMethod())
              .addMethod(getRegisterFunctionMethod())
              .addMethod(getRetrieveStateMethod())
              .build();
        }
      }
    }
    return result;
  }
}
