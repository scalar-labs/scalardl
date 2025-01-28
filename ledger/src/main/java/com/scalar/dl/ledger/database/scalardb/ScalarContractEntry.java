package com.scalar.dl.ledger.database.scalardb;

import com.scalar.db.io.BigIntValue;
import com.scalar.db.io.BlobValue;
import com.scalar.db.io.IntValue;
import com.scalar.db.io.TextValue;
import com.scalar.dl.ledger.contract.ContractEntry;
import javax.annotation.concurrent.Immutable;

@Immutable
public class ScalarContractEntry {
  private final ContractEntry entry;

  public ScalarContractEntry(ContractEntry entry) {
    this.entry = entry;
  }

  /**
   * Returns a {@code TextValue} consisting of the id of the {@code ContractEntry}.
   *
   * @return a {@code TextValue} consisting of the id of the {@code ContractEntry}
   */
  public TextValue getIdValue() {
    return new TextValue(ContractEntry.ID, entry.getId());
  }

  /**
   * Returns a {@code TextValue} consisting of the binary name of the {@code ContractEntry}.
   *
   * @return a {@code TextValue} consisting of the binary name of the {@code ContractEntry}
   */
  public TextValue getBinaryNameValue() {
    return new TextValue(ContractEntry.BINARY_NAME, entry.getBinaryName());
  }

  /**
   * Returns a {@code TextValue} consisting of the entity id of the {@code ContractEntry}.
   *
   * @return a {@code TextValue} consisting of the entity id of the {@code ContractEntry}
   */
  public TextValue getEntityIdValue() {
    return new TextValue(ContractEntry.ENTITY_ID, entry.getEntityId());
  }

  /**
   * Returns a {@code TextValue} consisting of the certificate version of the {@code ContractEntry}.
   *
   * @return a {@code TextValue} consisting of the certificate version of the {@code ContractEntry}
   */
  public IntValue getKeyVersionValue() {
    return new IntValue(ContractEntry.KEY_VERSION, entry.getKeyVersion());
  }

  /**
   * Returns a {@code BlobValue} consisting of the bytecode of the contract contained in the {@code
   * ContractEntry}.
   *
   * @return a {@code BlobValue} consisting of the bytecode of the contract contained in the {@code
   *     ContractEntry}
   */
  public BlobValue getByteCodeValue() {
    return new BlobValue(ContractEntry.BYTE_CODE, entry.getByteCode());
  }

  /**
   * Returns a {@code TextValue} consisting of the properties (possibly null) of the {@code
   * ContractEntry}.
   *
   * @return a {@code BlobValue} consisting of the properties (possibly null) of the {@code
   *     ContractEntry}
   */
  public TextValue getPropertiesValue() {
    if (entry.getProperties().isPresent()) {
      return new TextValue(ContractEntry.PROPERTIES, entry.getProperties().get());
    }
    return new TextValue(ContractEntry.PROPERTIES, (String) null);
  }

  /**
   * Returns a {@code BigIntValue} consisting of the registered at time of the {@code
   * ContractEntry}.
   *
   * @return a {@code BigIntValue} consisting of the registered at time of the {@code ContractEntry}
   */
  public BigIntValue getRegisteredAtValue() {
    return new BigIntValue(ContractEntry.REGISTERED_AT, entry.getRegisteredAt());
  }

  /**
   * Returns a {@code BlobValue} consisting of the signature of the {@code ContractEntry}.
   *
   * @return a {@code BlobValue} consisting of the signature of the {@code ContractEntry}
   */
  public BlobValue getSignatureValue() {
    return new BlobValue(ContractEntry.SIGNATURE, entry.getSignature());
  }
}
