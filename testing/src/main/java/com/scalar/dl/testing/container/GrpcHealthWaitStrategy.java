package com.scalar.dl.testing.container;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

/** Wait strategy that uses grpc_health_probe to check if a gRPC server is ready. */
public class GrpcHealthWaitStrategy extends AbstractWaitStrategy {
  private static final Logger logger = LoggerFactory.getLogger(GrpcHealthWaitStrategy.class);

  private static final String GRPC_HEALTH_PROBE_CMD = "grpc_health_probe";
  private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(2);

  private final int port;

  public GrpcHealthWaitStrategy(int port) {
    this.port = port;
    this.startupTimeout = DEFAULT_TIMEOUT;
  }

  @Override
  protected void waitUntilReady() {
    logger.info("Waiting for gRPC health check on port {} (timeout: {})", port, startupTimeout);

    try {
      Unreliables.retryUntilSuccess(
          (int) startupTimeout.getSeconds(),
          TimeUnit.SECONDS,
          () -> {
            getRateLimiter()
                .doWhenReady(
                    () -> {
                      try {
                        ExecResult result =
                            waitStrategyTarget.execInContainer(
                                GRPC_HEALTH_PROBE_CMD, "-addr=:" + port);

                        if (result.getExitCode() != 0) {
                          throw new RuntimeException(
                              "grpc_health_probe failed with exit code "
                                  + result.getExitCode()
                                  + ": "
                                  + result.getStderr());
                        }

                        logger.debug("gRPC health check passed");
                      } catch (Exception e) {
                        throw new RuntimeException("Health check failed", e);
                      }
                    });
            return true;
          });
    } catch (TimeoutException e) {
      throw new ContainerLaunchException(
          "Timed out waiting for gRPC server to be ready on port " + port);
    }

    logger.info("gRPC server is ready on port {}", port);
  }
}
