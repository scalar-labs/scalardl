package com.scalar.dl.genericcontracts.table.v1_0_0;

import java.util.regex.Pattern;

public class Constants {

  // Metadata
  public static final String PACKAGE = "table";
  public static final String VERSION = "v1_0_0";
  public static final String CONTRACT_CREATE = PACKAGE + "." + VERSION + ".Create";
  public static final String CONTRACT_INSERT = PACKAGE + "." + VERSION + ".Insert";
  public static final String CONTRACT_SELECT = PACKAGE + "." + VERSION + ".Select";
  public static final String CONTRACT_UPDATE = PACKAGE + "." + VERSION + ".Update";
  public static final String CONTRACT_GET_ASSET_ID = PACKAGE + "." + VERSION + ".GetAssetId";
  public static final String CONTRACT_SCAN = PACKAGE + "." + VERSION + ".Scan";

  // Constants
  public static final String PREFIX_TABLE = "tbl_";
  public static final String PREFIX_INDEX = "idx_";
  public static final String PREFIX_RECORD = "rec_";
  public static final String ASSET_ID_METADATA_TABLES = "metadata_tables";
  public static final String ASSET_ID_PREFIX = "prefix";
  public static final String ASSET_ID_VALUES = "values";
  public static final String ASSET_ID_SEPARATOR = "_";
  public static final String TABLE_NAME = "name";
  public static final String TABLE_KEY = "key";
  public static final String TABLE_KEY_TYPE = "type";
  public static final String TABLE_INDEXES = "indexes";
  public static final String INDEX_KEY = "key";
  public static final String INDEX_KEY_TYPE = "type";
  public static final String INDEX_ASSET_ADDED_AGE = "age";
  public static final String INDEX_ASSET_DELETE_MARKER = "deleted";
  public static final String RECORD_TABLE = "table";
  public static final String RECORD_KEY = "key";
  public static final String RECORD_VALUES = "values";
  public static final String QUERY_TABLE = "table";
  public static final String QUERY_JOINS = "joins";
  public static final String QUERY_CONDITIONS = "conditions";
  public static final String QUERY_PROJECTIONS = "projections";
  public static final String JOIN_TABLE = "table";
  public static final String JOIN_LEFT_KEY = "left";
  public static final String JOIN_RIGHT_KEY = "right";
  public static final String UPDATE_TABLE = "table";
  public static final String UPDATE_VALUES = "values";
  public static final String UPDATE_CONDITIONS = "conditions";
  public static final String HISTORY_LIMIT = "limit";
  public static final String HISTORY_ASSET_AGE = "age";
  public static final String ALIAS_NAME = "name";
  public static final String ALIAS_AS = "alias";
  public static final String CONDITION_COLUMN = "column";
  public static final String CONDITION_VALUE = "value";
  public static final String CONDITION_OPERATOR = "operator";
  public static final String COLUMN_SEPARATOR = ".";
  public static final String OPERATOR_EQ = "EQ";
  public static final String OPERATOR_NE = "NE";
  public static final String OPERATOR_LT = "LT";
  public static final String OPERATOR_LTE = "LTE";
  public static final String OPERATOR_GT = "GT";
  public static final String OPERATOR_GTE = "GTE";
  public static final String OPERATOR_IS_NULL = "IS_NULL";
  public static final String OPERATOR_IS_NOT_NULL = "IS_NOT_NULL";
  public static final String SCAN_OPTIONS = "options";
  public static final String SCAN_OPTIONS_INCLUDE_METADATA = "include_metadata";
  public static final String SCAN_OPTIONS_TABLE_REFERENCE = "table_reference";
  public static final String SCAN_METADATA_AGE = "age$";

  // Patterns
  public static final String OBJECT_NAME_PATTERN = "[a-zA-Z][A-Za-z0-9_]*";
  public static final Pattern OBJECT_NAME = Pattern.compile(OBJECT_NAME_PATTERN);
  public static final Pattern COLUMN_REFERENCE =
      Pattern.compile(OBJECT_NAME_PATTERN + "\\." + OBJECT_NAME_PATTERN);

  // Error messages
  public static final String INVALID_CONTRACT_ARGUMENTS =
      "The specified format of the contract arguments is invalid.";
  public static final String INVALID_TABLE_FORMAT = "The specified format of the table is invalid.";
  public static final String INVALID_INDEX_FORMAT =
      "The specified format of the indexes is invalid.";
  public static final String INVALID_RECORD_FORMAT =
      "The specified format of the record is invalid.";
  public static final String INVALID_QUERY_FORMAT = "The specified format of the query is invalid.";
  public static final String INVALID_QUERY_TABLE_FORMAT =
      "The specified format of the query table is invalid. Table: ";
  public static final String INVALID_COLUMN_FORMAT =
      "The specified format of the column is invalid. Column: ";
  public static final String INVALID_CONDITION_FORMAT =
      "The specified format of the condition is invalid. Condition: ";
  public static final String INVALID_PROJECTION_FORMAT =
      "The specified format of the projection is invalid. Projection: ";
  public static final String INVALID_JOIN_FORMAT =
      "The specified format of the join is invalid. Join: ";
  public static final String INVALID_UPDATE_FORMAT =
      "The specified format of the update is invalid.";
  public static final String INVALID_KEY_TYPE = "The specified key type is invalid. Type: ";
  public static final String INVALID_INDEX_KEY_TYPE =
      "The specified index key type is invalid. Type: ";
  public static final String INVALID_OPERATOR = "The specified operator is invalid. Condition: ";
  public static final String INVALID_KEY_SPECIFICATION =
      "At least a condition for the primary key or index key must be properly specified.";
  public static final String INVALID_JOIN_COLUMN =
      "The join column in the right table must be the primary key or index key. Column: ";
  public static final String INVALID_OBJECT_NAME = "The specified name is invalid: ";
  public static final String TABLE_ALREADY_EXISTS = "The specified table already exists.";
  public static final String TABLE_NOT_EXIST = "The specified table does not exist. Table: ";
  public static final String TABLE_AMBIGUOUS =
      "The specified table name or alias must be unique. Table: ";
  public static final String COLUMN_AMBIGUOUS =
      "The specified column name must be unique. Column: ";
  public static final String RECORD_KEY_NOT_EXIST = "The record values must have the key.";
  public static final String RECORD_ALREADY_EXISTS = "The specified record already exists.";
  public static final String UNKNOWN_TABLE = "The specified table is unknown. Table: ";
  public static final String CANNOT_UPDATE_KEY = "The primary key cannot be updated.";

  // Internal error messages due to a bug or tampering
  public static final String ILLEGAL_INDEX_STATE = "The state of the index is illegal.";
  public static final String ILLEGAL_ARGUMENT = "The specified argument is illegal.";
}
