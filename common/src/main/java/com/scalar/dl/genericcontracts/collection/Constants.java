package com.scalar.dl.genericcontracts.collection;

public class Constants {

  // Collection authenticity management
  public static final String COLLECTION_ID = "collection_id";
  public static final String COLLECTION_ID_PREFIX = "c_";
  public static final String OBJECT_IDS = "object_ids";
  public static final String COLLECTION_EVENTS = "collection_events";
  public static final String COLLECTION_AGE = "age";
  public static final String COLLECTION_SNAPSHOT = "snapshot";
  public static final String COLLECTION_CHECKPOINT_INTERVAL = "checkpoint_interval";
  public static final String OPERATION_TYPE = "operation_type";
  public static final String OPERATION_CREATE = "create";
  public static final String OPERATION_ADD = "add";
  public static final String OPERATION_REMOVE = "remove";
  public static final String OPTIONS = "options";
  public static final String OPTION_FORCE = "force";
  public static final String OPTION_LIMIT = "limit";
  public static final int MIN_COLLECTION_CHECKPOINT_INTERVAL = 1;
  public static final int DEFAULT_COLLECTION_CHECKPOINT_INTERVAL = 10;

  // Contracts
  public static final String PACKAGE = "collection";
  public static final String VERSION = "v1_0_0";
  public static final String CONTRACT_ADD = PACKAGE + "." + VERSION + ".Add";
  public static final String CONTRACT_CREATE = PACKAGE + "." + VERSION + ".Create";
  public static final String CONTRACT_GET = PACKAGE + "." + VERSION + ".Get";
  public static final String CONTRACT_GET_CHECKPOINT_INTERVAL =
      PACKAGE + "." + VERSION + ".GetCheckpointInterval";
  public static final String CONTRACT_GET_HISTORY = PACKAGE + "." + VERSION + ".GetHistory";
  public static final String CONTRACT_REMOVE = PACKAGE + "." + VERSION + ".Remove";

  // Error messages
  public static final String COLLECTION_ID_IS_MISSING_OR_INVALID =
      "The collection ID is not specified in the arguments or is invalid.";
  public static final String OBJECT_IDS_ARE_MISSING =
      "The object IDs are not specified in the arguments.";
  public static final String INVALID_OBJECT_IDS_FORMAT =
      "The specified format of the object IDs is invalid.";
  public static final String INVALID_CONTRACT_PROPERTIES_FORMAT =
      "The specified format of the contract properties is invalid.";
  public static final String INVALID_OPTIONS_FORMAT =
      "The specified format of the options is invalid.";
  public static final String COLLECTION_ALREADY_EXISTS = "The specified collection already exists.";
  public static final String COLLECTION_NOT_FOUND = "The specified collection is not found.";
  public static final String OBJECT_ALREADY_EXISTS_IN_COLLECTION =
      "One of the specified objects already exists in the collection.";
  public static final String OBJECT_NOT_FOUND_IN_COLLECTION =
      "One of the specified objects is not found in the collection.";
  public static final String INVALID_CHECKPOINT =
      "The checkpoint for the specified collection is invalid. Check if the correct contract and its properties are used.";

  // Internal error messages
  public static final String ILLEGAL_ASSET_STATE = "The state of the specified asset is illegal.";
}
