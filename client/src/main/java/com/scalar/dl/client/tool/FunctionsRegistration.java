package com.scalar.dl.client.tool;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import com.scalar.dl.client.error.ClientError;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "register-functions", description = "Register specified functions.")
public class FunctionsRegistration extends AbstractClientCommand {
  static final String REGISTRATION_FAILED_FUNCTIONS_TOML_FILE =
      "registration-failed-functions.toml";
  static final String TOML_TABLES_NAME = "functions";

  @CommandLine.Option(
      names = {"--functions-file"},
      required = true,
      paramLabel = "FUNCTIONS_FILE",
      description = "A file including functions to register in TOML format.")
  private String functionsFile;

  public static void main(String[] args) {
    int exitCode =
        new CommandLine(new com.scalar.dl.client.tool.FunctionsRegistration()).execute(args);
    System.exit(exitCode);
  }

  @Override
  protected Integer execute(ClientService service) throws ClientException {
    List<Toml> succeeded = new ArrayList<>();
    List<Toml> failed = new ArrayList<>();

    new Toml()
        .read(new File(functionsFile))
        .getTables(TOML_TABLES_NAME)
        .forEach(
            each -> {
              String id = each.getString("function-id");
              String binaryName = each.getString("function-binary-name");
              String classFile = each.getString("function-class-file");

              // All of them are required values to register a function.
              // Thus, the malformed table is skipped if one of them doesn't exist.
              if (id == null || binaryName == null || classFile == null) {
                failed.add(each);
                return;
              }

              try {
                System.out.printf("Register function %s as %s%n", binaryName, id);
                service.registerFunction(id, binaryName, classFile);
                Common.printOutput(null);
                succeeded.add(each);
              } catch (ClientException e) {
                Common.printError(e);
                printStackTrace(e);
                failed.add(each);
              }
            });

    System.out.printf("Registration succeeded: %d%n", succeeded.size());
    System.out.printf("Registration failed: %d%n", failed.size());

    if (!failed.isEmpty()) {
      try (OutputStreamWriter fileWriter =
          new OutputStreamWriter(
              Files.newOutputStream(Paths.get(REGISTRATION_FAILED_FUNCTIONS_TOML_FILE)),
              StandardCharsets.UTF_8)) {
        TomlWriter tomlWriter = new TomlWriter();

        for (Toml toml : failed) {
          fileWriter.write(String.format("[[%s]]%n", TOML_TABLES_NAME));
          tomlWriter.write(toml.toMap(), fileWriter);
        }
      } catch (IOException e) {
        throw new ClientException(ClientError.WRITING_RESULT_TO_FILE_FAILED, e, e.getMessage());
      }

      System.out.printf(
          "%nFunctions that failed to be registered are written to %s.%n",
          REGISTRATION_FAILED_FUNCTIONS_TOML_FILE);

      return 1;
    }

    return 0;
  }
}
