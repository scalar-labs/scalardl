package com.scalar.dl.ledger.database.scalardb;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import com.scalar.db.api.Consistency;
import com.scalar.db.api.Delete;
import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.io.Key;
import com.scalar.dl.ledger.crypto.CertificateEntry;
import com.scalar.dl.ledger.database.CertificateRegistry;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.MissingCertificateException;
import com.scalar.dl.ledger.exception.UnexpectedValueException;
import com.scalar.dl.ledger.service.StatusCode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.annotation.concurrent.Immutable;

@Immutable
public class ScalarCertificateRegistry implements CertificateRegistry {
  static final String TABLE = "certificate";
  private final DistributedStorage storage;

  @Inject
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public ScalarCertificateRegistry(DistributedStorage storage) {
    this.storage = checkNotNull(storage);
  }

  @Override
  public void bind(CertificateEntry entry) {
    Put put =
        new Put(
                new Key(CertificateEntry.ENTITY_ID, entry.getEntityId()),
                new Key(CertificateEntry.VERSION, entry.getVersion()))
            .withValue(CertificateEntry.PEM, entry.getPem())
            .withValue(CertificateEntry.REGISTERED_AT, entry.getRegisteredAt())
            .withConsistency(Consistency.SEQUENTIAL)
            .forTable(TABLE);

    try {
      storage.put(put);
    } catch (ExecutionException e) {
      throw new DatabaseException("can't bind the public key", e, StatusCode.DATABASE_ERROR);
    }
  }

  @Override
  public void unbind(CertificateEntry.Key key) {
    Delete delete =
        new Delete(
                new Key(CertificateEntry.ENTITY_ID, key.getEntityId()),
                new Key(CertificateEntry.VERSION, key.getKeyVersion()))
            .withConsistency(Consistency.SEQUENTIAL)
            .forTable(TABLE);

    try {
      storage.delete(delete);
    } catch (ExecutionException e) {
      throw new DatabaseException("can't unbind the public key", e, StatusCode.DATABASE_ERROR);
    }
  }

  @Override
  public CertificateEntry lookup(CertificateEntry.Key key) {
    Get get =
        new Get(
                new Key(CertificateEntry.ENTITY_ID, key.getEntityId()),
                new Key(CertificateEntry.VERSION, key.getKeyVersion()))
            .withConsistency(Consistency.SEQUENTIAL)
            .forTable(TABLE);

    Result result;
    try {
      result =
          storage
              .get(get)
              .orElseThrow(
                  () -> new MissingCertificateException("the specified public key not found"));
    } catch (ExecutionException e) {
      throw new DatabaseException(
          "can't get the public key from storage", e, StatusCode.DATABASE_ERROR);
    }

    return toCertEntry(result);
  }

  private String getIdFrom(Result result) {
    return result.getValue(CertificateEntry.ENTITY_ID).get().getAsString().get();
  }

  private int getVersionFrom(Result result) {
    return result.getValue(CertificateEntry.VERSION).get().getAsInt();
  }

  private String getPemFrom(Result result) {
    return result.getValue(CertificateEntry.PEM).get().getAsString().get();
  }

  private long getRegisteredAtFrom(Result result) {
    return result.getValue(CertificateEntry.REGISTERED_AT).get().getAsLong();
  }

  private CertificateEntry toCertEntry(Result result) {
    try {
      return new CertificateEntry(
          getIdFrom(result),
          getVersionFrom(result),
          getPemFrom(result),
          getRegisteredAtFrom(result));
    } catch (Exception e) {
      throw new UnexpectedValueException(e);
    }
  }
}
