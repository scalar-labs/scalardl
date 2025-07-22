package com.scalar.dl.genericcontracts.object;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;

import java.time.ZoneOffset;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;

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
      "The specified format of the PutToMutableDatabase function argument is invalid.";

  /** A formatter for a DATE literal. The format is "YYYY-MM-DD". For example, "2020-03-04". */
  public static final DateTimeFormatter DATE_FORMATTER =
      new DateTimeFormatterBuilder()
          .appendValue(YEAR, 4, 4, SignStyle.NEVER)
          .appendLiteral('-')
          .appendValue(MONTH_OF_YEAR, 2)
          .appendLiteral('-')
          .appendValue(DAY_OF_MONTH, 2)
          .toFormatter()
          .withResolverStyle(ResolverStyle.STRICT)
          .withChronology(IsoChronology.INSTANCE);
  /**
   * A formatter for a TIME literal. The format is "HH:MM:SS[.FFFFFF]". For example,
   * "12:34:56.123456". The fractional second is optional.
   */
  public static final DateTimeFormatter TIME_FORMATTER =
      new DateTimeFormatterBuilder()
          .appendValue(HOUR_OF_DAY, 2)
          .appendLiteral(':')
          .appendValue(MINUTE_OF_HOUR, 2)
          .optionalStart()
          .appendLiteral(':')
          .appendValue(SECOND_OF_MINUTE, 2)
          .optionalStart()
          .appendFraction(NANO_OF_SECOND, 0, 6, true)
          .toFormatter()
          .withResolverStyle(ResolverStyle.STRICT)
          .withChronology(IsoChronology.INSTANCE);
  /**
   * A formatter for a TIMESTAMP literal. The format is "YYYY-MM-DD HH:MM:SS[.FFF]". For example,
   * "2020-03-04 12:34:56.123". The fractional second is optional.
   */
  public static final DateTimeFormatter TIMESTAMP_FORMATTER =
      new DateTimeFormatterBuilder()
          .append(DATE_FORMATTER)
          .appendLiteral(' ')
          .appendValue(HOUR_OF_DAY, 2)
          .appendLiteral(':')
          .appendValue(MINUTE_OF_HOUR, 2)
          .optionalStart()
          .appendLiteral(':')
          .appendValue(SECOND_OF_MINUTE, 2)
          .optionalStart()
          .appendFraction(NANO_OF_SECOND, 0, 3, true)
          .toFormatter();
  /**
   * A formatter for a TIMESTAMPTZ literal. The format is "YYYY-MM-DD HH:MM:SS[.FFF] Z". For
   * example, "2020-03-04 12:34:56.123 Z". The fractional second is optional.
   */
  public static final DateTimeFormatter TIMESTAMPTZ_FORMATTER =
      new DateTimeFormatterBuilder()
          .append(TIMESTAMP_FORMATTER)
          .appendLiteral(' ')
          .appendLiteral('Z')
          .toFormatter()
          .withZone(ZoneOffset.UTC);
}
