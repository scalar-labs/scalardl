package com.scalar.dl.ledger.config;

import java.util.Collections;
import java.util.Map;

public interface ServerConfig {

  String getServiceName();

  String getName();

  String getNamespace();

  /**
   * Returns the options passed to ScalarDB when creating a namespace and its tables. The keys and
   * values are passed to ScalarDB as they are.
   *
   * @return an immutable map of the namespace creation options
   */
  default Map<String, String> getNamespaceCreationOptions() {
    return Collections.emptyMap();
  }

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
