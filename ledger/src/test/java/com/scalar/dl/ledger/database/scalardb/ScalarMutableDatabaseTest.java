package com.scalar.dl.ledger.database.scalardb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

import com.scalar.db.api.Delete;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Scan;
import com.scalar.db.io.Key;
import com.scalar.dl.ledger.exception.InvalidFunctionException;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ScalarMutableDatabaseTest {
  private static final String TABLE = "table";
  private static final Key PARTITION_KEY = Key.ofText("col", "val");

  @Mock private DistributedTransaction transaction;
  private ScalarMutableDatabase database;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    database = new ScalarMutableDatabase(transaction);
  }

  static Stream<String> disallowedNamespaces() {
    return Stream.of(
        // Exact match namespaces
        "system",
        "system_schema",
        "system_auth",
        "system_distributed",
        "system_traces",
        "coordinator",
        // Prefix match: scalar
        "scalar",
        "scalar_my_namespace",
        // Prefix match: auditor
        "auditor",
        "auditor_my_namespace",
        // Case insensitive
        "SYSTEM",
        "Coordinator",
        "SCALAR",
        "Scalar_Namespace",
        "AUDITOR",
        "Auditor_Namespace");
  }

  static Stream<String> allowedNamespaces() {
    return Stream.of("my_namespace", "app_data", "user_table");
  }

  @ParameterizedTest
  @MethodSource("disallowedNamespaces")
  public void get_DisallowedNamespaceGiven_ShouldThrowInvalidFunctionException(String namespace) {
    // Arrange
    Get get =
        Get.newBuilder().namespace(namespace).table(TABLE).partitionKey(PARTITION_KEY).build();

    // Act Assert
    assertThatThrownBy(() -> database.get(get)).isInstanceOf(InvalidFunctionException.class);
  }

  @ParameterizedTest
  @MethodSource("disallowedNamespaces")
  public void scan_DisallowedNamespaceGiven_ShouldThrowInvalidFunctionException(String namespace) {
    // Arrange
    Scan scan =
        Scan.newBuilder().namespace(namespace).table(TABLE).partitionKey(PARTITION_KEY).build();

    // Act Assert
    assertThatThrownBy(() -> database.scan(scan)).isInstanceOf(InvalidFunctionException.class);
  }

  @ParameterizedTest
  @MethodSource("disallowedNamespaces")
  public void put_DisallowedNamespaceGiven_ShouldThrowInvalidFunctionException(String namespace) {
    // Arrange
    Put put =
        Put.newBuilder().namespace(namespace).table(TABLE).partitionKey(PARTITION_KEY).build();

    // Act Assert
    assertThatThrownBy(() -> database.put(put)).isInstanceOf(InvalidFunctionException.class);
  }

  @ParameterizedTest
  @MethodSource("disallowedNamespaces")
  public void delete_DisallowedNamespaceGiven_ShouldThrowInvalidFunctionException(
      String namespace) {
    // Arrange
    Delete delete =
        Delete.newBuilder().namespace(namespace).table(TABLE).partitionKey(PARTITION_KEY).build();

    // Act Assert
    assertThatThrownBy(() -> database.delete(delete)).isInstanceOf(InvalidFunctionException.class);
  }

  @ParameterizedTest
  @MethodSource("allowedNamespaces")
  public void get_AllowedNamespaceGiven_ShouldNotThrow(String namespace) throws Exception {
    // Arrange
    Get get =
        Get.newBuilder().namespace(namespace).table(TABLE).partitionKey(PARTITION_KEY).build();
    when(transaction.get(get)).thenReturn(Optional.empty());

    // Act
    Throwable thrown = catchThrowable(() -> database.get(get));

    // Assert
    assertThat(thrown).isNull();
  }
}
