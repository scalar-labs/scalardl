package com.scalar.dl.ledger.contract;

import com.scalar.dl.ledger.statemachine.Ledger;
import javax.annotation.Nullable;

/**
 * A base contract using the internal String representation as it is for the Ledger data, invoke
 * method arguments, and invoke method return type. You can create your contracts based on it. It is
 * recommended to use {@link JacksonBasedContract} in most cases, but you can use this class to
 * achieve faster and more efficient contract execution by avoiding JSON serialization and
 * deserialization.
 *
 * @author Hiroyuki Yamada
 */
public abstract class StringBasedContract extends ContractBase<String> {

  @Override
  String deserialize(String string) {
    return string;
  }

  @Override
  @Nullable
  final String invokeRoot(Ledger<String> ledger, String argument, String properties) {
    return invoke(ledger, argument, properties);
  }
}
