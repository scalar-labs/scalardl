package com.scalar.dl.hashstore.client.tool;

import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.tool.Common;
import com.scalar.dl.hashstore.client.service.HashStoreClientService;
import picocli.CommandLine.Command;

@Command(
    name = "bootstrap",
    description = "Bootstrap the hash store by registering identity and contracts.")
public class Bootstrap extends AbstractHashStoreCommand {

  @Override
  protected Integer execute(HashStoreClientService service) throws ClientException {
    service.bootstrap();
    Common.printOutput(null);
    return 0;
  }
}
