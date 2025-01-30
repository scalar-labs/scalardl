package com.scalar.dl.client.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.util.JacksonSerDe;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "register-contracts", description = "Register specified contracts.")
public class ContractsRegistration implements Callable<Integer> {
  static final String REGISTRATION_FAILED_CONTRACTS_TOML_FILE =
      "registration-failed-contracts.toml";
  static final String TOML_TABLES_NAME = "contracts";

  @CommandLine.Option(
      names = {"--properties", "--config"},
      required = true,
      paramLabel = "PROPERTIES_FILE",
      description = "A configuration file in properties format.")
  private String properties;

  @CommandLine.Option(
      names = {"--contracts-file"},
      required = true,
      paramLabel = "CONTRACTS_FILE",
      description = "A file including contracts to register in TOML format.")
  private String contractsFile;

  @CommandLine.Option(
      names = {"-h", "--help"},
      usageHelp = true,
      description = "display the help message.")
  boolean helpRequested;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new ContractsRegistration()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    ClientServiceFactory factory = new ClientServiceFactory();
    ClientService service = factory.create(new ClientConfig(new File(properties)));
    JacksonSerDe serde = new JacksonSerDe(new ObjectMapper());

    List<Toml> succeeded = new ArrayList<Toml>();
    List<Toml> alreadyRegistered = new ArrayList<Toml>();
    List<Toml> failed = new ArrayList<Toml>();

    try {
      new Toml()
          .read(new File(contractsFile))
          .getTables(TOML_TABLES_NAME)
          .forEach(
              each -> {
                String id = each.getString("contract-id");
                String binaryName = each.getString("contract-binary-name");
                String classFile = each.getString("contract-class-file");
                JsonNode properties = null;

                if (each.contains("contract-properties")) {
                  properties = serde.deserialize(each.getString("contract-properties"));
                } else if (each.contains("properties")) {
                  properties = serde.deserialize(each.getString("properties"));
                }

                try {
                  System.out.printf("Register contract %s as %s%n", binaryName, id);
                  service.registerContract(id, binaryName, classFile, properties);
                  Common.printOutput(null);
                  succeeded.add(each);
                } catch (ClientException e) {
                  Common.printError(e);
                  if (e.getStatusCode() == StatusCode.CONTRACT_ALREADY_REGISTERED) {
                    alreadyRegistered.add(each);
                  } else {
                    failed.add(each);
                  }
                }
              });
    } finally {
      factory.close();
    }

    System.out.printf("Registration succeeded: %d%n", succeeded.size());
    System.out.printf("Already registered: %d%n", alreadyRegistered.size());
    System.out.printf("Registration failed: %d%n", failed.size());

    if (!failed.isEmpty()) {
      try (OutputStreamWriter fileWriter =
          new OutputStreamWriter(
              new FileOutputStream(REGISTRATION_FAILED_CONTRACTS_TOML_FILE),
              StandardCharsets.UTF_8)) {
        TomlWriter tomlWriter = new TomlWriter();

        for (Toml toml : failed) {
          fileWriter.write(String.format("[[%s]]%n", TOML_TABLES_NAME));
          tomlWriter.write(toml.toMap(), fileWriter);
        }
      }

      System.out.printf(
          "%nContracts that failed to be registered are written to %s.%n",
          REGISTRATION_FAILED_CONTRACTS_TOML_FILE);

      return 1;
    }

    return 0;
  }
}
