package com.scalar.dl.client.tool;

import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "register-secret", description = "Register a specified secret.")
public class SecretRegistration extends AbstractClientCommand {

  @ArgGroup(exclusive = false)
  private NamespaceOptions namespaceOptions;

  static class NamespaceOptions {
    @Option(
        names = {"--namespace"},
        required = true,
        paramLabel = "NAMESPACE",
        description = "A namespace where the secret is registered.")
    String namespace;

    @Option(
        names = {"--entity-id"},
        required = true,
        paramLabel = "ENTITY_ID",
        description = "An entity ID for the secret.")
    String entityId;

    @Option(
        names = {"--secret-key"},
        required = true,
        paramLabel = "SECRET_KEY",
        description = "A secret key for HMAC authentication.")
    String secretKey;

    @Option(
        names = {"--secret-key-version"},
        paramLabel = "SECRET_KEY_VERSION",
        description = "A version of the secret key (default: ${DEFAULT-VALUE}).")
    int secretKeyVersion = 1;
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new SecretRegistration()).execute(args);
    System.exit(exitCode);
  }

  @Override
  protected Integer execute(ClientService service) throws ClientException {
    if (namespaceOptions != null) {
      service.registerSecret(
          namespaceOptions.namespace,
          namespaceOptions.entityId,
          namespaceOptions.secretKeyVersion,
          namespaceOptions.secretKey);
    } else {
      service.registerSecret();
    }
    Common.printOutput(null);
    return 0;
  }
}
