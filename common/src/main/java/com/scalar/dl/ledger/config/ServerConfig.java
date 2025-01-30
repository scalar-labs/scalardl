package com.scalar.dl.ledger.config;

public interface ServerConfig {

  String getServiceName();

  String getName();

  int getPort();

  int getPrivilegedPort();

  int getAdminPort();

  int getPrometheusExporterPort();

  boolean isServerTlsEnabled();

  String getServerTlsCertChainPath();

  String getServerTlsPrivateKeyPath();
}
