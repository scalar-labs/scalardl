package com.scalar.dl.client.tool;

import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import java.io.File;
import java.util.concurrent.Callable;

public abstract class AbstractClientCommand extends CommonOptions implements Callable<Integer> {

  @Override
  public final Integer call() throws Exception {
    return call(new ClientServiceFactory());
  }

  @VisibleForTesting
  public final Integer call(ClientServiceFactory factory) throws Exception {
    try {
      ClientService service =
          useGateway
              ? factory.create(new GatewayClientConfig(new File(properties)))
              : factory.create(new ClientConfig(new File(properties)));
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
