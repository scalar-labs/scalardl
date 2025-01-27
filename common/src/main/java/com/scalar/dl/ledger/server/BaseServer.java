package com.scalar.dl.ledger.server;

import com.google.inject.Injector;
import com.scalar.dl.ledger.config.ServerConfig;
import io.grpc.BindableService;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseServer {
  private static final Logger LOGGER = LoggerFactory.getLogger(BaseServer.class.getName());
  private static final long MAX_WAIT_TIME_SECONDS = 60;
  private final Injector injector;
  private final ServerConfig config;
  private io.grpc.Server server;
  private io.grpc.Server privilegedServer;
  private io.grpc.Server adminServer;

  public BaseServer(Injector injector, ServerConfig config) {
    this.injector = injector;
    this.config = config;
  }

  /** A server for users. This server can be exposed to normal users. */
  public void start(Class<? extends BindableService> clazz) throws IOException {
    ServerBuilder<?> builder =
        ServerBuilder.forPort(config.getPort())
            .addService(injector.getInstance(clazz))
            .addService(new HealthService())
            .addService(ProtoReflectionService.newInstance());

    if (config.isServerTlsEnabled()) {
      builder.useTransportSecurity(
          new File(config.getServerTlsCertChainPath()),
          new File(config.getServerTlsPrivateKeyPath()));
    }

    Stats stats = injector.getInstance(Stats.class);
    stats.startJmxReporter();
    if (config.getPrometheusExporterPort() >= 0) {
      stats.startPrometheusExporter(config.getPrometheusExporterPort());
    }

    server = builder.build().start();
    log(clazz, config.getName(), config.getPort(), config.isServerTlsEnabled());
  }

  /**
   * A privileged server for privileged users. This server should be exposed to only privileged
   * users who are authorized to call the service.
   */
  public void startPrivileged(Class<? extends BindableService> clazz) throws IOException {
    ServerBuilder<?> builder =
        ServerBuilder.forPort(config.getPrivilegedPort())
            .addService(injector.getInstance(clazz))
            .addService(new HealthService())
            .addService(ProtoReflectionService.newInstance());

    if (config.isServerTlsEnabled()) {
      builder.useTransportSecurity(
          new File(config.getServerTlsCertChainPath()),
          new File(config.getServerTlsPrivateKeyPath()));
    }

    privilegedServer = builder.build().start();
    log(clazz, config.getName(), config.getPrivilegedPort(), config.isServerTlsEnabled());
  }

  /**
   * A admin server for admins. This server should be exposed to only admins who are in charge of
   * the entire service.
   */
  public void startAdmin(Class<? extends BindableService> clazz) throws IOException {
    ServerBuilder<?> builder =
        ServerBuilder.forPort(config.getAdminPort())
            .addService(injector.getInstance(clazz))
            .addService(new HealthService())
            .addService(ProtoReflectionService.newInstance());

    if (config.isServerTlsEnabled()) {
      builder.useTransportSecurity(
          new File(config.getServerTlsCertChainPath()),
          new File(config.getServerTlsPrivateKeyPath()));
    }

    adminServer = builder.build().start();
    log(clazz, config.getName(), config.getAdminPort(), config.isServerTlsEnabled());
  }

  public void addShutdownHook() {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  LOGGER.info("Signal received. Shutting down the server ...");
                  try {
                    this.stop();
                  } catch (InterruptedException e) {
                    LOGGER.warn("Interrupt received during stopping the servers.", e);
                  }
                  LOGGER.info("The server shut down.");
                }));
  }

  /** Await termination on the main thread since the grpc library uses daemon threads. */
  public void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
    if (privilegedServer != null) {
      privilegedServer.awaitTermination();
    }
    if (adminServer != null) {
      adminServer.awaitTermination();
    }
  }

  private void stop() throws InterruptedException {
    if (server != null) {
      server.shutdown();
    }
    if (privilegedServer != null) {
      privilegedServer.shutdown();
    }
    if (adminServer != null) {
      adminServer.shutdown();
    }
    // no more incoming requests are accepted after this

    if (server != null) {
      server.awaitTermination(MAX_WAIT_TIME_SECONDS, TimeUnit.SECONDS);
    }
    if (privilegedServer != null) {
      privilegedServer.awaitTermination(0, TimeUnit.SECONDS);
    }
    if (adminServer != null) {
      adminServer.awaitTermination(0, TimeUnit.SECONDS);
    }
  }

  private void log(
      Class<? extends BindableService> clazz, String name, int port, boolean isTlsEnabled) {
    LOGGER.info(
        clazz.getSimpleName()
            + " for \""
            + name
            + "\" started with TLS = "
            + isTlsEnabled
            + ", listening on "
            + port);
  }
}
