package com.scalar.dl.client.tool;

import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "register-secret", description = "Register a specified secret.")
public class SecretRegistration extends AbstractClientCommand {

  public static void main(String[] args) {
    int exitCode = new CommandLine(new SecretRegistration()).execute(args);
    System.exit(exitCode);
  }

  @Override
  protected Integer execute(ClientService service) throws ClientException {
    service.registerSecret();
    Common.printOutput(null);
    return 0;
  }
}
