package com.scalar.dl.genericcontracts.object;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.scalar.db.api.Delete;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.io.Key;
import com.scalar.dl.ledger.database.Database;
import com.scalar.dl.ledger.exception.ContractContextException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PutToMutableDatabaseTest {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String SOME_NAMESPACE = "ns";
  private static final String SOME_TABLE = "test_table";
  private static final String SOME_COLUMN_NAME = "col";
  private static final String SOME_TEXT_COLUMN1_NAME = "object_id";
  private static final String SOME_TEXT_COLUMN1_VALUE = "foo";
  private static final String SOME_TEXT_COLUMN2_NAME = "version";
  private static final String SOME_TEXT_COLUMN2_VALUE = "xyz";
  private static final String SOME_INT_COLUMN_NAME = "status";
  private static final int SOME_INT_COLUMN_VALUE = 3;
  private static final String SOME_BIGINT_COLUMN_NAME = "timestamp";
  private static final long SOME_BIGINT_COLUMN_VALUE = 1234567890123L;
  private static final String SOME_BOOLEAN_COLUMN_NAME = "boolean_column";
  private static final boolean SOME_BOOLEAN_COLUMN_VALUE = true;
  private static final String SOME_FLOAT_COLUMN_NAME = "float_column";
  private static final float SOME_FLOAT_COLUMN_VALUE = 1.23f;
  private static final String SOME_DOUBLE_COLUMN_NAME = "double_column";
  private static final double SOME_DOUBLE_COLUMN_VALUE = 1.23;
  private static final String SOME_BLOB_COLUMN_NAME = "blob_column";
  private static final byte[] SOME_BLOB_COLUMN_VALUE = {1, 2, 3, 4, 5};
  private static final String SOME_BLOB_COLUMN_TEXT = "AQIDBAU="; // Base64 of {1, 2, 3, 4, 5}
  private static final JsonNode SOME_TEXT_COLUMN1 =
      mapper
          .createObjectNode()
          .put(Constants.COLUMN_NAME, SOME_TEXT_COLUMN1_NAME)
          .put(Constants.VALUE, SOME_TEXT_COLUMN1_VALUE)
          .put(Constants.DATA_TYPE, "TEXT");
  private static final JsonNode SOME_TEXT_COLUMN2 =
      mapper
          .createObjectNode()
          .put(Constants.COLUMN_NAME, SOME_TEXT_COLUMN2_NAME)
          .put(Constants.VALUE, SOME_TEXT_COLUMN2_VALUE)
          .put(Constants.DATA_TYPE, "TEXT");
  private static final JsonNode SOME_INT_COLUMN =
      mapper
          .createObjectNode()
          .put(Constants.COLUMN_NAME, SOME_INT_COLUMN_NAME)
          .put(Constants.VALUE, SOME_INT_COLUMN_VALUE)
          .put(Constants.DATA_TYPE, "INT");
  private static final JsonNode SOME_BIGINT_COLUMN =
      mapper
          .createObjectNode()
          .put(Constants.COLUMN_NAME, SOME_BIGINT_COLUMN_NAME)
          .put(Constants.VALUE, SOME_BIGINT_COLUMN_VALUE)
          .put(Constants.DATA_TYPE, "BIGINT");
  private static final JsonNode SOME_BOOLEAN_COLUMN =
      mapper
          .createObjectNode()
          .put(Constants.COLUMN_NAME, SOME_BOOLEAN_COLUMN_NAME)
          .put(Constants.VALUE, SOME_BOOLEAN_COLUMN_VALUE)
          .put(Constants.DATA_TYPE, "BOOLEAN");
  private static final JsonNode SOME_FLOAT_COLUMN =
      mapper
          .createObjectNode()
          .put(Constants.COLUMN_NAME, SOME_FLOAT_COLUMN_NAME)
          .put(
              Constants.VALUE,
              Double.valueOf(
                  SOME_FLOAT_COLUMN_VALUE)) // float is always converted to double in function args
          .put(Constants.DATA_TYPE, "FLOAT");
  private static final JsonNode SOME_DOUBLE_COLUMN =
      mapper
          .createObjectNode()
          .put(Constants.COLUMN_NAME, SOME_DOUBLE_COLUMN_NAME)
          .put(Constants.VALUE, SOME_DOUBLE_COLUMN_VALUE)
          .put(Constants.DATA_TYPE, "DOUBLE");
  private static final JsonNode SOME_BLOB_COLUMN =
      mapper
          .createObjectNode()
          .put(Constants.COLUMN_NAME, SOME_BLOB_COLUMN_NAME)
          .put(Constants.VALUE, SOME_BLOB_COLUMN_TEXT)
          .put(Constants.DATA_TYPE, "BLOB");
  private static final JsonNode SOME_NULL_COLUMN =
      mapper
          .createObjectNode()
          .put(Constants.COLUMN_NAME, SOME_COLUMN_NAME)
          .put(Constants.VALUE, (byte[]) null)
          .put(Constants.DATA_TYPE, "BLOB");
  private static final JsonNode SOME_COLUMN_WITHOUT_VALUE =
      mapper.createObjectNode().put(Constants.COLUMN_NAME, SOME_COLUMN_NAME);
  private static final JsonNode SOME_COLUMN_WITH_INVALID_TYPE =
      mapper
          .createObjectNode()
          .put(Constants.COLUMN_NAME, SOME_FLOAT_COLUMN_NAME)
          .put(Constants.VALUE, SOME_FLOAT_COLUMN_VALUE)
          .put(Constants.DATA_TYPE, "foo");

  private final PutToMutableDatabase putToMutableDatabase = new PutToMutableDatabase();

  @Mock private Database<Get, Scan, Put, Delete, Result> database;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this).close();
  }

  @Test
  public void invoke_RequiredArgumentsGiven_ShouldPutRecord() {
    // Arrange
    ArrayNode partitionKey = mapper.createArrayNode().add(SOME_TEXT_COLUMN1);
    ObjectNode arguments = mapper.createObjectNode();
    arguments.put(Constants.NAMESPACE, SOME_NAMESPACE);
    arguments.put(Constants.TABLE, SOME_TABLE);
    arguments.set(Constants.PARTITION_KEY, partitionKey);
    Put expected =
        Put.newBuilder()
            .namespace(SOME_NAMESPACE)
            .table(SOME_TABLE)
            .partitionKey(Key.ofText(SOME_TEXT_COLUMN1_NAME, SOME_TEXT_COLUMN1_VALUE))
            .build();

    // Act
    JsonNode actual = putToMutableDatabase.invoke(database, arguments, null, null);

    // Assert
    assertThat(actual).isNull();
    verify(database).put(expected);
  }

  @Test
  public void invoke_AllArgumentsGiven_ShouldPutRecord() {
    // Arrange
    ArrayNode partitionKey = mapper.createArrayNode().add(SOME_TEXT_COLUMN1);
    ArrayNode clusteringKey = mapper.createArrayNode().add(SOME_TEXT_COLUMN2);
    ArrayNode columns =
        mapper
            .createArrayNode()
            .add(SOME_BOOLEAN_COLUMN)
            .add(SOME_INT_COLUMN)
            .add(SOME_BIGINT_COLUMN)
            .add(SOME_FLOAT_COLUMN)
            .add(SOME_DOUBLE_COLUMN)
            .add(SOME_BLOB_COLUMN);
    ObjectNode arguments = mapper.createObjectNode();
    arguments.put(Constants.NAMESPACE, SOME_NAMESPACE);
    arguments.put(Constants.TABLE, SOME_TABLE);
    arguments.set(Constants.PARTITION_KEY, partitionKey);
    arguments.set(Constants.CLUSTERING_KEY, clusteringKey);
    arguments.set(Constants.COLUMNS, columns);
    Put expected =
        Put.newBuilder()
            .namespace(SOME_NAMESPACE)
            .table(SOME_TABLE)
            .partitionKey(Key.ofText(SOME_TEXT_COLUMN1_NAME, SOME_TEXT_COLUMN1_VALUE))
            .clusteringKey(Key.ofText(SOME_TEXT_COLUMN2_NAME, SOME_TEXT_COLUMN2_VALUE))
            .booleanValue(SOME_BOOLEAN_COLUMN_NAME, SOME_BOOLEAN_COLUMN_VALUE)
            .intValue(SOME_INT_COLUMN_NAME, SOME_INT_COLUMN_VALUE)
            .bigIntValue(SOME_BIGINT_COLUMN_NAME, SOME_BIGINT_COLUMN_VALUE)
            .floatValue(SOME_FLOAT_COLUMN_NAME, SOME_FLOAT_COLUMN_VALUE)
            .doubleValue(SOME_DOUBLE_COLUMN_NAME, SOME_DOUBLE_COLUMN_VALUE)
            .blobValue(SOME_BLOB_COLUMN_NAME, SOME_BLOB_COLUMN_VALUE)
            .build();

    // Act
    JsonNode actual = putToMutableDatabase.invoke(database, arguments, null, null);

    // Assert
    assertThat(actual).isNull();
    verify(database).put(expected);
  }

  @Test
  public void invoke_MultiplePartitionKeyColumnsGiven_ShouldPutRecord() {
    // Arrange
    ArrayNode partitionKey = mapper.createArrayNode().add(SOME_TEXT_COLUMN1).add(SOME_TEXT_COLUMN2);
    ObjectNode arguments = mapper.createObjectNode();
    arguments.put(Constants.NAMESPACE, SOME_NAMESPACE);
    arguments.put(Constants.TABLE, SOME_TABLE);
    arguments.set(Constants.PARTITION_KEY, partitionKey);
    Put expected =
        Put.newBuilder()
            .namespace(SOME_NAMESPACE)
            .table(SOME_TABLE)
            .partitionKey(
                Key.of(
                    SOME_TEXT_COLUMN1_NAME, SOME_TEXT_COLUMN1_VALUE,
                    SOME_TEXT_COLUMN2_NAME, SOME_TEXT_COLUMN2_VALUE))
            .build();

    // Act
    JsonNode actual = putToMutableDatabase.invoke(database, arguments, null, null);

    // Assert
    assertThat(actual).isNull();
    verify(database).put(expected);
  }

  @Test
  public void invoke_MultipleClusteringKeyColumnsGiven_ShouldPutRecord() {
    // Arrange
    ArrayNode partitionKey = mapper.createArrayNode().add(SOME_TEXT_COLUMN1);
    ArrayNode clusteringKey = mapper.createArrayNode().add(SOME_TEXT_COLUMN2).add(SOME_INT_COLUMN);
    ObjectNode arguments = mapper.createObjectNode();
    arguments.put(Constants.NAMESPACE, SOME_NAMESPACE);
    arguments.put(Constants.TABLE, SOME_TABLE);
    arguments.set(Constants.PARTITION_KEY, partitionKey);
    arguments.set(Constants.CLUSTERING_KEY, clusteringKey);
    Put expected =
        Put.newBuilder()
            .namespace(SOME_NAMESPACE)
            .table(SOME_TABLE)
            .partitionKey(Key.ofText(SOME_TEXT_COLUMN1_NAME, SOME_TEXT_COLUMN1_VALUE))
            .clusteringKey(
                Key.of(
                    SOME_TEXT_COLUMN2_NAME,
                    SOME_TEXT_COLUMN2_VALUE,
                    SOME_INT_COLUMN_NAME,
                    SOME_INT_COLUMN_VALUE))
            .build();

    // Act
    JsonNode actual = putToMutableDatabase.invoke(database, arguments, null, null);

    // Assert
    assertThat(actual).isNull();
    verify(database).put(expected);
  }

  @Test
  public void invoke_EmptyClusteringKeyAndColumnsGiven_ShouldPutRecord() {
    // Arrange
    ArrayNode partitionKey = mapper.createArrayNode().add(SOME_TEXT_COLUMN1);
    ArrayNode clusteringKey = mapper.createArrayNode();
    ArrayNode columns = mapper.createArrayNode();
    ObjectNode arguments = mapper.createObjectNode();
    arguments.put(Constants.NAMESPACE, SOME_NAMESPACE);
    arguments.put(Constants.TABLE, SOME_TABLE);
    arguments.set(Constants.PARTITION_KEY, partitionKey);
    arguments.set(Constants.CLUSTERING_KEY, clusteringKey);
    arguments.set(Constants.COLUMNS, columns);
    Put expected =
        Put.newBuilder()
            .namespace(SOME_NAMESPACE)
            .table(SOME_TABLE)
            .partitionKey(Key.ofText(SOME_TEXT_COLUMN1_NAME, SOME_TEXT_COLUMN1_VALUE))
            .build();

    // Act
    JsonNode actual = putToMutableDatabase.invoke(database, arguments, null, null);

    // Assert
    assertThat(actual).isNull();
    verify(database).put(expected);
  }

  @Test
  public void invoke_NullValueColumnGiven_ShouldPutRecord() {
    // Arrange
    ArrayNode partitionKey = mapper.createArrayNode().add(SOME_TEXT_COLUMN1);
    ObjectNode arguments = mapper.createObjectNode();
    ArrayNode columns = mapper.createArrayNode().add(SOME_NULL_COLUMN);
    arguments.put(Constants.NAMESPACE, SOME_NAMESPACE);
    arguments.put(Constants.TABLE, SOME_TABLE);
    arguments.set(Constants.PARTITION_KEY, partitionKey);
    arguments.set(Constants.COLUMNS, columns);
    Put expected =
        Put.newBuilder()
            .namespace(SOME_NAMESPACE)
            .table(SOME_TABLE)
            .partitionKey(Key.ofText(SOME_TEXT_COLUMN1_NAME, SOME_TEXT_COLUMN1_VALUE))
            .blobValue(SOME_COLUMN_NAME, (byte[]) null)
            .build();

    // Act
    JsonNode actual = putToMutableDatabase.invoke(database, arguments, null, null);

    // Assert
    assertThat(actual).isNull();
    verify(database).put(expected);
  }

  @Test
  public void invoke_BigIntColumnWithIntRangeValueGiven_ShouldPutRecord() {
    // Arrange
    ArrayNode partitionKey = mapper.createArrayNode().add(SOME_TEXT_COLUMN1);
    JsonNode column =
        mapper
            .createObjectNode()
            .put(Constants.COLUMN_NAME, SOME_BIGINT_COLUMN_NAME)
            .put(Constants.VALUE, 1)
            .put(Constants.DATA_TYPE, "BIGINT");
    ArrayNode columns = mapper.createArrayNode().add(column);
    ObjectNode arguments = mapper.createObjectNode();
    arguments.put(Constants.NAMESPACE, SOME_NAMESPACE);
    arguments.put(Constants.TABLE, SOME_TABLE);
    arguments.set(Constants.PARTITION_KEY, partitionKey);
    arguments.set(Constants.COLUMNS, columns);
    Put expected =
        Put.newBuilder()
            .namespace(SOME_NAMESPACE)
            .table(SOME_TABLE)
            .partitionKey(Key.ofText(SOME_TEXT_COLUMN1_NAME, SOME_TEXT_COLUMN1_VALUE))
            .bigIntValue(SOME_BIGINT_COLUMN_NAME, 1)
            .build();

    // Act
    JsonNode actual = putToMutableDatabase.invoke(database, arguments, null, null);

    // Assert
    assertThat(actual).isNull();
    verify(database).put(expected);
  }

  @Test
  public void invoke_NoNamespaceGiven_ShouldThrowContractContextException() {
    // Arrange
    ArrayNode partitionKey = mapper.createArrayNode().add(SOME_TEXT_COLUMN1);
    ArrayNode columns = mapper.createArrayNode().add(SOME_INT_COLUMN_VALUE);
    ObjectNode arguments = mapper.createObjectNode();
    arguments.put(Constants.TABLE, SOME_TABLE);
    arguments.set(Constants.PARTITION_KEY, partitionKey);
    arguments.set(Constants.COLUMNS, columns);

    // Act Assert
    assertThatThrownBy(() -> putToMutableDatabase.invoke(database, arguments, null, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT);
    verify(database, never()).put(any());
  }

  @Test
  public void invoke_NoPartitionKeyGiven_ShouldThrowContractContextException() {
    // Arrange
    ArrayNode columns = mapper.createArrayNode().add(SOME_INT_COLUMN_VALUE);
    ObjectNode arguments = mapper.createObjectNode();
    arguments.put(Constants.NAMESPACE, SOME_NAMESPACE);
    arguments.put(Constants.TABLE, SOME_TABLE);
    arguments.set(Constants.COLUMNS, columns);

    // Act Assert
    assertThatThrownBy(() -> putToMutableDatabase.invoke(database, arguments, null, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT);
    verify(database, never()).put(any());
  }

  @Test
  public void invoke_InvalidPartitionKeyGiven_ShouldThrowContractContextException() {
    // Arrange
    ObjectNode arguments = mapper.createObjectNode();
    arguments.put(Constants.NAMESPACE, SOME_NAMESPACE);
    arguments.put(Constants.TABLE, SOME_TABLE);
    arguments.put(Constants.PARTITION_KEY, "invalid");

    // Act Assert
    assertThatThrownBy(() -> putToMutableDatabase.invoke(database, arguments, null, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT);
    verify(database, never()).put(any());
  }

  @Test
  public void invoke_InvalidClusteringKeyGiven_ShouldThrowContractContextException() {
    // Arrange
    ArrayNode partitionKey = mapper.createArrayNode().add(SOME_TEXT_COLUMN1);
    ObjectNode arguments = mapper.createObjectNode();
    arguments.put(Constants.NAMESPACE, SOME_NAMESPACE);
    arguments.put(Constants.TABLE, SOME_TABLE);
    arguments.set(Constants.PARTITION_KEY, partitionKey);
    arguments.put(Constants.CLUSTERING_KEY, "invalid");

    // Act Assert
    assertThatThrownBy(() -> putToMutableDatabase.invoke(database, arguments, null, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT);
    verify(database, never()).put(any());
  }

  @Test
  public void invoke_InvalidColumnsGiven_ShouldThrowContractContextException() {
    // Arrange
    ArrayNode partitionKey = mapper.createArrayNode().add(SOME_TEXT_COLUMN1);
    ObjectNode arguments = mapper.createObjectNode();
    arguments.put(Constants.NAMESPACE, SOME_NAMESPACE);
    arguments.put(Constants.TABLE, SOME_TABLE);
    arguments.set(Constants.PARTITION_KEY, partitionKey);
    arguments.put(Constants.COLUMNS, "invalid");

    // Act Assert
    assertThatThrownBy(() -> putToMutableDatabase.invoke(database, arguments, null, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT);
    verify(database, never()).put(any());
  }

  @Test
  public void invoke_ColumnWithoutValueGiven_ShouldThrowContractContextException() {
    // Arrange
    ArrayNode partitionKey = mapper.createArrayNode().add(SOME_TEXT_COLUMN1);
    ArrayNode columns = mapper.createArrayNode().add(SOME_COLUMN_WITHOUT_VALUE);
    ObjectNode arguments = mapper.createObjectNode();
    arguments.put(Constants.NAMESPACE, SOME_NAMESPACE);
    arguments.put(Constants.TABLE, SOME_TABLE);
    arguments.set(Constants.PARTITION_KEY, partitionKey);
    arguments.set(Constants.COLUMNS, columns);

    // Act Assert
    assertThatThrownBy(() -> putToMutableDatabase.invoke(database, arguments, null, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT);
    verify(database, never()).put(any());
  }

  @Test
  public void invoke_ColumnWithInvalidTypeGiven_ShouldThrowContractContextException() {
    // Arrange
    ArrayNode partitionKey = mapper.createArrayNode().add(SOME_TEXT_COLUMN1);
    ArrayNode columns = mapper.createArrayNode().add(SOME_COLUMN_WITH_INVALID_TYPE);
    ObjectNode arguments = mapper.createObjectNode();
    arguments.put(Constants.NAMESPACE, SOME_NAMESPACE);
    arguments.put(Constants.TABLE, SOME_TABLE);
    arguments.set(Constants.PARTITION_KEY, partitionKey);
    arguments.set(Constants.COLUMNS, columns);

    // Act Assert
    assertThatThrownBy(() -> putToMutableDatabase.invoke(database, arguments, null, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT);
    verify(database, never()).put(any());
  }

  @Test
  public void invoke_ColumnsWithUnmatchedTypeGiven_ShouldThrowContractContextException() {
    // Arrange
    Builder<JsonNode, String> builder = ImmutableMap.builder();
    builder
        .put(
            mapper
                .createObjectNode()
                .put(Constants.COLUMN_NAME, SOME_BOOLEAN_COLUMN_NAME)
                .put(Constants.VALUE, "true")
                .put(Constants.DATA_TYPE, "BOOLEAN"),
            "TEXT 'true' with BOOLEAN data type")
        .put(
            mapper
                .createObjectNode()
                .put(Constants.COLUMN_NAME, SOME_INT_COLUMN_NAME)
                .put(Constants.VALUE, 0.0)
                .put(Constants.DATA_TYPE, "INT"),
            "DOUBLE 0.0 with INT data type")
        .put(
            mapper
                .createObjectNode()
                .put(Constants.COLUMN_NAME, SOME_INT_COLUMN_NAME)
                .put(Constants.VALUE, SOME_BIGINT_COLUMN_VALUE)
                .put(Constants.DATA_TYPE, "INT"),
            "BIGINT value with INT data type")
        .put(
            mapper
                .createObjectNode()
                .put(Constants.COLUMN_NAME, SOME_BIGINT_COLUMN_NAME)
                .put(Constants.VALUE, 0.0)
                .put(Constants.DATA_TYPE, "BIGINT"),
            "DOUBLE 0.0 with BIGINT data type")
        .put(
            mapper
                .createObjectNode()
                .put(Constants.COLUMN_NAME, SOME_FLOAT_COLUMN_NAME)
                .put(Constants.VALUE, SOME_INT_COLUMN_VALUE)
                .put(Constants.DATA_TYPE, "FLOAT"),
            "INT value with FLOAT data type")
        .put(
            mapper
                .createObjectNode()
                .put(Constants.COLUMN_NAME, SOME_DOUBLE_COLUMN_NAME)
                .put(Constants.VALUE, SOME_FLOAT_COLUMN_VALUE)
                .put(Constants.DATA_TYPE, "DOUBLE"),
            "FLOAT value with DOUBLE data type")
        .put(
            mapper
                .createObjectNode()
                .put(Constants.COLUMN_NAME, SOME_TEXT_COLUMN1_NAME)
                .put(Constants.VALUE, SOME_INT_COLUMN_VALUE)
                .put(Constants.DATA_TYPE, "TEXT"),
            "INT value with TEXT data type")
        .put(
            mapper
                .createObjectNode()
                .put(Constants.COLUMN_NAME, SOME_BLOB_COLUMN_NAME)
                .put(Constants.VALUE, SOME_BLOB_COLUMN_VALUE)
                .put(Constants.DATA_TYPE, "BLOB"),
            "BLOB value with BLOB data type");

    // Act Assert
    builder
        .build()
        .entrySet()
        .parallelStream()
        .forEach(
            entry ->
                invoke_ColumnsWithUnmatchedTypeGiven_ShouldThrowContractContextException(
                    entry.getKey(), entry.getValue()));
  }

  private void invoke_ColumnsWithUnmatchedTypeGiven_ShouldThrowContractContextException(
      JsonNode column, String description) {
    // Arrange
    ArrayNode partitionKey = mapper.createArrayNode().add(SOME_TEXT_COLUMN1);
    ArrayNode columns = mapper.createArrayNode().add(column);
    ObjectNode arguments = mapper.createObjectNode();
    arguments.put(Constants.NAMESPACE, SOME_NAMESPACE);
    arguments.put(Constants.TABLE, SOME_TABLE);
    arguments.set(Constants.PARTITION_KEY, partitionKey);
    arguments.set(Constants.COLUMNS, columns);

    // Act Assert
    assertThatThrownBy(
            () -> putToMutableDatabase.invoke(database, arguments, null, null), description)
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT);
    verify(database, never()).put(any());
  }
}
