package com.scalar.dl.client.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Splitter;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.proof.AssetProof;
import java.io.File;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "validate-ledger", description = "Validate a specified asset in a ledger.")
public class LedgerValidation implements Callable<Integer> {

  @CommandLine.Option(
      names = {"--properties", "--config"},
      required = true,
      paramLabel = "PROPERTIES_FILE",
      description = "A configuration file in properties format.")
  private String properties;

  @CommandLine.Option(
      names = {"--asset-id"},
      required = true,
      paramLabel = "ASSET_ID",
      description =
          "The ID (and the ages) of an asset. Format: 'asset_id' or 'asset_id,start_age,end_age'")
  private List<String> assetIds;

  @CommandLine.Option(
      names = {"-h", "--help"},
      usageHelp = true,
      description = "display the help message.")
  boolean helpRequested;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new LedgerValidation()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    ClientServiceFactory factory = new ClientServiceFactory();
    ClientService service = factory.create(new ClientConfig(new File(properties)));
    ObjectMapper mapper = new ObjectMapper();

    try {
      assetIds.forEach(
          assetId -> {
            LedgerValidationResult result;
            List<String> idAndAges = Splitter.on(',').splitToList(assetId);
            if (idAndAges.size() != 3) {
              result = service.validateLedger(idAndAges.get(0));
            } else {
              result =
                  service.validateLedger(
                      idAndAges.get(0),
                      Integer.parseInt(idAndAges.get(1)),
                      Integer.parseInt(idAndAges.get(2)));
            }
            ObjectNode json =
                mapper.createObjectNode().put(Common.STATUS_CODE_KEY, result.getCode().toString());
            json.set("Ledger", getProof(mapper, result.getLedgerProof().orElse(null)));
            json.set("Auditor", getProof(mapper, result.getAuditorProof().orElse(null)));

            Common.printJson(json);
          });
      return 0;
    } catch (ClientException e) {
      Common.printError(e);
      return 1;
    } finally {
      factory.close();
    }
  }

  private JsonNode getProof(ObjectMapper mapper, @Nullable AssetProof proof) {
    if (proof == null) {
      return null;
    }

    return mapper
        .createObjectNode()
        .put("id", proof.getId())
        .put("age", proof.getAge())
        .put("nonce", proof.getNonce())
        .put("hash", Base64.getEncoder().encodeToString(proof.getHash()))
        .put("signature", Base64.getEncoder().encodeToString(proof.getSignature()));
  }
}
