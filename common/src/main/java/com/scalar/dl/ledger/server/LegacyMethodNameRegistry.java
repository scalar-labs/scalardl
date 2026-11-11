package com.scalar.dl.ledger.server;

import com.google.common.collect.ImmutableMap;
import io.grpc.HandlerRegistry;
import io.grpc.MethodDescriptor;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * A {@link HandlerRegistry} that serves RPC methods that were renamed in the proto under their
 * former names, so that clients built with the former proto keep working against this server.
 *
 * <p>gRPC identifies a method on the wire by its full name (e.g., {@code
 * rpc.LedgerPrivileged/CreateNamespace}), so renaming an RPC in the proto is a wire-incompatible
 * change. This registry is meant to be set as the fallback handler registry of a server. When the
 * server receives a call under a former name, it is dispatched to the handler of the current name.
 *
 * <p>The former names can be removed once all the client versions that send them are no longer
 * supported.
 */
@Immutable
public class LegacyMethodNameRegistry extends HandlerRegistry {
  // Current method name -> former method name. The names do not include the service name.
  private static final ImmutableMap<String, String> LEGACY_METHOD_NAMES =
      ImmutableMap.of(
          "CreateNamespace", "createNamespace",
          "DropNamespace", "dropNamespace",
          "ListNamespaces", "listNamespaces");

  private final ImmutableMap<String, ServerMethodDefinition<?, ?>> methods;

  public LegacyMethodNameRegistry(ServerServiceDefinition service) {
    ImmutableMap.Builder<String, ServerMethodDefinition<?, ?>> builder = ImmutableMap.builder();
    String serviceName = service.getServiceDescriptor().getName();
    for (ServerMethodDefinition<?, ?> method : service.getMethods()) {
      String legacyName = LEGACY_METHOD_NAMES.get(method.getMethodDescriptor().getBareMethodName());
      if (legacyName != null) {
        String legacyFullName = MethodDescriptor.generateFullMethodName(serviceName, legacyName);
        builder.put(legacyFullName, withFullMethodName(method, legacyFullName));
      }
    }
    this.methods = builder.build();
  }

  @Nullable
  @Override
  public ServerMethodDefinition<?, ?> lookupMethod(String methodName, @Nullable String authority) {
    return methods.get(methodName);
  }

  private static <ReqT, RespT> ServerMethodDefinition<ReqT, RespT> withFullMethodName(
      ServerMethodDefinition<ReqT, RespT> method, String fullMethodName) {
    MethodDescriptor<ReqT, RespT> descriptor =
        method.getMethodDescriptor().toBuilder().setFullMethodName(fullMethodName).build();
    return ServerMethodDefinition.create(descriptor, method.getServerCallHandler());
  }
}
