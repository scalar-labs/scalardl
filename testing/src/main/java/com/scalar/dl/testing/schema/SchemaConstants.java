package com.scalar.dl.testing.schema;

import com.scalar.db.api.TableMetadata;
import com.scalar.db.io.DataType;

/** Constants for ScalarDL schema (namespace, table, column names). */
public final class SchemaConstants {
  // Ledger and Auditor namespaces and tables
  public static final String SCALAR_NAMESPACE = "scalar";
  public static final String AUDITOR_NAMESPACE = "auditor";
  public static final String ASSET_TABLE = "asset";
  public static final String ASSET_METADATA_TABLE = "asset_metadata";
  public static final String ASSET_LOCK_TABLE = "asset_lock";
  public static final String REQUEST_PROOF_TABLE = "request_proof";

  // Asset table columns
  public static final String ASSET_ID_COLUMN_NAME = "id";
  public static final String ASSET_AGE_COLUMN_NAME = "age";
  public static final String ASSET_INPUT_COLUMN_NAME = "input";
  public static final String ASSET_OUTPUT_COLUMN_NAME = "output";
  public static final String ASSET_ARGUMENT_COLUMN_NAME = "argument";
  public static final String ASSET_PREV_HASH_COLUMN_NAME = "prev_hash";

  // Function namespace and tables
  public static final String FUNCTION_NAMESPACE = "function_test";
  public static final String FUNCTION_TABLE = "function_test";

  // Function table schema
  public static final TableMetadata FUNCTION_TABLE_METADATA =
      TableMetadata.newBuilder()
          .addColumn("id", DataType.TEXT)
          .addColumn("balance", DataType.INT)
          .addPartitionKey("id")
          .build();

  private SchemaConstants() {}
}
