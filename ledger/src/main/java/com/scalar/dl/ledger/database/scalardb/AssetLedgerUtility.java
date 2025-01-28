package com.scalar.dl.ledger.database.scalardb;

import static com.scalar.dl.ledger.database.AssetRecord.AGE;
import static com.scalar.dl.ledger.database.AssetRecord.ARGUMENT;
import static com.scalar.dl.ledger.database.AssetRecord.CONTRACT_ID;
import static com.scalar.dl.ledger.database.AssetRecord.HASH;
import static com.scalar.dl.ledger.database.AssetRecord.ID;
import static com.scalar.dl.ledger.database.AssetRecord.NONCE;
import static com.scalar.dl.ledger.database.AssetRecord.PREV_HASH;
import static com.scalar.dl.ledger.database.AssetRecord.SIGNATURE;

import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.io.Key;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetRecord;
import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.exception.UnexpectedValueException;
import com.scalar.dl.ledger.util.Argument;

public class AssetLedgerUtility {

  public static Scan getScanFrom(AssetFilter filter) {
    AssetFilter.AgeOrder ageOrder = filter.getAgeOrder().orElse(AssetFilter.AgeOrder.DESC);
    Scan.Ordering.Order order = Scan.Ordering.Order.DESC;
    if (ageOrder.equals(AssetFilter.AgeOrder.ASC)) {
      order = Scan.Ordering.Order.ASC;
    }

    Scan scan = new Scan(new Key(AssetAttribute.ID, filter.getId()));
    filter
        .getStartAge()
        .ifPresent(v -> scan.withStart(new Key(AssetAttribute.AGE, v), filter.isStartInclusive()));
    filter
        .getEndAge()
        .ifPresent(v -> scan.withEnd(new Key(AssetAttribute.AGE, v), filter.isEndInclusive()));

    scan.withOrdering(new Scan.Ordering(AssetAttribute.AGE, order))
        .withLimit(Math.max(filter.getLimit(), 0));

    return scan;
  }

  public static AssetRecord getAssetRecordFrom(Result result) {
    try {
      AssetRecord.Builder builder = AssetRecord.newBuilder();
      builder.id(result.getValue(ID).get().getAsString().get());
      builder.age(result.getValue(AGE).get().getAsInt());
      String argument = result.getValue(ARGUMENT).get().getAsString().get();
      builder.argument(argument);
      if (result.getValue(NONCE).isPresent()) {
        builder.nonce(result.getValue(NONCE).get().getAsString().get());
      } else {
        builder.nonce(Argument.getNonce(argument));
      }
      builder.contractId(result.getValue(CONTRACT_ID).get().getAsString().get());
      builder.input(result.getValue(AssetRecord.INPUT).get().getAsString().get());
      builder.data(result.getValue(AssetRecord.OUTPUT).get().getAsString().get());
      builder.signature(result.getValue(SIGNATURE).get().getAsBytes().get());
      builder.prevHash(result.getValue(PREV_HASH).get().getAsBytes().orElse(null));
      builder.hash(result.getValue(HASH).get().getAsBytes().get());
      return builder.build();
    } catch (Exception e) {
      throw new UnexpectedValueException(
          CommonError.UNEXPECTED_RECORD_VALUE_OBSERVED, e, e.getMessage());
    }
  }
}
