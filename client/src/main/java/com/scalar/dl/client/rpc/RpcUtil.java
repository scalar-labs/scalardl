package com.scalar.dl.client.rpc;

import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.ledger.config.TargetConfig;
import com.scalar.dl.ledger.service.StatusCode;
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
          throw new ClientException("couldn't configure SSL.", e, StatusCode.RUNTIME_ERROR);
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
}
