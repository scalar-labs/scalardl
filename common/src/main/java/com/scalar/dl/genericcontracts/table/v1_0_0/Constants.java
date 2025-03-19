package com.scalar.dl.genericcontracts.table.v1_0_0;

public class Constants {

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
  public static final String ASSET_AGE = "age";

  // Error messages
  public static final String INVALID_TABLE_FORMAT = "The specified format of the table is invalid.";
  public static final String INVALID_INDEX_FORMAT =
      "The specified format of the indexes is invalid.";
  public static final String INVALID_RECORD_FORMAT =
      "The specified format of the record is invalid.";
  public static final String INVALID_KEY_TYPE = "The specified key type is invalid.";
  public static final String INVALID_INDEX_KEY_TYPE = "The specified index key type is invalid.";
  public static final String TABLE_ALREADY_EXISTS = "The specified table already exists.";
  public static final String TABLE_NOT_EXIST = "The specified table does not exist.";
  public static final String RECORD_KEY_NOT_EXIST = "The record values must have the key.";
  public static final String RECORD_ALREADY_EXISTS = "The specified record already exists.";
}
