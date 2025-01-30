package com.scalar.dl.ledger.contract;

import com.scalar.dl.ledger.crypto.ClientIdentityKey;
import com.scalar.dl.ledger.statemachine.DeserializationType;
import com.scalar.dl.ledger.statemachine.Ledger;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class ContractMachine {
  private final ContractBase<?> contractBase;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public ContractMachine(Object contract) {
    if (contract instanceof Contract) {
      contractBase = new DeprecatedContract((Contract) contract);
    } else if (contract instanceof ContractBase) {
      this.contractBase = (ContractBase<?>) contract;
    } else {
      throw new IllegalArgumentException("unsupported contract type");
    }
  }

  /**
   * SpotBugs detects Bug Type "CT_CONSTRUCTOR_THROW" saying that "The object under construction
   * remains partially initialized and may be vulnerable to Finalizer attacks."
   */
  @Override
  protected final void finalize() {}

  public void initialize(ContractManager manager, ClientIdentityKey clientIdentityKey) {
    contractBase.initialize(manager, clientIdentityKey);
  }

  public void setRoot(boolean isRoot) {
    contractBase.setRoot(isRoot);
  }

  public boolean isRoot() {
    return contractBase.isRoot();
  }

  public ClientIdentityKey getClientIdentityKey() {
    return contractBase.getClientIdentityKey();
  }

  @Nullable
  public Object getContext() {
    return contractBase.getContext();
  }

  @SuppressWarnings("unchecked")
  @Nullable
  public <T> String invoke(Ledger<T> ledger, String argument, @Nullable String properties) {
    ContractBase<T> contract = (ContractBase<T>) contractBase;
    contract.setRoot(true);
    return contract.invokeRoot(ledger, argument, properties);
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public ContractBase<?> getContractBase() {
    return contractBase;
  }

  public DeserializationType getDeserializationType() {
    if (contractBase instanceof DeprecatedContract) {
      return DeserializationType.DEPRECATED;
    } else if (contractBase instanceof JsonpBasedContract) {
      return DeserializationType.JSONP_JSON;
    } else if (contractBase instanceof JacksonBasedContract) {
      return DeserializationType.JACKSON_JSON;
    } else if (contractBase instanceof StringBasedContract) {
      return DeserializationType.STRING;
    } else {
      throw new IllegalStateException("unsupported contract instance");
    }
  }
}
