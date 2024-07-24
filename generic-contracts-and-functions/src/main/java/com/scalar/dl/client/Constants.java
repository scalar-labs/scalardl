package com.scalar.dl.client;

public class Constants {

  public static final String OBJECT_ID = "object_id";
  public static final String HASH_VALUE = "hash_value";
  public static final String HASH_VALUES = "hash_values";
  public static final String PROPERTIES = "properties";
  public static final String STATUS = "status";
  public static final String FAULTY_VERSIONS = "faulty_versions";

  public static final String STATUS_NORMAL = "normal";
  public static final String STATUS_FAULTY = "faulty";

  // Error messages
  public static final String OBJECT_ID_IS_MISSING_OR_INVALID =
      "The object ID is not specified in the arguments or is invalid.";
  public static final String HASH_VALUE_IS_MISSING_OR_INVALID =
      "The hash value is not specified in the arguments or is invalid.";
  public static final String HASH_VALUES_ARE_MISSING =
      "The hash values are not specified in the arguments.";
  public static final String INVALID_PROPERTIES_FORMAT =
      "The specified format of the properties is invalid.";
  public static final String INVALID_HASH_VALUES_FORMAT =
      "The specified format of the hash values is invalid.";
}
