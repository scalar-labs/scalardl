package com.scalar.dl.ledger.function;

import com.scalar.dl.ledger.database.Database;
import com.scalar.dl.ledger.error.LedgerError;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class FunctionMachine {
  private final FunctionBase<?, ?, ?, ?, ?, ?> functionBase;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public FunctionMachine(Object function) {
    if (function instanceof Function) {
      functionBase = new DeprecatedFunction((Function) function);
    } else if (function instanceof FunctionBase) {
      this.functionBase = (FunctionBase<?, ?, ?, ?, ?, ?>) function;
    } else {
      throw new IllegalArgumentException(LedgerError.UNSUPPORTED_FUNCTION.buildMessage());
    }
  }

  /**
   * SpotBugs detects Bug Type "CT_CONSTRUCTOR_THROW" saying that "The object under construction
   * remains partially initialized and may be vulnerable to Finalizer attacks."
   */
  @Override
  protected final void finalize() {}

  void initialize(FunctionManager manager) {
    functionBase.initialize(manager);
  }

  void setRoot(boolean isRoot) {
    functionBase.setRoot(isRoot);
  }

  public boolean isRoot() {
    return functionBase.isRoot();
  }

  public void setContractContext(@Nullable Object contractContext) {
    functionBase.setContractContext(contractContext);
  }

  @SuppressWarnings("unchecked")
  @Nullable
  public <T, G, S, P, D, R> String invoke(
      Database<G, S, P, D, R> database,
      @Nullable String functionArgument,
      String contractArgument,
      @Nullable String contractProperties) {
    FunctionBase<T, G, S, P, D, R> function = (FunctionBase<T, G, S, P, D, R>) functionBase;
    function.setRoot(true);
    function.setContractProperties(contractProperties);
    return function.invokeRoot(database, functionArgument, contractArgument);
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public FunctionBase<?, ?, ?, ?, ?, ?> getFunctionBase() {
    return functionBase;
  }
}
