package com.scalar.dl.ledger.contract;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.scalar.dl.ledger.crypto.CertificateEntry;
import com.scalar.dl.ledger.database.Ledger;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.DeprecatedLedger;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.json.JsonObject;

/**
 * An invokable contract which users can create their contracts based on.
 *
 * @author Hiroyuki Yamada
 * @deprecated This class will be package-private and won't be available to users in release 5.0.0.
 */
@Deprecated
public abstract class Contract {
  private ContractManager manager;
  private CertificateEntry.Key certificateKey;
  private boolean isRoot;

  final void initialize(ContractManager manager, @Nullable CertificateEntry.Key certificateKey) {
    this.manager = checkNotNull(manager);
    this.certificateKey = certificateKey;
    this.isRoot = false;
  }

  final void setRoot(boolean isRoot) {
    this.isRoot = isRoot;
  }

  public boolean isRoot() {
    return isRoot;
  }

  @Nullable
  public CertificateEntry.Key getCertificateKey() {
    return certificateKey;
  }

  /**
   * Invokes the contract to {@link Ledger} with the specified argument and the pre-registered
   * contract properties. An implementation of the {@code Contract} should throw {@link
   * ContractContextException} if it faces application-level contextual error (such as lack of
   * balance in payment application).
   *
   * @param ledger tamper-evident ledger
   * @param argument json-formatted argument
   * @param properties json-formatted pre-registered contract properties
   * @return json-formatted result
   */
  public abstract JsonObject invoke(
      Ledger ledger, JsonObject argument, Optional<JsonObject> properties);

  /**
   * Invokes the specified contract to {@link Ledger} with the specified argument.
   *
   * @param contractId another contract to invoke
   * @param ledger tamper-evident ledger
   * @param argument json-formatted result
   * @return json-formatted result
   */
  protected final JsonObject invoke(String contractId, Ledger ledger, JsonObject argument) {
    checkArgument(manager != null, "please call initialize() before this.");

    ContractEntry.Key key = new ContractEntry.Key(contractId, certificateKey);
    ContractEntry entry = manager.get(key);
    DeprecatedContract contract = (DeprecatedContract) manager.getInstance(entry).getContractBase();
    DeprecatedLedger deprecatedLedger = new DeprecatedLedger(ledger);
    JsonObject properties = entry.getProperties().map(contract::deserialize).orElse(null);
    return contract.invoke(deprecatedLedger, argument, properties);
  }
}
