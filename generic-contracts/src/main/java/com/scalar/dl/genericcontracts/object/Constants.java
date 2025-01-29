package com.scalar.dl.genericcontracts.object;

public class Constants {

  // Object authenticity management
  public static final String OBJECT_ID = "object_id";
  public static final String OBJECT_ID_PREFIX = "o_";
  public static final String VERSIONS = "versions";
  public static final String VERSION_ID = "version_id";
  public static final String HASH_VALUE = "hash_value";
  public static final String METADATA = "metadata";
  public static final String STATUS = "status";
  public static final String STATUS_CORRECT = "correct";
  public static final String STATUS_FAULTY = "faulty";
  public static final String DETAILS = "details";
  public static final String DETAILS_CORRECT_STATUS = "The status is correct.";
  public static final String DETAILS_NUMBER_OF_VERSIONS_MISMATCH =
      "The number of versions is mismatched.";
  public static final String DETAILS_FAULTY_VERSIONS_EXIST = "A faulty version is found.";
  public static final String FAULTY_VERSIONS = "faulty_versions";
  public static final String GIVEN_VERSIONS = "corresponding_given_versions";

  // Function-related
  public static final String NAMESPACE = "namespace";
  public static final String TABLE = "table";
  public static final String PARTITION_KEY = "partition_key";
  public static final String CLUSTERING_KEY = "clustering_key";
  public static final String COLUMNS = "columns";
  public static final String COLUMN_NAME = "column_name";
  public static final String VALUE = "value";
  public static final String DATA_TYPE = "data_type";

  // Options
  public static final String OPTIONS = "options";
  public static final String OPTION_ALL = "all";
  public static final String OPTION_VERBOSE = "verbose";

  // Error messages
  public static final String OBJECT_ID_IS_MISSING_OR_INVALID =
      "The object ID is not specified in the arguments or is invalid.";
  public static final String HASH_VALUE_IS_MISSING_OR_INVALID =
      "The hash value is not specified in the arguments or is invalid.";
  public static final String VERSIONS_ARE_MISSING =
      "The versions are not specified in the arguments.";
  public static final String INVALID_METADATA_FORMAT =
      "The specified format of the metadata is invalid.";
  public static final String INVALID_VERSIONS_FORMAT =
      "The specified format of the version information is invalid.";
  public static final String COLLECTION_ID_IS_MISSING_OR_INVALID =
      "The collection ID is not specified in the arguments or is invalid.";
  public static final String INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT =
      "The specified format of the PutMutable function argument is invalid.";
}
