package com.scalar.dl.client.tool;

import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import java.io.File;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "register-function", description = "Register a specified function.")
public class FunctionRegistration implements Callable<Integer> {

  @CommandLine.Option(
      names = {"--properties", "--config"},
      required = true,
      paramLabel = "PROPERTIES_FILE",
      description = "A configuration file in properties format.")
  private String properties;

  @CommandLine.Option(
      names = {"--function-id"},
      required = true,
      paramLabel = "FUNCTION_ID",
      description = "An ID of a function to register.")
  private String functionId;

  @CommandLine.Option(
      names = {"--function-binary-name"},
      required = true,
      paramLabel = "FUNCTION_BINARY_NAME",
      description = "A binary name of a function to register.")
  private String functionBinaryName;

  @CommandLine.Option(
      names = {"--function-class-file"},
      required = true,
      paramLabel = "FUNCTION_CLASS_FILE",
      description = "A function class file to register.")
  private String functionClassFile;

  @CommandLine.Option(
      names = {"-h", "--help"},
      usageHelp = true,
      description = "display the help message.")
  boolean helpRequested;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new FunctionRegistration()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    ClientServiceFactory factory = new ClientServiceFactory();
    ClientService service = factory.create(new ClientConfig(new File(properties)));

    try {
      service.registerFunction(functionId, functionBinaryName, functionClassFile);
      Common.printOutput(null);
      return 0;
    } catch (ClientException e) {
      Common.printError(e);
      return 1;
    } finally {
      factory.close();
    }
  }
}
