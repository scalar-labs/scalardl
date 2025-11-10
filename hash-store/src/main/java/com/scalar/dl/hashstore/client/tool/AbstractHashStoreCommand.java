package com.scalar.dl.hashstore.client.tool;

import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.tool.Common;
import com.scalar.dl.client.tool.CommonOptions;
import com.scalar.dl.hashstore.client.service.HashStoreClientService;
import com.scalar.dl.hashstore.client.service.HashStoreClientServiceFactory;
import java.io.File;
import java.util.concurrent.Callable;

public abstract class AbstractHashStoreCommand extends CommonOptions implements Callable<Integer> {

  @Override
  public final Integer call() throws Exception {
    return call(new HashStoreClientServiceFactory());
  }

  @VisibleForTesting
  public final Integer call(HashStoreClientServiceFactory factory) throws Exception {
    try {
      HashStoreClientService service =
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
  protected abstract Integer execute(HashStoreClientService service) throws ClientException;
}
