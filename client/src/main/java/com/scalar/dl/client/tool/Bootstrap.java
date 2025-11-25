package com.scalar.dl.client.tool;

import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import picocli.CommandLine.Command;

@Command(
    name = "bootstrap",
    description = "Bootstrap the ledger by registering identity and system contracts.")
public class Bootstrap extends AbstractClientCommand {

  @Override
  protected Integer execute(ClientService service) throws ClientException {
    service.bootstrap();
    Common.printOutput(null);
    return 0;
  }
}
