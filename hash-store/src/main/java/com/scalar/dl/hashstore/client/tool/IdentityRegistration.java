package com.scalar.dl.hashstore.client.tool;

import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.hashstore.client.service.ClientService;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "register-identity",
    description = "Register identity and contracts for the hash store.")
public class IdentityRegistration extends AbstractHashStoreCommand {

  public static void main(String[] args) {
    int exitCode = new CommandLine(new IdentityRegistration()).execute(args);
    System.exit(exitCode);
  }

  @Override
  protected Integer execute(ClientService service) throws ClientException {
    service.registerIdentity();
    return 0;
  }
}
