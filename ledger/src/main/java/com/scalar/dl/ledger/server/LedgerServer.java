package com.scalar.dl.ledger.server;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.scalar.dl.ledger.config.LedgerConfig;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "scalar-ledger", description = "Starts ScalarDL ledger.")
public class LedgerServer implements Callable<Integer> {

  @CommandLine.Option(
      names = {"--properties", "--config"},
      required = true,
      paramLabel = "PROPERTIES_FILE",
      description = "A configuration file in properties format.")
  protected String properties;

  @Override
  public Integer call() throws Exception {
    startLedger();
    return 0;
  }

  private void startLedger() throws IOException, InterruptedException {
    LedgerConfig config = new LedgerConfig(new File(properties));
    Injector injector = Guice.createInjector(new LedgerServerModule(config));
    BaseServer server = new BaseServer(injector, config);

    server.start(LedgerService.class);
    server.startPrivileged(LedgerPrivilegedService.class);
    server.startAdmin(AdminService.class);

    server.addShutdownHook();
    server.blockUntilShutdown();
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new LedgerServer()).execute(args);
    System.exit(exitCode);
  }
}
