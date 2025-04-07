package com.scalar.dl.genericcontracts.table.v1_0_0;

public class Constants {

  // Metadata
  public static final String PACKAGE = "table";
  public static final String VERSION = "v1_0_0";
  public static final String CONTRACT_SCAN = PACKAGE + "." + VERSION + ".Scan";

  // Constants
  public static final String PREFIX_TABLE = "tbl_";
  public static final String PREFIX_INDEX = "idx_";
  public static final String PREFIX_RECORD = "rec_";
  public static final String ASSET_ID_METADATA_TABLES = "metadata_tables";
  public static final String ASSET_ID_SEPARATOR = "_";
  public static final String TABLE_NAME = "name";
  public static final String TABLE_KEY = "key";
  public static final String TABLE_KEY_TYPE = "type";
  public static final String TABLE_INDEXES = "indexes";
  public static final String INDEX_KEY = "key";
  public static final String INDEX_KEY_TYPE = "type";
  public static final String RECORD_TABLE = "table";
  public static final String RECORD_VALUES = "values";
  public static final String QUERY_TABLE = "table";
  public static final String QUERY_CONDITIONS = "conditions";
  public static final String QUERY_PROJECTIONS = "projections";
  public static final String CONDITION_COLUMN = "column";
  public static final String CONDITION_VALUE = "value";
  public static final String CONDITION_OPERATOR = "operator";
  public static final String OPERATOR_EQ = "EQ";
  public static final String OPERATOR_NE = "NE";
  public static final String OPERATOR_LT = "LT";
  public static final String OPERATOR_LTE = "LTE";
  public static final String OPERATOR_GT = "GT";
  public static final String OPERATOR_GTE = "GTE";
  public static final String OPERATOR_IS_NULL = "IS_NULL";
  public static final String OPERATOR_IS_NOT_NULL = "IS_NOT_NULL";
  public static final String ASSET_AGE = "age";

  // Error messages
  public static final String INVALID_TABLE_FORMAT = "The specified format of the table is invalid.";
  public static final String INVALID_INDEX_FORMAT =
      "The specified format of the indexes is invalid.";
  public static final String INVALID_RECORD_FORMAT =
      "The specified format of the record is invalid.";
  public static final String INVALID_QUERY_FORMAT = "The specified format of the query is invalid.";
  public static final String INVALID_CONDITION_FORMAT =
      "The specified format of the condition is invalid. Condition: ";
  public static final String INVALID_PROJECTIONS_FORMAT =
      "The specified format of the projections.";
  public static final String INVALID_KEY_TYPE = "The specified key type is invalid.";
  public static final String INVALID_INDEX_KEY_TYPE = "The specified index key type is invalid.";
  public static final String INVALID_OPERATOR = "The specified operator is invalid. Condition: ";
  public static final String INVALID_KEY_SPECIFICATION =
      "At least a condition for the primary key or index key must be properly specified.";
  public static final String TABLE_ALREADY_EXISTS = "The specified table already exists.";
  public static final String TABLE_NOT_EXIST = "The specified table does not exist.";
  public static final String RECORD_KEY_NOT_EXIST = "The record values must have the key.";
  public static final String RECORD_ALREADY_EXISTS = "The specified record already exists.";

  // Internal error messages due to a bug or tampering
  public static final String ILLEGAL_INDEX_STATE = "The state of the index is illegal.";
  public static final String ILLEGAL_ARGUMENT = "The specified argument is illegal.";
}
