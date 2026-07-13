package com.scalar.dl.ledger.database.scalardb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.api.Delete;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.io.Key;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ScalarMutableDatabaseTest {
  private static final String NAMESPACE = "my_namespace";
  private static final String TABLE = "table";
  private static final Key PARTITION_KEY = Key.ofText("col", "val");

  @Mock private DistributedTransaction transaction;
  private ScalarMutableDatabase database;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    database = new ScalarMutableDatabase(transaction);
  }

  @Test
  public void get_ShouldDelegateToTransaction() throws Exception {
    // Arrange
    Get get =
        Get.newBuilder().namespace(NAMESPACE).table(TABLE).partitionKey(PARTITION_KEY).build();
    Result result = mock(Result.class);
    when(transaction.get(get)).thenReturn(Optional.of(result));

    // Act
    Optional<Result> actual = database.get(get);

    // Assert
    assertThat(actual).hasValue(result);
    verify(transaction).get(get);
  }

  @Test
  public void scan_ShouldDelegateToTransaction() throws Exception {
    // Arrange
    Scan scan =
        Scan.newBuilder().namespace(NAMESPACE).table(TABLE).partitionKey(PARTITION_KEY).build();
    Result result = mock(Result.class);
    when(transaction.scan(scan)).thenReturn(Collections.singletonList(result));

    // Act
    List<Result> actual = database.scan(scan);

    // Assert
    assertThat(actual).containsExactly(result);
    verify(transaction).scan(scan);
  }

  @Test
  public void put_ShouldDelegateToTransactionWithImplicitPreReadEnabled() throws Exception {
    // Arrange
    Put put =
        Put.newBuilder().namespace(NAMESPACE).table(TABLE).partitionKey(PARTITION_KEY).build();

    // Act
    database.put(put);

    // Assert
    ArgumentCaptor<Put> captor = ArgumentCaptor.forClass(Put.class);
    verify(transaction).put(captor.capture());
    assertThat(captor.getValue().isImplicitPreReadEnabled()).isTrue();
  }

  @Test
  public void delete_ShouldDelegateToTransaction() throws Exception {
    // Arrange
    Delete delete =
        Delete.newBuilder().namespace(NAMESPACE).table(TABLE).partitionKey(PARTITION_KEY).build();

    // Act
    database.delete(delete);

    // Assert
    verify(transaction).delete(delete);
  }
}
