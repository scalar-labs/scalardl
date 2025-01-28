package com.scalar.dl.ledger.config;

public interface ServerConfig {

  String getServiceName();

  String getName();

  int getPort();

  int getPrivilegedPort();

  int getAdminPort();

  int getPrometheusExporterPort();

  int getDecommissioningDurationSecs();

  boolean isServerTlsEnabled();

  String getServerTlsCertChainPath();

  String getServerTlsPrivateKeyPath();

  GrpcServerConfig getGrpcServerConfig();
}
