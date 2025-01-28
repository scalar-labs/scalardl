package com.scalar.dl.ledger.server;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer.Context;
import com.codahale.metrics.jmx.JmxReporter;
import com.google.common.base.CaseFormat;
import com.scalar.dl.ledger.config.ServerConfig;
import com.scalar.dl.ledger.util.CryptoUtils;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Stats {
  private static final Logger LOGGER = LoggerFactory.getLogger(Stats.class.getName());
  private static final String STATS_PREFIX = "stats";
  private static final String SUCCESS_SUFFIX = "success";
  private static final String FAILURE_SUFFIX = "failure";
  private static final TimerContext EMPTY_TIMER_CONTEXT = new TimerContext(null);
  private final MetricRegistry metricRegistry;
  private final String prefix;
  private final String totalSuccessLabel;
  private final String totalFailureLabel;

  public Stats(String productName, String serviceName) {
    this.metricRegistry = new MetricRegistry();
    this.prefix = name(productName, STATS_PREFIX, serviceName);
    this.totalSuccessLabel = name(prefix, "total", SUCCESS_SUFFIX);
    this.totalFailureLabel = name(prefix, "total", FAILURE_SUFFIX);
  }

  public void incrementCounter(String name, boolean isSucceeded) {
    String snake = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);
    metricRegistry
        .counter(name(prefix, snake, isSucceeded ? SUCCESS_SUFFIX : FAILURE_SUFFIX))
        .inc();
  }

  public void incrementTotalSuccess() {
    metricRegistry.counter(totalSuccessLabel).inc();
  }

  public long getTotalSuccess() {
    return metricRegistry.counter(totalSuccessLabel).getCount();
  }

  public void incrementTotalFailure() {
    metricRegistry.counter(totalFailureLabel).inc();
  }

  public long getTotalFailure() {
    return metricRegistry.counter(totalFailureLabel).getCount();
  }

  public TimerContext measureTime(String name) {
    String snake = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);
    return new TimerContext(metricRegistry.timer(name(prefix, snake)).time());
  }

  public void startJmxReporter() {
    JmxReporter reporter = JmxReporter.forRegistry(metricRegistry).build();
    reporter.start();
    LOGGER.info("JMX reporter started.");
    Runtime.getRuntime().addShutdownHook(new Thread(reporter::stop));
  }

  public void startPrometheusExporter(ServerConfig config) {
    try {
      CollectorRegistry.defaultRegistry.register(new DropwizardExports(metricRegistry));
      DefaultExports.initialize();

      Server server = createServer(config);
      ServletContextHandler context = new ServletContextHandler();
      context.setContextPath("/");
      server.setHandler(context);
      context.addServlet(new ServletHolder(new MetricsServlet()), "/stats/prometheus");
      server.setStopAtShutdown(true);
      server.start();
      LOGGER.info(
          "Prometheus exporter started, listening on {}"
              + (config.isServerTlsEnabled() ? " with TLS" : ""),
          config.getPrometheusExporterPort());
    } catch (Exception e) {
      LOGGER.error("Failed to start Prometheus exporter.", e);
    }
  }

  private Server createServer(ServerConfig config) {
    Server server = new Server();
    ServerConnector connector;
    if (config.isServerTlsEnabled()) {
      // Disable SNI host check to allow Prometheus to connect
      SecureRequestCustomizer secureRequestCustomizer = new SecureRequestCustomizer();
      secureRequestCustomizer.setSniHostCheck(false);
      HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory();
      httpConnectionFactory.getHttpConfiguration().addCustomizer(secureRequestCustomizer);

      connector =
          new ServerConnector(server, createSslContextFactory(config), httpConnectionFactory);
    } else {
      connector = new ServerConnector(server);
    }
    connector.setPort(config.getPrometheusExporterPort());
    server.setConnectors(new Connector[] {connector});
    return server;
  }

  private SslContextFactory createSslContextFactory(ServerConfig config) {
    // We can use a dummy password because the key store is not persisted
    String keyPassword = "Dummy_password1234!";

    SslContextFactory sslContextFactory = new SslContextFactory.Server();
    sslContextFactory.setKeyStore(
        CryptoUtils.createKeyStore(
            config.getServerTlsCertChainPath(), config.getServerTlsPrivateKeyPath(), keyPassword));
    sslContextFactory.setKeyStorePassword(keyPassword);
    return sslContextFactory;
  }

  public static TimerContext emptyTimerContext() {
    return EMPTY_TIMER_CONTEXT;
  }

  public static class TimerContext implements AutoCloseable {
    private final Context context;

    public TimerContext(Context context) {
      this.context = context;
    }

    @Override
    public void close() throws Exception {
      if (context != null) {
        context.close();
      }
    }
  }
}
