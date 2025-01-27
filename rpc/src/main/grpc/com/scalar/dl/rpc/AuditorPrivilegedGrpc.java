package com.scalar.dl.rpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.68.1)",
    comments = "Source: scalar.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class AuditorPrivilegedGrpc {

  private AuditorPrivilegedGrpc() {}

  public static final java.lang.String SERVICE_NAME = "rpc.AuditorPrivileged";

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
    if ((getRegisterCertMethod = AuditorPrivilegedGrpc.getRegisterCertMethod) == null) {
      synchronized (AuditorPrivilegedGrpc.class) {
        if ((getRegisterCertMethod = AuditorPrivilegedGrpc.getRegisterCertMethod) == null) {
          AuditorPrivilegedGrpc.getRegisterCertMethod = getRegisterCertMethod =
              io.grpc.MethodDescriptor.<com.scalar.dl.rpc.CertificateRegistrationRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RegisterCert"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.scalar.dl.rpc.CertificateRegistrationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new AuditorPrivilegedMethodDescriptorSupplier("RegisterCert"))
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
    if ((getRegisterSecretMethod = AuditorPrivilegedGrpc.getRegisterSecretMethod) == null) {
      synchronized (AuditorPrivilegedGrpc.class) {
        if ((getRegisterSecretMethod = AuditorPrivilegedGrpc.getRegisterSecretMethod) == null) {
          AuditorPrivilegedGrpc.getRegisterSecretMethod = getRegisterSecretMethod =
              io.grpc.MethodDescriptor.<com.scalar.dl.rpc.SecretRegistrationRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RegisterSecret"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.scalar.dl.rpc.SecretRegistrationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new AuditorPrivilegedMethodDescriptorSupplier("RegisterSecret"))
              .build();
        }
      }
    }
    return getRegisterSecretMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static AuditorPrivilegedStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuditorPrivilegedStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuditorPrivilegedStub>() {
        @java.lang.Override
        public AuditorPrivilegedStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuditorPrivilegedStub(channel, callOptions);
        }
      };
    return AuditorPrivilegedStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static AuditorPrivilegedBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuditorPrivilegedBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuditorPrivilegedBlockingStub>() {
        @java.lang.Override
        public AuditorPrivilegedBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuditorPrivilegedBlockingStub(channel, callOptions);
        }
      };
    return AuditorPrivilegedBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static AuditorPrivilegedFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuditorPrivilegedFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuditorPrivilegedFutureStub>() {
        @java.lang.Override
        public AuditorPrivilegedFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuditorPrivilegedFutureStub(channel, callOptions);
        }
      };
    return AuditorPrivilegedFutureStub.newStub(factory, channel);
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
  }

  /**
   * Base class for the server implementation of the service AuditorPrivileged.
   */
  public static abstract class AuditorPrivilegedImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return AuditorPrivilegedGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service AuditorPrivileged.
   */
  public static final class AuditorPrivilegedStub
      extends io.grpc.stub.AbstractAsyncStub<AuditorPrivilegedStub> {
    private AuditorPrivilegedStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuditorPrivilegedStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuditorPrivilegedStub(channel, callOptions);
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
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service AuditorPrivileged.
   */
  public static final class AuditorPrivilegedBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<AuditorPrivilegedBlockingStub> {
    private AuditorPrivilegedBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuditorPrivilegedBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuditorPrivilegedBlockingStub(channel, callOptions);
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
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service AuditorPrivileged.
   */
  public static final class AuditorPrivilegedFutureStub
      extends io.grpc.stub.AbstractFutureStub<AuditorPrivilegedFutureStub> {
    private AuditorPrivilegedFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuditorPrivilegedFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuditorPrivilegedFutureStub(channel, callOptions);
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
  }

  private static final int METHODID_REGISTER_CERT = 0;
  private static final int METHODID_REGISTER_SECRET = 1;

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
        .build();
  }

  private static abstract class AuditorPrivilegedBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    AuditorPrivilegedBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.scalar.dl.rpc.ScalarProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("AuditorPrivileged");
    }
  }

  private static final class AuditorPrivilegedFileDescriptorSupplier
      extends AuditorPrivilegedBaseDescriptorSupplier {
    AuditorPrivilegedFileDescriptorSupplier() {}
  }

  private static final class AuditorPrivilegedMethodDescriptorSupplier
      extends AuditorPrivilegedBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    AuditorPrivilegedMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (AuditorPrivilegedGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new AuditorPrivilegedFileDescriptorSupplier())
              .addMethod(getRegisterCertMethod())
              .addMethod(getRegisterSecretMethod())
              .build();
        }
      }
    }
    return result;
  }
}
