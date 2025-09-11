package com.scalar.dl.hashstore.client.tool;

import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.tool.Common;
import com.scalar.dl.client.tool.CommonOptions;
import com.scalar.dl.hashstore.client.service.ClientService;
import com.scalar.dl.hashstore.client.service.ClientServiceFactory;
import java.io.File;
import java.util.concurrent.Callable;

public abstract class AbstractHashStoreCommand extends CommonOptions implements Callable<Integer> {

  @Override
  public Integer call() throws Exception {
    return call(new ClientServiceFactory());
  }

  @VisibleForTesting
  public final Integer call(ClientServiceFactory factory) throws Exception {
    try {
      ClientService service =
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
  protected abstract Integer execute(ClientService service) throws ClientException;
}
