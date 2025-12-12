package com.scalar.dl.client.tool;

import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "create-namespace", description = "Create a namespace.")
public class NamespaceCreation extends AbstractClientCommand {

  @CommandLine.Option(
      names = {"--namespace"},
      required = true,
      paramLabel = "NAMESPACE",
      description = "A namespace name to create.")
  private String namespace;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new NamespaceCreation()).execute(args);
    System.exit(exitCode);
  }

  @Override
  protected Integer execute(ClientService service) throws ClientException {
    service.createNamespace(namespace);
    Common.printOutput(null);
    return 0;
  }
}
