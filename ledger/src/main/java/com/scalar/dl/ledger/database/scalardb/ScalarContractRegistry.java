package com.scalar.dl.ledger.database.scalardb;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Streams;
import com.google.inject.Inject;
import com.scalar.db.api.Consistency;
import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.PutIfNotExists;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.api.Scanner;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.exception.storage.NoMutationException;
import com.scalar.db.io.IntValue;
import com.scalar.db.io.Key;
import com.scalar.db.io.TextValue;
import com.scalar.dl.ledger.contract.ContractEntry;
import com.scalar.dl.ledger.database.ContractRegistry;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.MissingContractException;
import com.scalar.dl.ledger.exception.UnexpectedValueException;
import com.scalar.dl.ledger.service.StatusCode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
public class ScalarContractRegistry implements ContractRegistry {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ScalarContractRegistry.class.getName());
  static final String CONTRACT_TABLE = "contract";
  static final String CONTRACT_CLASS_TABLE = "contract_class";
  private static final int CONTRACT_CACHE_SIZE = 1048576;
  private static final int CONTRACT_CLASS_CACHE_SIZE = 128;
  private final DistributedStorage storage;
  private final Cache<ContractEntry.Key, Result> contractCache;
  private final Cache<String, Result> contractClassCache;

  @Inject
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public ScalarContractRegistry(DistributedStorage storage) {
    this.storage = storage;
    this.contractCache = CacheBuilder.newBuilder().maximumSize(CONTRACT_CACHE_SIZE).build();
    this.contractClassCache =
        CacheBuilder.newBuilder().maximumSize(CONTRACT_CLASS_CACHE_SIZE).build();
  }

  @Override
  public void bind(ContractEntry entry) {
    if (hasDifferentClassWithSameName(entry)) {
      throw new DatabaseException(
          "the contract binary name has been already registered with a different byte code.",
          StatusCode.CONTRACT_ALREADY_REGISTERED);
    }

    ScalarContractEntry wrapped = new ScalarContractEntry(entry);
    Put putForClass =
        new Put(new Key(wrapped.getBinaryNameValue()))
            .withValue(wrapped.getByteCodeValue())
            .withCondition(new PutIfNotExists())
            .withConsistency(Consistency.LINEARIZABLE)
            .forTable(CONTRACT_CLASS_TABLE);

    Put putForContract =
        new Put(
                new Key(wrapped.getEntityIdValue()),
                new Key(wrapped.getKeyVersionValue(), wrapped.getIdValue()))
            .withValue(wrapped.getBinaryNameValue())
            .withValue(wrapped.getPropertiesValue())
            .withValue(wrapped.getRegisteredAtValue())
            .withValue(wrapped.getSignatureValue())
            .withConsistency(Consistency.SEQUENTIAL)
            .forTable(CONTRACT_TABLE);

    try {
      storage.put(putForClass);
    } catch (NoMutationException e) {
      // it's already registered
    } catch (ExecutionException e) {
      throw new DatabaseException("can't bind the contract", e, StatusCode.DATABASE_ERROR);
    }

    try {
      storage.put(putForContract);
    } catch (ExecutionException e) {
      throw new DatabaseException("can't bind the contract", e, StatusCode.DATABASE_ERROR);
    }
  }

  @Override
  public void unbind(ContractEntry.Key key) {
    throw new UnsupportedOperationException("delete operation is not supported");
  }

  @Override
  public ContractEntry lookup(ContractEntry.Key key) {
    Result resultForContract = contractCache.getIfPresent(key);
    if (resultForContract == null) {
      Get getForContract = prepareGetForContract(key);
      resultForContract = get(getForContract);
      contractCache.put(key, resultForContract);
    }

    String binaryName = getNameFrom(resultForContract);
    Result resultForClass = contractClassCache.getIfPresent(binaryName);
    if (resultForClass == null) {
      Get getForClass = prepareGetForClass(binaryName);
      resultForClass = get(getForClass);
      contractClassCache.put(binaryName, resultForClass);
    }

    return toContractEntry(resultForContract, resultForClass);
  }

  @Override
  public List<ContractEntry> scan(String entityId) {
    return scan(entityId, 0);
  }

  @Override
  public List<ContractEntry> scan(String entityId, int certVersion) {
    Scan scan = new Scan(new Key(ContractEntry.ENTITY_ID, entityId));
    if (certVersion > 0) {
      scan.withStart(new Key(ContractEntry.KEY_VERSION, certVersion))
          .withEnd(new Key(ContractEntry.KEY_VERSION, certVersion));
    }
    scan.withConsistency(Consistency.SEQUENTIAL).forTable(CONTRACT_TABLE);

    Scanner scanner = null;
    try {
      scanner = storage.scan(scan);
      return Streams.stream(scanner)
          .map(r -> toContractEntry(r, get(prepareGetForClass(getNameFrom(r)))))
          .collect(Collectors.toList());
    } catch (ExecutionException e) {
      throw new DatabaseException(
          "can't get the contracts from storage", e, StatusCode.DATABASE_ERROR);
    } finally {
      if (scanner != null) {
        try {
          scanner.close();
        } catch (IOException e) {
          LOGGER.warn("failed to close scanner.", e);
        }
      }
    }
  }

  private Get prepareGetForContract(ContractEntry.Key key) {
    return new Get(
            new Key(ContractEntry.ENTITY_ID, key.getClientIdentityKey().getEntityId()),
            new Key(
                new IntValue(ContractEntry.KEY_VERSION, key.getClientIdentityKey().getKeyVersion()),
                new TextValue(ContractEntry.ID, key.getId())))
        .withConsistency(Consistency.SEQUENTIAL)
        .forTable(CONTRACT_TABLE);
  }

  private Get prepareGetForClass(String name) {
    return new Get(new Key(ContractEntry.BINARY_NAME, name))
        .withConsistency(Consistency.LINEARIZABLE)
        .forTable(CONTRACT_CLASS_TABLE);
  }

  private Result get(Get get) {
    try {
      return storage.get(get).orElseThrow(() -> new MissingContractException("contract not found"));
    } catch (ExecutionException e) {
      throw new DatabaseException(
          "can't get the contract from storage", e, StatusCode.DATABASE_ERROR);
    }
  }

  private boolean hasDifferentClassWithSameName(ContractEntry entry) {
    Get getForClass = prepareGetForClass(entry.getBinaryName());
    try {
      Result resultForClass = get(getForClass);
      byte[] bytesRegistered = getContractBytesFrom(resultForClass);
      if (!Arrays.equals(bytesRegistered, entry.getByteCode())) {
        return true;
      }
    } catch (MissingContractException e) {
      // ignore
    }
    return false;
  }

  private String getIdFrom(Result result) {
    return result.getValue(ContractEntry.ID).get().getAsString().get();
  }

  private String getNameFrom(Result result) {
    return result.getValue(ContractEntry.BINARY_NAME).get().getAsString().get();
  }

  private String getEntityIdFrom(Result result) {
    return result.getValue(ContractEntry.ENTITY_ID).get().getAsString().get();
  }

  private int getCertVersionFrom(Result result) {
    return result.getValue(ContractEntry.KEY_VERSION).get().getAsInt();
  }

  private byte[] getContractBytesFrom(Result result) {
    return result.getValue(ContractEntry.BYTE_CODE).get().getAsBytes().get();
  }

  private String getPropertiesFrom(Result result) {
    if (!result.getValue(ContractEntry.PROPERTIES).isPresent()) {
      return null;
    }
    TextValue value = (TextValue) result.getValue(ContractEntry.PROPERTIES).get();
    if (!value.getAsString().isPresent()) {
      return null;
    }
    return value.getAsString().get();
  }

  private long getRegisteredAtFrom(Result result) {
    return result.getValue(ContractEntry.REGISTERED_AT).get().getAsLong();
  }

  private byte[] getSignatureFrom(Result result) {
    return result.getValue(ContractEntry.SIGNATURE).get().getAsBytes().get();
  }

  private ContractEntry toContractEntry(Result forContract, Result forClass) {
    try {
      return new ContractEntry(
          getIdFrom(forContract),
          getNameFrom(forContract),
          getEntityIdFrom(forContract),
          getCertVersionFrom(forContract),
          getContractBytesFrom(forClass),
          getPropertiesFrom(forContract),
          getRegisteredAtFrom(forContract),
          getSignatureFrom(forContract));
    } catch (Exception e) {
      throw new UnexpectedValueException(e);
    }
  }
}
