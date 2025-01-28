package com.scalar.dl.client.tool;

import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import java.io.File;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "register-function", description = "Register a specified function.")
public class FunctionRegistration extends CommonOptions implements Callable<Integer> {

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

  public static void main(String[] args) {
    int exitCode = new CommandLine(new FunctionRegistration()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    return call(new ClientServiceFactory());
  }

  @VisibleForTesting
  Integer call(ClientServiceFactory factory) throws Exception {
    ClientService service =
        useGateway
            ? factory.create(new GatewayClientConfig(new File(properties)))
            : factory.create(new ClientConfig(new File(properties)));
    return call(factory, service);
  }

  Integer call(ClientServiceFactory factory, ClientService service) {
    try {
      service.registerFunction(functionId, functionBinaryName, functionClassFile);
      Common.printOutput(null);
      return 0;
    } catch (ClientException e) {
      Common.printError(e);
      printStackTrace(e);
      return 1;
    } finally {
      factory.close();
    }
  }
}
