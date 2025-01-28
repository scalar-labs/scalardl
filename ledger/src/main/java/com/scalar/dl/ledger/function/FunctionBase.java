package com.scalar.dl.ledger.function;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.scalar.dl.ledger.database.Database;
import com.scalar.dl.ledger.exception.ContractContextException;
import javax.annotation.Nullable;

abstract class FunctionBase<T, G, S, P, D, R> {
  private FunctionManager manager;
  protected T contractProperties;
  protected T contractContext;
  private boolean isRoot;

  void initialize(FunctionManager manager) {
    this.manager = checkNotNull(manager);
  }

  abstract void setContractProperties(@Nullable String contractProperties);

  @Nullable
  protected T getContractProperties() {
    return contractProperties;
  }

  void setRoot(boolean isRoot) {
    this.isRoot = isRoot;
  }

  public boolean isRoot() {
    return isRoot;
  }

  @SuppressWarnings("unchecked")
  void setContractContext(@Nullable Object contractContext) {
    this.contractContext = (T) contractContext;
  }

  /**
   * Returns a contract context given from a contract that is executed together with the function.
   *
   * @return contract context
   */
  @Nullable
  protected T getContractContext() {
    return contractContext;
  }

  @Nullable
  abstract String invokeRoot(
      Database<G, S, P, D, R> database, @Nullable String functionArgument, String contractArgument);

  /**
   * Invokes the function to {@link Database} with the specified argument and the corresponding root
   * contract argument and properties. An implementation of the {@code FunctionBase} should throw
   * {@link ContractContextException} if it faces application-level contextual error (such as lack
   * of balance in payment application).
   *
   * @param database mutable database
   * @param functionArgument function argument
   * @param contractArgument the argument of the corresponding root contract
   * @param contractProperties the pre-registered properties of the corresponding root contract
   */
  @Nullable
  public abstract T invoke(
      Database<G, S, P, D, R> database,
      @Nullable T functionArgument,
      T contractArgument,
      @Nullable T contractProperties);

  @SuppressWarnings("unchecked")
  @Nullable
  protected final T invoke(
      String functionId,
      Database<G, S, P, D, R> database,
      @Nullable T functionArgument,
      T contractArgument) {
    checkArgument(manager != null, "please call initialize() before this.");

    FunctionBase<T, G, S, P, D, R> function =
        (FunctionBase<T, G, S, P, D, R>) manager.getInstance(functionId).getFunctionBase();
    function.setContractContext(contractContext); // context is propagated to all the functions
    return function.invoke(database, functionArgument, contractArgument, contractProperties);
  }
}
