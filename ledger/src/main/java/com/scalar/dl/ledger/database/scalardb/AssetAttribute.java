package com.scalar.dl.ledger.database.scalardb;

import com.scalar.db.io.BlobValue;
import com.scalar.db.io.IntValue;
import com.scalar.db.io.TextValue;
import javax.annotation.concurrent.Immutable;

@Immutable
public class AssetAttribute {
  public static final String ID = "id";
  public static final String AGE = "age";
  public static final String INPUT = "input";
  public static final String OUTPUT = "output";
  public static final String CONTRACT_ID = "contract_id";
  public static final String ARGUMENT = "argument";
  public static final String SIGNATURE = "signature";
  public static final String HASH = "hash";
  public static final String PREV_HASH = "prev_hash";

  public static TextValue toIdValue(String assetId) {
    return new TextValue(AssetAttribute.ID, assetId);
  }

  public static IntValue toAgeValue(int age) {
    return new IntValue(AssetAttribute.AGE, age);
  }

  public static TextValue toInputValue(String input) {
    return new TextValue(AssetAttribute.INPUT, input);
  }

  public static TextValue toOutputValue(String output) {
    return new TextValue(AssetAttribute.OUTPUT, output);
  }

  public static BlobValue toSignatureValue(byte[] signature) {
    return new BlobValue(AssetAttribute.SIGNATURE, signature);
  }

  public static TextValue toContactIdValue(String contactRef) {
    return new TextValue(AssetAttribute.CONTRACT_ID, contactRef);
  }

  public static TextValue toArgumentValue(String argument) {
    return new TextValue(AssetAttribute.ARGUMENT, argument);
  }

  public static BlobValue toHashValue(byte[] hash) {
    return new BlobValue(AssetAttribute.HASH, hash);
  }

  public static BlobValue toPrevHashValue(byte[] prevHash) {
    return new BlobValue(AssetAttribute.PREV_HASH, prevHash);
  }
}
