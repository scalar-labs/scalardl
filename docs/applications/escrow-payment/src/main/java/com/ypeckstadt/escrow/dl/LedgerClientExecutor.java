package com.ypeckstadt.escrow.dl;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientModule;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import java.io.File;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public abstract class LedgerClientExecutor {

  private static final Logger LOG = LogManager.getLogger(LedgerClientExecutor.class);
  private static final String CLIENT_PROPERTIES = "client.properties";

  private ClientConfig loadClientConfig() throws Exception {
    File file = new File(System.getProperty("user.dir") + File.separator + CLIENT_PROPERTIES);
    return new ClientConfig(file);
  }

  protected ContractExecutionResult executeContract(
      String contractName, JsonObject contractParameter, boolean useCertHolderIdSuffix)
      throws Exception {
    ClientConfig config = loadClientConfig();

    // Custom naming convention to make sure the contract Id is unique, not required and contractId
    // can be anything
    // just make sure the contractId is set to the same value as specified when registering the
    // contract on the ledger
    String contractId =
        useCertHolderIdSuffix ? contractName + "_" + config.getCertHolderId() : contractName;

    Injector injector = Guice.createInjector(new ClientModule(config));
    ClientService service = injector.getInstance(ClientService.class);
    JsonObject object =
        (contractParameter != null) ? contractParameter : Json.createObjectBuilder().build();
    return service.executeContract(contractId, object);
  }

  /**
   * Pretty print the json to the standard output
   *
   * @param json the json to print
   */
  protected void prettyPrintJson(JsonObject jsonObject) {
    if (jsonObject != null) {
      System.out.println("[Return]");
      Map<String, Object> config = new HashMap<>(1);
      config.put(JsonGenerator.PRETTY_PRINTING, true);
      JsonWriterFactory factory = Json.createWriterFactory(config);
      JsonWriter writer = factory.createWriter(System.out);
      writer.writeObject(jsonObject);
      System.out.println("");
      writer.close();
    }
  }
}
