package com.scalar.dl.client.rpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

public class AuthorizationInterceptor implements ClientInterceptor {
  private static final String KEY = "authorization";
  private final String token;

  public AuthorizationInterceptor(String token) {
    this.token = token;
  }

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {
    return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
        channel.newCall(methodDescriptor, callOptions)) {
      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        headers.put(Metadata.Key.of(KEY, Metadata.ASCII_STRING_MARSHALLER), token);
        super.start(responseListener, headers);
      }
    };
  }
}
