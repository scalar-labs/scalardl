package com.scalar.dl.ledger.util;

public interface JsonSerDe<T> {

  String serialize(T json);

  T deserialize(String jsonString);
}
