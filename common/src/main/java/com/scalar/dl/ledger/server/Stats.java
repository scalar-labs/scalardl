package com.scalar.dl.ledger.server;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer.Context;
import com.codahale.metrics.jmx.JmxReporter;
import com.google.common.base.CaseFormat;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
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

  public void startPrometheusExporter(int port) {
    CollectorRegistry.defaultRegistry.register(new DropwizardExports(metricRegistry));
    DefaultExports.initialize();

    Server server = new Server(port);
    ServletContextHandler context = new ServletContextHandler();
    context.setContextPath("/");
    server.setHandler(context);
    context.addServlet(new ServletHolder(new MetricsServlet()), "/stats/prometheus");
    server.setStopAtShutdown(true);

    try {
      server.start();
      LOGGER.info("Prometheus exporter started, listening on {}.", port);
    } catch (Exception e) {
      LOGGER.error("Failed to start Prometheus exporter.", e);
    }
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
