package com.scalar.dl.ledger.contract;

import com.scalar.dl.ledger.crypto.CertificateEntry;
import com.scalar.dl.ledger.crypto.ClientIdentityKey;
import com.scalar.dl.ledger.statemachine.DeprecatedLedgerReturnable;
import com.scalar.dl.ledger.statemachine.Ledger;
import com.scalar.dl.ledger.util.JsonpSerDe;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.json.JsonObject;

class DeprecatedContract extends ContractBase<JsonObject> {
  private static final JsonpSerDe serde = new JsonpSerDe();
  private final Contract contract;

  public DeprecatedContract(Contract contract) {
    this.contract = contract;
  }

  @Override
  final void initialize(ContractManager manager, ClientIdentityKey clientIdentityKey) {
    CertificateEntry.Key certificateKey =
        new CertificateEntry.Key(
            clientIdentityKey.getEntityId(), clientIdentityKey.getKeyVersion());
    contract.initialize(manager, certificateKey);
  }

  @Override
  JsonObject deserialize(String string) {
    return serde.deserialize(string);
  }

  @Override
  final void setRoot(boolean isRoot) {
    contract.setRoot(isRoot);
  }

  @Override
  public boolean isRoot() {
    return contract.isRoot();
  }

  @Override
  @Nullable
  public CertificateEntry.Key getCertificateKey() {
    return contract.getCertificateKey();
  }

  @Override
  public ClientIdentityKey getClientIdentityKey() {
    return contract.getCertificateKey();
  }

  @Nullable
  @Override
  String invokeRoot(Ledger<JsonObject> ledger, String argument, String properties) {
    JsonObject jsonProperties = properties == null ? null : serde.deserialize(properties);
    JsonObject result = invoke(ledger, serde.deserialize(argument), jsonProperties);
    return result == null ? null : serde.serialize(result);
  }

  @Override
  @Nullable
  public JsonObject invoke(
      Ledger<JsonObject> ledger, JsonObject argument, @Nullable JsonObject properties) {
    return contract.invoke(getLedger(ledger), argument, Optional.ofNullable(properties));
  }

  @Override
  @Nullable
  protected final JsonObject invoke(
      String contractId, Ledger<JsonObject> ledger, JsonObject argument) {
    return contract.invoke(contractId, getLedger(ledger), argument);
  }

  private com.scalar.dl.ledger.database.Ledger getLedger(Ledger<JsonObject> ledger) {
    DeprecatedLedgerReturnable deprecatedLedgerReturnable = (DeprecatedLedgerReturnable) ledger;
    return deprecatedLedgerReturnable.getDeprecatedLedger();
  }
}
