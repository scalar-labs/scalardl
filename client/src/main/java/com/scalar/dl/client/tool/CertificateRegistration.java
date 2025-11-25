package com.scalar.dl.client.tool;

import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "register-cert", description = "Register a specified certificate.")
public class CertificateRegistration extends AbstractClientCommand {

  public static void main(String[] args) {
    int exitCode = new CommandLine(new CertificateRegistration()).execute(args);
    System.exit(exitCode);
  }

  @Override
  protected Integer execute(ClientService service) throws ClientException {
    service.registerCertificate();
    Common.printOutput(null);
    return 0;
  }
}
