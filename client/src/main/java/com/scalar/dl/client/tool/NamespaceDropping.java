package com.scalar.dl.client.tool;

import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import java.io.Console;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "drop-namespace", description = "Drop a namespace.")
public class NamespaceDropping extends AbstractClientCommand {

  @CommandLine.Option(
      names = {"--namespace"},
      required = true,
      paramLabel = "NAMESPACE",
      description = "A namespace name to drop.")
  private String namespace;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new NamespaceDropping()).execute(args);
    System.exit(exitCode);
  }

  @Override
  protected Integer execute(ClientService service) throws ClientException {
    if (!confirmDropping()) {
      return 1;
    }

    service.dropNamespace(namespace);
    Common.printOutput(null);
    return 0;
  }

  protected boolean confirmDropping() {
    Console console = System.console();
    if (console == null) {
      System.err.println("No console available.");
      return false;
    }

    System.out.print("Type namespace name to confirm: ");
    String confirmation = console.readLine();
    if (!namespace.equals(confirmation)) {
      System.err.println("Namespace name does not match. Aborting.");
      return false;
    }
    return true;
  }
}
