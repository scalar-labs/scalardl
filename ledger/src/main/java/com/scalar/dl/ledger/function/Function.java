package com.scalar.dl.ledger.function;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.scalar.dl.ledger.database.Database;
import com.scalar.dl.ledger.exception.ContractContextException;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.json.JsonObject;

/**
 * An invokable function which users can create their functions based on.
 *
 * @author Hiroyuki Yamada
 * @deprecated This class will be package-private and won't be available to users in release 5.0.0.
 */
@Deprecated
public abstract class Function {
  private FunctionManager manager;
  private JsonObject properties;

  final void initialize(FunctionManager manager) {
    this.manager = checkNotNull(manager);
  }

  final void setProperties(@Nullable JsonObject properties) {
    this.properties = properties;
  }

  @Nullable
  final JsonObject getProperties() {
    return properties;
  }

  /**
   * Invokes the function to {@link Database} with the specified argument and the corresponding root
   * contract argument and properties. An implementation of the {@code Function} should throw {@link
   * ContractContextException} if it faces application-level contextual error (such as lack of
   * balance in payment application).
   *
   * @param database mutable database
   * @param functionArgument json-formatted argument of the function
   * @param contractArgument json-formatted argument of the corresponding root contract
   * @param properties json-formatted pre-registered properties of the corresponding root contract
   */
  public abstract void invoke(
      Database database,
      Optional<JsonObject> functionArgument,
      JsonObject contractArgument,
      Optional<JsonObject> properties);

  @SuppressWarnings("unchecked")
  protected final void invoke(
      String functionId,
      Database database,
      Optional<JsonObject> functionArgument,
      JsonObject contractArgument,
      Optional<JsonObject> contractProperties) {
    checkArgument(manager != null, "please call initialize() before this.");

    DeprecatedFunction function =
        (DeprecatedFunction) manager.getInstance(functionId).getFunctionBase();
    function.invoke(
        database, functionArgument.orElse(null), contractArgument, contractProperties.orElse(null));
  }
}
