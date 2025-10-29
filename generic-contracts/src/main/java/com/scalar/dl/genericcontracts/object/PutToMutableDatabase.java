package com.scalar.dl.genericcontracts.object;

import com.fasterxml.jackson.databind.JsonNode;
import com.scalar.db.api.Delete;
import com.scalar.db.api.Get;
import com.scalar.db.api.GetBuilder;
import com.scalar.db.api.Put;
import com.scalar.db.api.PutBuilder;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.io.BigIntColumn;
import com.scalar.db.io.BlobColumn;
import com.scalar.db.io.BooleanColumn;
import com.scalar.db.io.Column;
import com.scalar.db.io.DataType;
import com.scalar.db.io.DoubleColumn;
import com.scalar.db.io.FloatColumn;
import com.scalar.db.io.IntColumn;
import com.scalar.db.io.Key;
import com.scalar.db.io.TextColumn;
import com.scalar.dl.ledger.database.Database;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.function.JacksonBasedFunction;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class PutToMutableDatabase extends JacksonBasedFunction {

  @Nullable
  @Override
  public JsonNode invoke(
      Database<Get, Scan, Put, Delete, Result> database,
      @Nullable JsonNode functionArguments,
      JsonNode contractArguments,
      @Nullable JsonNode contractProperties) {

    if (functionArguments == null
        || !functionArguments.has(Constants.NAMESPACE)
        || !functionArguments.get(Constants.NAMESPACE).isTextual()
        || !functionArguments.has(Constants.TABLE)
        || !functionArguments.get(Constants.TABLE).isTextual()) {
      throw new ContractContextException(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT);
    }

    String namespace = functionArguments.get(Constants.NAMESPACE).textValue();
    String table = functionArguments.get(Constants.TABLE).textValue();
    Key partitionKey = getPartitionKey(functionArguments);
    Key clusteringKey = getClusteringKey(functionArguments);
    List<Column<?>> columns = getColumns(functionArguments);

    GetBuilder.BuildableGet buildableGet =
        Get.newBuilder().namespace(namespace).table(table).partitionKey(partitionKey);
    PutBuilder.Buildable buildablePut =
        Put.newBuilder().namespace(namespace).table(table).partitionKey(partitionKey);

    if (clusteringKey != null) {
      buildableGet.clusteringKey(clusteringKey);
      buildablePut.clusteringKey(clusteringKey);
    }

    columns.forEach(buildablePut::value);

    database.get(buildableGet.build());
    database.put(buildablePut.build());

    return null;
  }

  private Key getPartitionKey(JsonNode functionArgument) {
    if (!functionArgument.has(Constants.PARTITION_KEY)) {
      throw new ContractContextException(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT);
    }

    JsonNode partitionKey = functionArgument.get(Constants.PARTITION_KEY);
    if (!partitionKey.isArray() || partitionKey.isEmpty()) {
      throw new ContractContextException(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT);
    }

    Key.Builder builder = Key.newBuilder();
    partitionKey.forEach(column -> builder.add(getColumn(column)));

    return builder.build();
  }

  private Key getClusteringKey(JsonNode functionArgument) {
    if (functionArgument.has(Constants.CLUSTERING_KEY)) {
      JsonNode clusteringKey = functionArgument.get(Constants.CLUSTERING_KEY);
      if (!clusteringKey.isArray()) {
        throw new ContractContextException(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT);
      }

      if (clusteringKey.isEmpty()) {
        return null;
      }

      Key.Builder builder = Key.newBuilder();
      clusteringKey.forEach(column -> builder.add(getColumn(column)));

      return builder.build();
    } else {
      return null;
    }
  }

  private List<Column<?>> getColumns(JsonNode functionArgument) {
    List<Column<?>> resultColumns = new ArrayList<>();
    if (functionArgument.has(Constants.COLUMNS)) {
      JsonNode columns = functionArgument.get(Constants.COLUMNS);
      if (!columns.isArray()) {
        throw new ContractContextException(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT);
      }

      columns.forEach(column -> resultColumns.add(getColumn(column)));

      return resultColumns;
    }

    return resultColumns;
  }

  private Column<?> getColumn(JsonNode jsonColumn) {
    if (!jsonColumn.isObject()
        || !jsonColumn.has(Constants.COLUMN_NAME)
        || !jsonColumn.get(Constants.COLUMN_NAME).isTextual()
        || !jsonColumn.has(Constants.VALUE)
        || !jsonColumn.has(Constants.DATA_TYPE)
        || !jsonColumn.get(Constants.DATA_TYPE).isTextual()) {
      throw new ContractContextException(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT);
    }

    String columnName = jsonColumn.get(Constants.COLUMN_NAME).textValue();
    DataType dataType = getDataType(jsonColumn.get(Constants.DATA_TYPE).textValue());
    JsonNode value = jsonColumn.get(Constants.VALUE);

    if (value.isNull()) {
      return createNullColumn(columnName, dataType);
    }

    // Use the `if` statement instead of `switch` because it creates a separate anonymous inner
    // class. We only support a single class for each function registration.
    if (dataType.equals(DataType.BOOLEAN)) {
      if (!value.isBoolean()) {
        throw new ContractContextException(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT);
      }
      return BooleanColumn.of(columnName, value.booleanValue());
    }

    if (dataType.equals(DataType.INT)) {
      if (!value.isInt()) {
        throw new ContractContextException(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT);
      }
      return IntColumn.of(columnName, value.intValue());
    }

    if (dataType.equals(DataType.BIGINT)) {
      if (!value.isInt() && !value.isLong()) {
        throw new ContractContextException(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT);
      }
      return BigIntColumn.of(columnName, value.longValue());
    }

    if (dataType.equals(DataType.FLOAT)) {
      // The JSON deserializer does not distinguish between float and double values; all JSON
      // numbers with a decimal point are deserialized as double. Therefore, we check for isDouble()
      // here even for FLOAT columns.
      if (!value.isDouble()) {
        throw new ContractContextException(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT);
      }
      return FloatColumn.of(columnName, value.floatValue());
    }

    if (dataType.equals(DataType.DOUBLE)) {
      if (!value.isDouble()) {
        throw new ContractContextException(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT);
      }
      return DoubleColumn.of(columnName, value.doubleValue());
    }

    if (dataType.equals(DataType.TEXT)) {
      if (!value.isTextual()) {
        throw new ContractContextException(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT);
      }
      return TextColumn.of(columnName, value.textValue());
    }

    if (dataType.equals(DataType.BLOB)) {
      // BLOB data is expected as a Base64-encoded string due to JSON limitations. JSON cannot
      // represent binary data directly, so BLOBs must be provided as Base64-encoded strings.
      if (!value.isTextual()) {
        throw new ContractContextException(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT);
      }
      try {
        return BlobColumn.of(columnName, value.binaryValue());
      } catch (IOException e) {
        throw new ContractContextException(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT);
      }
    }

    throw new ContractContextException(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT);
  }

  private Column<?> createNullColumn(String columnName, DataType dataType) {
    // Use the `if` statement instead of `switch` because it creates a separate anonymous inner
    // class. We only support a single class for each function registration.
    if (dataType.equals(DataType.BOOLEAN)) {
      return BooleanColumn.ofNull(columnName);
    }

    if (dataType.equals(DataType.INT)) {
      return IntColumn.ofNull(columnName);
    }

    if (dataType.equals(DataType.BIGINT)) {
      return BigIntColumn.ofNull(columnName);
    }

    if (dataType.equals(DataType.FLOAT)) {
      return FloatColumn.ofNull(columnName);
    }

    if (dataType.equals(DataType.DOUBLE)) {
      return DoubleColumn.ofNull(columnName);
    }

    if (dataType.equals(DataType.TEXT)) {
      return TextColumn.ofNull(columnName);
    }

    if (dataType.equals(DataType.BLOB)) {
      return BlobColumn.ofNull(columnName);
    }

    throw new ContractContextException(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT);
  }

  private DataType getDataType(String dataType) {
    switch (dataType.toUpperCase()) {
      case "BOOLEAN":
        return DataType.BOOLEAN;
      case "INT":
        return DataType.INT;
      case "BIGINT":
        return DataType.BIGINT;
      case "FLOAT":
        return DataType.FLOAT;
      case "DOUBLE":
        return DataType.DOUBLE;
      case "TEXT":
        return DataType.TEXT;
      case "BLOB":
        return DataType.BLOB;
      default:
        throw new ContractContextException(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT);
    }
  }
}
