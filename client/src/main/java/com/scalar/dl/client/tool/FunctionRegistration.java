package com.scalar.dl.client.tool;

import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "register-function", description = "Register a specified function.")
public class FunctionRegistration extends AbstractClientCommand {

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
  protected Integer execute(ClientService service) throws ClientException {
    service.registerFunction(functionId, functionBinaryName, functionClassFile);
    Common.printOutput(null);
    return 0;
  }
}
