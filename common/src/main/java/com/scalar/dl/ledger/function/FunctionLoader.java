package com.scalar.dl.ledger.function;

import com.google.inject.Inject;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.concurrent.Immutable;

/**
 * A class loader for a Functioin from its bytecode.
 *
 * <p>The {@code defineClass} method in this class creates an instance of the {@link Function} or
 * the {@link FunctionBase} class from its corresponding bytecode. It only grants the given
 * permissions with {@code ProtectionDomain}.
 */
@Immutable
public class FunctionLoader extends ClassLoader {
  private final Map<String, Class<?>> loadedMap;
  private final ProtectionDomain protectionDomain;

  @Inject
  public FunctionLoader(ProtectionDomain protectionDomain) {
    this.loadedMap = new ConcurrentHashMap<>();
    this.protectionDomain = protectionDomain;
  }

  /**
   * Turns the bytecode of a registered transaction into a {@code Function} instance.
   *
   * @param entry the entry of a {@code Function} that will be loaded
   * @return a {@code Function}
   */
  public Class<?> defineClass(FunctionEntry entry) {
    return loadedMap.computeIfAbsent(
        entry.getBinaryName(),
        k ->
            defineClass(
                entry.getBinaryName(),
                entry.getByteCode(),
                0,
                entry.getByteCode().length,
                protectionDomain));
  }
}
