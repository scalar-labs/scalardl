package com.scalar.dl.ledger.function;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.scalar.dl.ledger.database.FunctionRegistry;
import com.scalar.dl.ledger.error.CommonLedgerError;
import com.scalar.dl.ledger.exception.MissingFunctionException;
import com.scalar.dl.ledger.exception.UnloadableFunctionException;
import java.util.Optional;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class FunctionManager {
  private final FunctionRegistry registry;
  private final FunctionLoader loader;

  @Inject
  public FunctionManager(FunctionRegistry registry, FunctionLoader loader) {
    this.registry = registry;
    this.loader = loader;
  }

  public void register(FunctionEntry entry) {
    // verify if a specified function is loadable.
    getInstance(entry);

    registry.bind(entry);
  }

  @VisibleForTesting
  FunctionEntry get(String id) {
    Optional<FunctionEntry> entry = registry.lookup(id);
    return entry.orElseThrow(
        () -> new MissingFunctionException(CommonLedgerError.FUNCTION_NOT_FOUND));
  }

  public FunctionMachine getInstance(FunctionEntry entry) {
    Class<?> functionClazz = defineClass(entry);
    FunctionMachine function = new FunctionMachine(createInstance(functionClazz));
    function.initialize(this);
    return function;
  }

  public FunctionMachine getInstance(String id) {
    Class<?> functionClazz = defineClass(id);
    FunctionMachine function = new FunctionMachine(createInstance(functionClazz));
    function.initialize(this);
    return function;
  }

  @VisibleForTesting
  Class<?> defineClass(FunctionEntry entry) {
    try {
      return loader.defineClass(entry);
    } catch (Exception e) {
      throw new UnloadableFunctionException(
          CommonLedgerError.LOADING_FUNCTION_FAILED, e, e.getMessage());
    }
  }

  @VisibleForTesting
  Class<?> defineClass(String id) {
    try {
      return loader.defineClass(get(id));
    } catch (MissingFunctionException e) {
      throw e;
    } catch (Exception e) {
      throw new UnloadableFunctionException(
          CommonLedgerError.LOADING_FUNCTION_FAILED, e, e.getMessage());
    }
  }

  private Object createInstance(Class<?> clazz) {
    try {
      return clazz.getConstructor().newInstance();
    } catch (Exception e) {
      throw new UnloadableFunctionException(
          CommonLedgerError.LOADING_FUNCTION_FAILED, e.getMessage());
    }
  }
}
