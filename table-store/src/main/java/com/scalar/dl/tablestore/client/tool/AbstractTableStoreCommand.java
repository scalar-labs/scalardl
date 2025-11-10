package com.scalar.dl.tablestore.client.tool;

import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.tool.Common;
import com.scalar.dl.client.tool.CommonOptions;
import com.scalar.dl.tablestore.client.service.TableStoreClientService;
import com.scalar.dl.tablestore.client.service.TableStoreClientServiceFactory;
import java.io.File;
import java.util.concurrent.Callable;

public abstract class AbstractTableStoreCommand extends CommonOptions implements Callable<Integer> {

  @Override
  public Integer call() throws Exception {
    return call(new TableStoreClientServiceFactory());
  }

  @VisibleForTesting
  public final Integer call(TableStoreClientServiceFactory factory) throws Exception {
    try {
      TableStoreClientService service =
          useGateway
              ? factory.create(new GatewayClientConfig(new File(properties)), false)
              : factory.create(new ClientConfig(new File(properties)), false);
      return execute(service);
    } catch (ClientException e) {
      Common.printError(e);
      printStackTrace(e);
      return 1;
    } finally {
      factory.close();
    }
  }

  /**
   * Executes the specific command logic.
   *
   * @param service the client service to use for execution.
   * @return the exit code.
   * @throws ClientException if the execution fails.
   */
  protected abstract Integer execute(TableStoreClientService service) throws ClientException;
}
