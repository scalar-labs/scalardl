package com.scalar.dl.ledger.statemachine;

/** An abstraction for the internal representation of an asset. */
public interface InternalAsset {

  String id();

  int age();

  String data();

  String input();

  byte[] signature();

  String contractId();

  String argument();

  byte[] hash();

  byte[] prevHash();
}
