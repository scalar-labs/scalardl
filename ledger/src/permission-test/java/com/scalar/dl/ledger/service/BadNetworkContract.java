package com.scalar.dl.ledger.service;

import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.Ledger;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;

public class BadNetworkContract extends Contract {

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {
    try (Socket socket = new Socket("localhost", 12345)) {
      return Json.createObjectBuilder().add("connected", socket.isConnected()).build();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
