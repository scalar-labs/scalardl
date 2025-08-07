package com.scalar.dl.client.tool;

import picocli.CommandLine;

/**
 * This is the class to define options in common that can be reused by the subcommands. To reuse
 * these options, a subcommand class can simply inherit {@link CommonOptions} by using the keyword
 * "extends", e.g., "public class Subcommand extends CommonOptions".
 *
 * <p>ref. https://picocli.info/#_subclassing
 */
public class CommonOptions {

  @CommandLine.Option(
      names = {"-h", "--help"},
      usageHelp = true,
      description = "display the help message.")
  protected boolean helpRequested;

  @CommandLine.Option(
      names = {"--properties", "--config"},
      required = true,
      paramLabel = "PROPERTIES_FILE",
      description = "A configuration file in properties format.")
  protected String properties;

  @CommandLine.Option(
      names = {"--stacktrace"},
      description = "output Java Stack Trace to stderr stream.")
  protected boolean stacktraceEnabled;

  @CommandLine.Option(
      names = {"-g", "--use-gateway"},
      paramLabel = "USE_GATEWAY",
      defaultValue = "false",
      description = "A flag to use the gateway.")
  protected boolean useGateway;

  /**
   * Outputs Java stack trace to stderr stream by using {@link Exception#printStackTrace()} when the
   * stacktrace option is set. Otherwise, we don't display the stack trace.
   *
   * @param e Exception to display if the stack trace option is set to true.
   */
  public void printStackTrace(Exception e) {
    if (stacktraceEnabled) {
      e.printStackTrace();
    }
  }
}
