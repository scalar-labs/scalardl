package com.scalar.dl.client.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import com.scalar.dl.ledger.service.StatusCode;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "smallbank-bench", description = "Execute smallbank workload concurrently.")
public class SmallbankBench implements Callable<Integer> {

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
      names = {"--duration"},
      required = false,
      paramLabel = "DURATION",
      description = "The duration of benchmark in seconds")
  private int duration = 200;

  @CommandLine.Option(
      names = {"--ramp-up-time"},
      required = false,
      paramLabel = "RAMP_UP_TIME",
      description = "The ramp up time in seconds.")
  private int rampUpTime = 60;

  @CommandLine.Option(
      names = {"--operation"},
      required = false,
      paramLabel = "OPERATION",
      description = "transact_savings, deposit_checking, send_payment, write_check, amalgamate")
  private List<String> operations = DEFAULT_OPERATIONS;

  @CommandLine.Option(
      names = {"-h", "--help"},
      usageHelp = true,
      description = "display the help message.")
  boolean helpRequested;

  private static final AtomicInteger counter = new AtomicInteger();
  private static final AtomicInteger totalCounter = new AtomicInteger();
  private static final AtomicLong latencyTotal = new AtomicLong();
  private static final AtomicInteger errorCounter = new AtomicInteger();
  private static final AtomicBoolean hasUnacceptableClientException = new AtomicBoolean();
  private static final List<String> DEFAULT_OPERATIONS =
      Arrays.asList(
          "transact_savings", "deposit_checking", "send_payment", "write_check", "amalgamate");

  public static void main(String[] args) {
    int exitCode = new CommandLine(new SmallbankBench()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    ClientServiceFactory factory = new ClientServiceFactory();
    ClientService service = factory.create(new ClientConfig(new File(properties)));

    long durationMillis = duration * 1000L;
    long rampUpTimeMillis = rampUpTime * 1000L;

    ObjectMapper mapper = new ObjectMapper();
    AtomicBoolean isRunning = new AtomicBoolean(true);
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    Random rand = new Random();
    final long start = System.currentTimeMillis();
    long from = start;
    for (int i = 0; i < numThreads; ++i) {
      executor.execute(
          () -> {
            while (isRunning.get()) {
              Request request = create(mapper, rand);
              try {
                long eachStart = System.currentTimeMillis();
                service.executeContract(request.getOperation(), request.getArgument());
                long eachEnd = System.currentTimeMillis();
                counter.incrementAndGet();
                if (System.currentTimeMillis() >= start + rampUpTimeMillis) {
                  totalCounter.incrementAndGet();
                  latencyTotal.addAndGet(eachEnd - eachStart);
                }
              } catch (ClientException e) {
                if (e.getStatusCode() != StatusCode.CONFLICT) {
                  hasUnacceptableClientException.set(true);
                  System.err.println(
                      "Unacceptable client exception: " + e.getStatusCode() + " " + e.getMessage());
                }
                errorCounter.incrementAndGet();
              }
            }
          });
    }

    long end = start + rampUpTimeMillis + durationMillis;
    while (true) {
      long to = System.currentTimeMillis();
      if (to >= end) {
        isRunning.set(false);
        break;
      }
      System.out.println(((double) counter.get() * 1000 / (to - from)) + " tps");
      counter.set(0);
      from = System.currentTimeMillis();

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        // ignore
      }
    }
    System.out.println(
        "TPS: " + (double) totalCounter.get() * 1000 / (end - start - rampUpTimeMillis));
    System.out.println("Average-Latency(ms): " + (double) latencyTotal.get() / totalCounter.get());
    System.out.println("Error-Counts: " + errorCounter.get());

    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);
    factory.close();

    return hasUnacceptableClientException.get() ? 1 : 0;
  }

  private Request create(ObjectMapper mapper, Random rand) {
    String operation = operations.get(rand.nextInt(operations.size()));

    int account1 = rand.nextInt(numAccounts);
    int account2 = rand.nextInt(numAccounts);
    if (account2 == account1) {
      account2 = (account2 + 1) % numAccounts;
    }
    int amount = rand.nextInt(100) + 1;

    ObjectNode node = mapper.createObjectNode();
    switch (operation) {
      case "transact_savings":
      case "deposit_checking":
      case "write_check":
        node.put("customer_id", account1);
        node.put("amount", amount);
        break;
      case "send_payment":
        node.put("source_customer_id", account1);
        node.put("dest_customer_id", account2);
        node.put("amount", amount);
        break;
      case "amalgamate":
        node.put("source_customer_id", account1);
        node.put("dest_customer_id", account2);
        break;
      default:
    }

    return new Request(operation, node);
  }

  private static class Request {
    private final String operation;
    private final JsonNode argument;

    public Request(String operation, JsonNode argument) {
      this.operation = operation;
      this.argument = argument;
    }

    public String getOperation() {
      return operation;
    }

    public JsonNode getArgument() {
      return argument;
    }
  }
}
