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

  @CommandLine.Option(
      names = {"--for-import"},
      paramLabel = "FOR_IMPORT",
      description =
          "A flag to create the namespace to hold data imported from another ScalarDL deployment. "
              + "This only marks the namespace as such and does not import any data.")
  private boolean forImport;

  @Override
  protected Integer execute(ClientService service) throws ClientException {
    service.createNamespace(namespace, forImport);
    Common.printOutput(null);
    return 0;
  }
}
