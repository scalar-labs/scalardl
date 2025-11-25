package com.scalar.dl.tablestore.client.tool;

import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.tool.Common;
import com.scalar.dl.tablestore.client.service.TableStoreClientService;
import picocli.CommandLine.Command;

@Command(
    name = "bootstrap",
    description = "Bootstrap the table store by registering identity and contracts.")
public class Bootstrap extends AbstractTableStoreCommand {

  @Override
  protected Integer execute(TableStoreClientService service) throws ClientException {
    service.bootstrap();
    Common.printOutput(null);
    return 0;
  }
}
