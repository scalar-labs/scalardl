package com.scalar.dl.ledger.service;

@FunctionalInterface
public interface ThrowableFunction<T, R> {
  R apply(T t) throws Exception;
}
