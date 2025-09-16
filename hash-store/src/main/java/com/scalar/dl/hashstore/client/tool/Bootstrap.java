package com.scalar.dl.hashstore.client.tool;

import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.hashstore.client.service.HashStoreClientService;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "bootstrap",
    description = "Bootstrap the hash store by registering identity and contracts.")
public class Bootstrap extends AbstractHashStoreCommand {

  public static void main(String[] args) {
    int exitCode = new CommandLine(new Bootstrap()).execute(args);
    System.exit(exitCode);
  }

  @Override
  protected Integer execute(HashStoreClientService service) throws ClientException {
    service.bootstrap();
    return 0;
  }
}
