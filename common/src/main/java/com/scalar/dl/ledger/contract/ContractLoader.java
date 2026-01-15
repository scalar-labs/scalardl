package com.scalar.dl.ledger.contract;

import com.google.inject.Inject;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.concurrent.Immutable;

/**
 * A class loader for a Contract from its bytecode.
 *
 * <p>The {@code defineClass} method in this class creates an instance of the {@link Contract} or
 * the {@link ContractBase} class from its corresponding bytecode. It only grants the given
 * permissions with {@code ProtectionDomain}.
 */
@Immutable
public class ContractLoader extends ClassLoader {
  private final Map<String, Class<?>> loadedMap;
  private final ProtectionDomain protectionDomain;

  @Inject
  public ContractLoader(ProtectionDomain protectionDomain) {
    this.loadedMap = new ConcurrentHashMap<>();
    this.protectionDomain = protectionDomain;
  }

  /**
   * Turns the bytecode of a registered contract into an instance of class {@code Class}.
   *
   * @param entry the entry of a {@code Contract} or a {@code ContractBase} that will be loaded
   * @return a class object of {@code Contract} or {@code ContractBase}
   */
  public Class<?> defineClass(ContractEntry entry) {
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
