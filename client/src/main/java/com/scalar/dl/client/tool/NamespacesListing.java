package com.scalar.dl.client.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.ledger.util.JacksonSerDe;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "list-namespaces", description = "List namespaces.")
public class NamespacesListing extends AbstractClientCommand {

  @CommandLine.Option(
      names = {"--pattern"},
      required = false,
      paramLabel = "PATTERN",
      description = "A pattern to filter namespaces (partial match).")
  private String pattern;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new NamespacesListing()).execute(args);
    System.exit(exitCode);
  }

  @Override
  protected Integer execute(ClientService service) throws ClientException {
    JacksonSerDe serde = new JacksonSerDe(new ObjectMapper());
    String result = service.listNamespaces(pattern);
    Common.printOutput(serde.deserialize(result));
    return 0;
  }
}
