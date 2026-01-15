package com.scalar.dl.ledger.database;

import java.util.List;
import java.util.Optional;

/**
 * A mutable database abstraction for functions, which can be executed atomically with contracts as
 * requested.
 *
 * @author Hiroyuki Yamada
 */
public interface Database<G, S, P, D, R> {

  Optional<R> get(G get);

  List<R> scan(S scan);

  void put(P put);

  void delete(D delete);
}
