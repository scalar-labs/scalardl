package com.scalar.dl.ledger.database.scalardb;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.scalar.db.api.Consistency;
import com.scalar.db.api.Delete;
import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.io.Key;
import com.scalar.dl.ledger.crypto.Cipher;
import com.scalar.dl.ledger.crypto.SecretEntry;
import com.scalar.dl.ledger.database.SecretRegistry;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.MissingSecretException;
import com.scalar.dl.ledger.exception.UnexpectedValueException;
import com.scalar.dl.ledger.service.StatusCode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import javax.annotation.concurrent.Immutable;

@Immutable
public class ScalarSecretRegistry implements SecretRegistry {
  static final String TABLE = "secret";
  private final DistributedStorage storage;
  private final Cipher cipher;

  @Inject
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public ScalarSecretRegistry(DistributedStorage storage, @Named("SecretRegistry") Cipher cipher) {
    this.storage = checkNotNull(storage);
    this.cipher = cipher;
  }

  @Override
  public void bind(SecretEntry entry) {
    byte[] encryptedSecretKey = encrypt(entry.getSecretKey(), entry.getEntityId());
    Put put =
        new Put(
                Key.ofText(SecretEntry.ENTITY_ID, entry.getEntityId()),
                Key.ofInt(SecretEntry.KEY_VERSION, entry.getKeyVersion()))
            .withValue(SecretEntry.SECRET_KEY, encryptedSecretKey)
            .withValue(SecretEntry.REGISTERED_AT, entry.getRegisteredAt())
            .withConsistency(Consistency.SEQUENTIAL)
            .forTable(TABLE);

    try {
      storage.put(put);
    } catch (ExecutionException e) {
      throw new DatabaseException("can't bind the secret key", e, StatusCode.DATABASE_ERROR);
    }
  }

  @Override
  public void unbind(SecretEntry.Key key) {
    Delete delete =
        new Delete(
                Key.ofText(SecretEntry.ENTITY_ID, key.getEntityId()),
                Key.ofInt(SecretEntry.KEY_VERSION, key.getKeyVersion()))
            .withConsistency(Consistency.SEQUENTIAL)
            .forTable(TABLE);

    try {
      storage.delete(delete);
    } catch (ExecutionException e) {
      throw new DatabaseException("can't unbind the secret key", e, StatusCode.DATABASE_ERROR);
    }
  }

  @Override
  public SecretEntry lookup(SecretEntry.Key key) {
    Get get =
        new Get(
                Key.ofText(SecretEntry.ENTITY_ID, key.getEntityId()),
                Key.ofInt(SecretEntry.KEY_VERSION, key.getKeyVersion()))
            .withConsistency(Consistency.SEQUENTIAL)
            .forTable(TABLE);

    Result result;
    try {
      result =
          storage
              .get(get)
              .orElseThrow(() -> new MissingSecretException("the specified secret is not found"));
    } catch (ExecutionException e) {
      throw new DatabaseException(
          "can't get the secret key from storage", e, StatusCode.DATABASE_ERROR);
    }

    return toSecretEntry(result);
  }

  private SecretEntry toSecretEntry(Result result) {
    try {
      String entityId = result.getText(SecretEntry.ENTITY_ID);
      int keyVersion = result.getInt(SecretEntry.KEY_VERSION);
      String decryptedSecretKey = decrypt(result.getBlobAsBytes(SecretEntry.SECRET_KEY), entityId);
      long registeredAt = result.getBigInt(SecretEntry.REGISTERED_AT);
      return new SecretEntry(entityId, keyVersion, decryptedSecretKey, registeredAt);
    } catch (Exception e) {
      throw new UnexpectedValueException(e);
    }
  }

  private byte[] encrypt(String plainText, String nonce) {
    return cipher.encrypt(plainText.getBytes(StandardCharsets.UTF_8), nonce);
  }

  private String decrypt(byte[] encrypted, String nonce) {
    return new String(cipher.decrypt(encrypted, nonce), StandardCharsets.UTF_8);
  }
}
