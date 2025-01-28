package com.scalar.dl.ledger.validation;

import com.scalar.dl.ledger.statemachine.DeserializationType;

public interface LedgerTracerManager {

  LedgerTracerBase<?> start(DeserializationType type);
}
