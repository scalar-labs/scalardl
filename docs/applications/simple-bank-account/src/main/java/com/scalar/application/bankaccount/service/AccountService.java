package com.scalar.application.bankaccount.service;

import com.scalar.application.bankaccount.repository.AccountRepository;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.client.exception.ClientException;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
  private static final String ID = "id";
  private static final String AMOUNT = "amount";

  @Autowired private AccountRepository accountRepository;

  public ResponseEntity<String> create(String id) {
    JsonObject argument = Json.createObjectBuilder().add(ID, id).build();

    ThrowableFunction f = a -> accountRepository.create(a);
    return serve(f, argument);
  }

  public ResponseEntity<String> history(String id, int start, int end, int limit, String order) {
    JsonObjectBuilder argumentBuilder = Json.createObjectBuilder().add(ID, id);
    if (start > 0) {
      argumentBuilder.add("start", start);
    }
    if (end > 0) {
      argumentBuilder.add("end", end);
    }
    if (limit > 0) {
      argumentBuilder.add("limit", limit);
    }
    if (order.equals("asc")) {
      argumentBuilder.add("order", order);
    }

    ThrowableFunction f = a -> accountRepository.history(a);
    return serve(f, argumentBuilder.build());
  }

  public ResponseEntity<String> deposit(String id, long amount) {
    JsonObject argument = Json.createObjectBuilder().add(ID, id).add(AMOUNT, amount).build();

    ThrowableFunction f = a -> accountRepository.deposit(a);
    return serve(f, argument);
  }

  public ResponseEntity<String> withdraw(String id, long amount) {
    JsonObject argument = Json.createObjectBuilder().add(ID, id).add(AMOUNT, amount).build();

    ThrowableFunction f = a -> accountRepository.withdraw(a);
    return serve(f, argument);
  }

  public ResponseEntity<String> transfer(String from, String to, long amount) {
    JsonObject argument =
        Json.createObjectBuilder().add("from", from).add("to", to).add(AMOUNT, amount).build();

    ThrowableFunction f = a -> accountRepository.transfer(a);
    return serve(f, argument);
  }

  private ResponseEntity<String> serve(ThrowableFunction f, JsonObject json) {
    try {
      ContractExecutionResult result = f.apply(json);

      return ResponseEntity
              .ok(result.getResult().isPresent() ? result.getResult().get().toString() : "{}");
    } catch (ClientException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Json.createObjectBuilder()
                      .add("status", e.getStatusCode().toString())
                      .add("message", e.getMessage()).build().toString());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }
  }

  @FunctionalInterface
  private interface ThrowableFunction {
    ContractExecutionResult apply(JsonObject json) throws Exception;
  }
}
