package com.scalar.dl.ledger.database.scalardb;

import com.scalar.db.io.BigIntValue;
import com.scalar.db.io.BlobValue;
import com.scalar.db.io.TextValue;
import com.scalar.dl.ledger.function.FunctionEntry;

public class ScalarFunctionEntry {
  private final FunctionEntry entry;

  public ScalarFunctionEntry(FunctionEntry entry) {
    this.entry = entry;
  }

  /**
   * Returns a {@code TextValue} consisting of the id of the {@code FunctionEntry}.
   *
   * @return a {@code TextValue} consisting of the id of the {@code FunctionEntry}
   */
  public TextValue getIdValue() {
    return new TextValue(FunctionEntry.ID, entry.getId());
  }

  /**
   * Returns a {@code TextValue} consisting of the binary name of the {@code FunctionEntry}.
   *
   * @return a {@code TextValue} consisting of the binary name of the {@code FunctionEntry}
   */
  public TextValue getBinaryNameValue() {
    return new TextValue(FunctionEntry.BINARY_NAME, entry.getBinaryName());
  }

  /**
   * Returns a {@code BlobValue} consisting of the bytecode of the function contained in the {@code
   * FunctionEntry}.
   *
   * @return a {@code BlobValue} consisting of the bytecode of the function contained in the {@code
   *     FunctionEntry}
   */
  public BlobValue getByteCodeValue() {
    return new BlobValue(FunctionEntry.BYTE_CODE, entry.getByteCode());
  }

  /**
   * Returns a {@code BigIntValue} consisting of the registered at time of the {@code
   * FunctionEntry}.
   *
   * @return a {@code BigIntValue} consisting of the registered at time of the {@code FunctionEntry}
   */
  public BigIntValue getRegisteredAtValue() {
    return new BigIntValue(FunctionEntry.REGISTERED_AT, entry.getRegisteredAt());
  }
}
