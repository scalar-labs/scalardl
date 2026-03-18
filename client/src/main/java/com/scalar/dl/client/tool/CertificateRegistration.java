package com.scalar.dl.client.tool;

import static com.scalar.dl.client.util.Common.fileToString;

import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "register-cert", description = "Register a specified certificate.")
public class CertificateRegistration extends AbstractClientCommand {

  @ArgGroup(exclusive = false)
  private NamespaceOptions namespaceOptions;

  static class NamespaceOptions {
    @Option(
        names = {"--namespace"},
        required = true,
        paramLabel = "NAMESPACE",
        description = "A namespace where the certificate is registered.")
    String namespace;

    @Option(
        names = {"--entity-id"},
        required = true,
        paramLabel = "ENTITY_ID",
        description = "An entity ID for the certificate.")
    String entityId;

    @Option(
        names = {"--cert-path"},
        required = true,
        paramLabel = "CERT_PATH",
        description = "A path to the certificate file in PEM format.")
    String certPath;

    @Option(
        names = {"--cert-version"},
        paramLabel = "CERT_VERSION",
        description = "A version of the certificate (default: ${DEFAULT-VALUE}).")
    int certVersion = 1;
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new CertificateRegistration()).execute(args);
    System.exit(exitCode);
  }

  @Override
  protected Integer execute(ClientService service) throws ClientException {
    if (namespaceOptions != null) {
      String pem = fileToString(namespaceOptions.certPath);
      service.registerCertificate(
          namespaceOptions.namespace, namespaceOptions.entityId, namespaceOptions.certVersion, pem);
    } else {
      service.registerCertificate();
    }
    Common.printOutput(null);
    return 0;
  }
}
