package com.scalar.dl.ledger.database.scalardb;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import com.scalar.db.api.ConditionBuilder;
import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.DistributedStorageAdmin;
import com.scalar.db.api.DistributedTransactionAdmin;
import com.scalar.db.api.Put;
import com.scalar.db.api.Scan;
import com.scalar.db.api.Scan.Ordering;
import com.scalar.db.api.ScanBuilder.BuildableScanWithPartitionKey;
import com.scalar.db.api.Scanner;
import com.scalar.db.api.TableMetadata;
import com.scalar.db.common.CoreError;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.exception.storage.NoMutationException;
import com.scalar.db.io.DataType;
import com.scalar.db.io.Key;
import com.scalar.dl.ledger.config.ServerConfig;
import com.scalar.dl.ledger.database.NamespaceRegistry;
import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.exception.DatabaseException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
public abstract class AbstractScalarNamespaceRegistry implements NamespaceRegistry {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(AbstractScalarNamespaceRegistry.class.getName());
  private static final int MAX_ATTEMPTS = 5;
  private static final long SLEEP_BASE_MILLIS = 100L;
  @VisibleForTesting static final int DEFAULT_PARTITION_ID = 0;
  @VisibleForTesting static final String NAMESPACE_NAME_SEPARATOR = "_";
  @VisibleForTesting static final String NAMESPACE_TABLE_NAME = "namespace";
  @VisibleForTesting static final String COLUMN_PARTITION_ID = "partition_id";
  @VisibleForTesting static final String COLUMN_NAME = "name";

  @VisibleForTesting
  static final TableMetadata NAMESPACE_TABLE_METADATA =
      TableMetadata.newBuilder()
          .addColumn(COLUMN_PARTITION_ID, DataType.INT)
          .addColumn(COLUMN_NAME, DataType.TEXT)
          .addPartitionKey(COLUMN_PARTITION_ID)
          .addClusteringKey(COLUMN_NAME)
          .build();

  private final ServerConfig config;
  private final DistributedStorage storage;
  private final DistributedStorageAdmin storageAdmin;
  @Nullable private final DistributedTransactionAdmin transactionAdmin;
  private final ScalarNamespaceResolver namespaceResolver;
  private final Set<TableMetadataProvider> tableMetadataProviders;
  private final ImmutableMap<String, TableMetadata> storageTables;
  private final ImmutableMap<String, TableMetadata> transactionTables;
  private final Retry retry;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public AbstractScalarNamespaceRegistry(
      ServerConfig config,
      DistributedStorage storage,
      DistributedStorageAdmin storageAdmin,
      DistributedTransactionAdmin transactionAdmin,
      ScalarNamespaceResolver namespaceResolver,
      Set<TableMetadataProvider> tableMetadataProviders) {
    this.config = checkNotNull(config);
    this.storage = checkNotNull(storage);
    this.storageAdmin = checkNotNull(storageAdmin);
    this.transactionAdmin = transactionAdmin;
    this.namespaceResolver = checkNotNull(namespaceResolver);
    this.tableMetadataProviders = checkNotNull(tableMetadataProviders);
    this.storageTables = collectStorageTables();
    this.transactionTables = collectTransactionTables();
    this.retry =
        Retry.of(
            "retry",
            RetryConfig.custom()
                .maxAttempts(MAX_ATTEMPTS)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(SLEEP_BASE_MILLIS, 2.0))
                .retryOnException(
                    e -> {
                      LOGGER.warn("Namespace creation failed");
                      return e instanceof IllegalArgumentException
                          && e.getMessage() != null
                          && (e.getMessage().startsWith(CoreError.NAMESPACE_NOT_FOUND.buildCode())
                              || e.getMessage().startsWith(CoreError.TABLE_NOT_FOUND.buildCode()));
                    })
                .build());
  }

  public AbstractScalarNamespaceRegistry(
      ServerConfig config,
      DistributedStorage storage,
      DistributedStorageAdmin storageAdmin,
      ScalarNamespaceResolver namespaceResolver,
      Set<TableMetadataProvider> tableMetadataProviders) {
    this(config, storage, storageAdmin, null, namespaceResolver, tableMetadataProviders);
  }

  @Override
  public void create(String namespace) {
    Retry.decorateRunnable(
            retry,
            () -> {
              createNamespaceManagementTable();
              createNamespace(namespace);
              addNamespaceEntry(namespace);
            })
        .run();
  }

  private void createNamespaceManagementTable() {
    try {
      storageAdmin.createTable(
          config.getNamespace(), NAMESPACE_TABLE_NAME, NAMESPACE_TABLE_METADATA, true);
    } catch (ExecutionException e) {
      throw new DatabaseException(CommonError.CREATING_NAMESPACE_TABLE_FAILED, e, e.getMessage());
    }
  }

  private void createNamespace(String namespace) {
    try {
      String fullNamespaceName = namespaceResolver.resolve(namespace);
      storageAdmin.createNamespace(fullNamespaceName, true);
      createStorageTables(fullNamespaceName);
      createTransactionTables(fullNamespaceName);
    } catch (ExecutionException e) {
      throw new DatabaseException(CommonError.CREATING_NAMESPACE_FAILED, e, e.getMessage());
    }
  }

  private void createStorageTables(String fullNamespaceName) throws ExecutionException {
    for (Map.Entry<String, TableMetadata> entry : storageTables.entrySet()) {
      String tableName = entry.getKey();
      TableMetadata tableMetadata = entry.getValue();
      storageAdmin.createTable(fullNamespaceName, tableName, tableMetadata, true);
    }
  }

  private void createTransactionTables(String fullNamespaceName) throws ExecutionException {
    if (transactionAdmin == null || transactionTables.isEmpty()) {
      return;
    }

    for (Map.Entry<String, TableMetadata> entry : transactionTables.entrySet()) {
      String tableName = entry.getKey();
      TableMetadata tableMetadata = entry.getValue();
      transactionAdmin.createTable(fullNamespaceName, tableName, tableMetadata, true);
    }
  }

  private void addNamespaceEntry(String namespace) {
    Put put =
        Put.newBuilder()
            .namespace(config.getNamespace())
            .table(NAMESPACE_TABLE_NAME)
            .partitionKey(Key.ofInt(COLUMN_PARTITION_ID, DEFAULT_PARTITION_ID))
            .clusteringKey(Key.ofText(COLUMN_NAME, namespace))
            .condition(ConditionBuilder.putIfNotExists())
            .build();
    try {
      storage.put(put);
    } catch (NoMutationException e) {
      throw new DatabaseException(CommonError.NAMESPACE_ALREADY_EXISTS);
    } catch (ExecutionException e) {
      throw new DatabaseException(CommonError.CREATING_NAMESPACE_FAILED, e, e.getMessage());
    }
  }

  private ImmutableMap<String, TableMetadata> collectStorageTables() {
    ImmutableMap.Builder<String, TableMetadata> builder = ImmutableMap.builder();
    for (TableMetadataProvider provider : tableMetadataProviders) {
      builder.putAll(provider.getStorageTables());
    }
    return builder.build();
  }

  private ImmutableMap<String, TableMetadata> collectTransactionTables() {
    ImmutableMap.Builder<String, TableMetadata> builder = ImmutableMap.builder();
    for (TableMetadataProvider provider : tableMetadataProviders) {
      builder.putAll(provider.getTransactionTables());
    }
    return builder.build();
  }

  @Override
  public List<String> scan(@Nonnull String pattern) {
    BuildableScanWithPartitionKey scanBuilder =
        Scan.newBuilder()
            .namespace(config.getNamespace())
            .table(NAMESPACE_TABLE_NAME)
            .partitionKey(Key.ofInt(COLUMN_PARTITION_ID, DEFAULT_PARTITION_ID))
            .ordering(Ordering.asc(COLUMN_NAME));

    if (!pattern.isEmpty()) {
      scanBuilder.where(ConditionBuilder.column(COLUMN_NAME).isLikeText("%" + pattern + "%"));
    }

    return scan(scanBuilder.build());
  }

  private List<String> scan(Scan scan) {
    Scanner scanner = null;
    try {
      if (!storageAdmin.tableExists(config.getNamespace(), NAMESPACE_TABLE_NAME)) {
        // Before creating a namespace, the namespace management table may not exist (e.g., when
        // using old ScalarDL versions).
        return Collections.emptyList();
      } else {
        scanner = storage.scan(scan);
        return Streams.stream(scanner)
            .map(result -> result.getText(COLUMN_NAME))
            .collect(Collectors.toList());
      }
    } catch (ExecutionException e) {
      throw new DatabaseException(CommonError.SCANNING_NAMESPACES_FAILED, e, e.getMessage());
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
}
