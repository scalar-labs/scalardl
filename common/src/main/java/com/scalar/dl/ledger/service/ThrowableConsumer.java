package com.scalar.dl.ledger.service;

@FunctionalInterface
public interface ThrowableConsumer<T> {
  void accept(T t) throws Exception;
}
