package com.scalar.dl.client.rpc;

import com.scalar.dl.client.error.ClientError;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.ledger.config.TargetConfig;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.SSLException;

public class RpcUtil {

  public static void configureTls(NettyChannelBuilder builder, TargetConfig config) {
    if (config.isTargetTlsEnabled()) {
      if (config.getTargetTlsCaRootCert() != null) {
        // With a custom root CA certificate
        try {
          builder.sslContext(
              GrpcSslContexts.forClient()
                  .trustManager(
                      new ByteArrayInputStream(
                          config.getTargetTlsCaRootCert().getBytes(StandardCharsets.UTF_8)))
                  .build());
        } catch (SSLException e) {
          throw new ClientException(ClientError.CONFIGURING_SSL_FAILED, e, e.getMessage());
        }
      }
      // Use a certificate from the trusted CAs if it's not given

      if (config.getTargetTlsOverrideAuthority() != null) {
        builder.overrideAuthority(config.getTargetTlsOverrideAuthority());
      }
    } else {
      builder.usePlaintext();
    }
  }

  public static void configureHeader(NettyChannelBuilder builder, TargetConfig config) {
    if (config.getTargetAuthorizationCredential() != null) {
      // add an authorization header
      builder.intercept(new AuthorizationInterceptor(config.getTargetAuthorizationCredential()));
    }
  }

  public static void configureDataSize(NettyChannelBuilder builder, TargetConfig config) {
    if (config.getGrpcClientConfig().getMaxInboundMessageSize() > 0) {
      builder.maxInboundMessageSize(config.getGrpcClientConfig().getMaxInboundMessageSize());
    }
    if (config.getGrpcClientConfig().getMaxInboundMetadataSize() > 0) {
      builder.maxInboundMetadataSize(config.getGrpcClientConfig().getMaxInboundMetadataSize());
    }
  }
}
