package com.scalar.dl.ledger.contract;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.scalar.dl.ledger.crypto.CertificateEntry;
import com.scalar.dl.ledger.crypto.ClientIdentityKey;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Ledger;
import javax.annotation.Nullable;

abstract class ContractBase<T> {
  private ContractManager manager;
  private ClientIdentityKey clientIdentityKey;
  private T context;
  private boolean isRoot;

  void initialize(ContractManager manager, ClientIdentityKey clientIdentityKey) {
    this.manager = checkNotNull(manager);
    this.clientIdentityKey = clientIdentityKey;
    this.isRoot = false;
  }

  abstract T deserialize(String string);

  void setRoot(boolean isRoot) {
    this.isRoot = isRoot;
  }

  public boolean isRoot() {
    return isRoot;
  }

  /**
   * This method will be deleted in 5.0.0. Note that, although it returns {@link
   * CertificateEntry.Key}, but it returns the entity ID and key version of a certificate or HMAC
   * secret key tied to the contract.
   */
  @Deprecated
  public CertificateEntry.Key getCertificateKey() {
    return new CertificateEntry.Key(
        clientIdentityKey.getEntityId(), clientIdentityKey.getKeyVersion());
  }

  public ClientIdentityKey getClientIdentityKey() {
    return clientIdentityKey;
  }

  /**
   * Sets a contract runtime context to pass it to functions. Setting a new context overwrites the
   * previous one.
   *
   * @param context a contract runtime context
   */
  protected void setContext(@Nullable T context) {
    this.context = context;
  }

  T getContext() {
    return context;
  }

  @Nullable
  abstract String invokeRoot(Ledger<T> ledger, String argument, String properties);

  /**
   * Invokes the contract to {@link Ledger} with the specified argument and the pre-registered
   * contract properties. An implementation of the {@code ContractBase} should throw {@link
   * ContractContextException} if it faces application-level contextual error (such as lack of
   * balance in payment application).
   *
   * @param ledger tamper-evident ledger
   * @param argument contract argument
   * @param properties pre-registered contract properties
   * @return json-formatted result
   */
  @Nullable
  public abstract T invoke(Ledger<T> ledger, T argument, @Nullable T properties);

  /**
   * Invokes the specified contract to {@link Ledger} with the specified argument.
   *
   * @param contractId another contract ID to invoke
   * @param ledger tamper-evident ledger
   * @param argument contract argument
   * @return contract execution result
   */
  @SuppressWarnings("unchecked")
  @Nullable
  protected T invoke(String contractId, Ledger<T> ledger, T argument) {
    checkArgument(manager != null, "please call initialize() before this.");

    ContractEntry.Key key = new ContractEntry.Key(contractId, clientIdentityKey);
    ContractEntry entry = manager.get(key);
    ContractBase<T> contract = (ContractBase<T>) manager.getInstance(entry).getContractBase();
    T properties = entry.getProperties().map(this::deserialize).orElse(null);
    T result = contract.invoke(ledger, argument, properties);
    setContext(contract.getContext()); // context is propagated to the root
    return result;
  }
}
