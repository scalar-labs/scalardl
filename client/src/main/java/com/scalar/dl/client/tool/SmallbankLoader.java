package com.scalar.dl.client.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import com.scalar.dl.ledger.service.StatusCode;
import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "smallbank-loader", description = "Create accounts for smallbank workload.")
public class SmallbankLoader implements Callable<Integer> {

  @CommandLine.Option(
      names = {"--properties", "--config"},
      required = true,
      paramLabel = "PROPERTIES_FILE",
      description = "A configuration file in properties format.")
  private String properties;

  @CommandLine.Option(
      names = {"--num-accounts"},
      required = false,
      paramLabel = "NUM_ACCOUNTS",
      description = "The number of target accounts.")
  private int numAccounts = 10000;

  @CommandLine.Option(
      names = {"--num-threads"},
      required = false,
      paramLabel = "NUM_THREADS",
      description = "The number of threads to run.")
  private int numThreads = 1;

  @CommandLine.Option(
      names = {"-h", "--help"},
      usageHelp = true,
      description = "display the help message.")
  boolean helpRequested;

  private static final AtomicInteger counter = new AtomicInteger(0);
  private static final AtomicBoolean hasUnacceptableClientException = new AtomicBoolean();
  private static final String contractId = "create_account";
  private static final int DEFAULT_BALANCE = 100000;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new SmallbankLoader()).execute(args);
    System.exit(exitCode);
  }

  @SuppressWarnings("CatchAndPrintStackTrace")
  @Override
  public Integer call() throws Exception {
    ClientServiceFactory factory = new ClientServiceFactory();
    ClientService service = factory.create(new ClientConfig(new File(properties)));

    ObjectMapper mapper = new ObjectMapper();
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    for (int i = 0; i < numThreads; ++i) {
      executor.execute(
          () -> {
            while (true) {
              int id = counter.getAndIncrement();
              JsonNode jsonArgument =
                  mapper
                      .createObjectNode()
                      .put("customer_id", id)
                      .put("customer_name", "Name " + id)
                      .put("initial_checking_balance", DEFAULT_BALANCE)
                      .put("initial_savings_balance", DEFAULT_BALANCE);

              try {
                if (counter.get() > numAccounts) {
                  break;
                }
                service.executeContract(contractId, jsonArgument);
              } catch (ClientException e) {
                if (e.getStatusCode() != StatusCode.CONFLICT) {
                  hasUnacceptableClientException.set(true);
                  System.err.println(
                      "Unacceptable client exception: " + e.getStatusCode() + " " + e.getMessage());
                }
                e.printStackTrace();
              }
            }
          });
    }

    while (counter.get() <= numAccounts) {
      System.out.println(counter.get() + " assets are loaded.");

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        // ignore
      }
    }

    factory.close();
    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);

    return hasUnacceptableClientException.get() ? 1 : 0;
  }
}
