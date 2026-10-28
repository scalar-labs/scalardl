package com.scalar.dl.ledger.config;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.scalar.dl.ledger.error.CommonError;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import javax.annotation.Nullable;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookupFactory;

/**
 * A utility class for retrieving configuration values from {@link Properties}. You can use
 * placeholders in the values, and they are replaced with environment variables
 * (${env:&#60;environment variable name&#62;}) or system properties (${sys:&#60;system property
 * name&#62;}). You can also specify default values in placeholders like ${sys:&#60;system property
 * name&#62;:-defaultValue}.
 */
public final class ConfigUtils {

  /**
   * A {@link StringSubstitutor} instance with the environment variable string lookup and system
   * property string lookup.
   */
  private static final StringSubstitutor stringSubstitutor =
      new StringSubstitutor(
          StringLookupFactory.INSTANCE.interpolatorStringLookup(
              ImmutableMap.of(
                  StringLookupFactory.KEY_ENV,
                  StringLookupFactory.INSTANCE.environmentVariableStringLookup(),
                  StringLookupFactory.KEY_SYS,
                  StringLookupFactory.INSTANCE.systemPropertyStringLookup()),
              StringLookupFactory.INSTANCE.nullStringLookup(),
              false));

  private ConfigUtils() {}

  @Nullable
  public static String getString(
      Properties properties, String name, @Nullable String defaultValue) {
    String value = trimAndReplace(properties.getProperty(name));
    if (Strings.isNullOrEmpty(value)) {
      return defaultValue;
    }
    return value;
  }

  public static int getInt(Properties properties, String name, int defaultValue) {
    String value = trimAndReplace(properties.getProperty(name));
    if (Strings.isNullOrEmpty(value)) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ignored) {
      throw new IllegalArgumentException(
          CommonError.CONFIG_UTILS_INVALID_NUMBER_FORMAT.buildMessage(name, value));
    }
  }

  @Nullable
  public static Integer getInt(Properties properties, String name, @Nullable Integer defaultValue) {
    String value = trimAndReplace(properties.getProperty(name));
    if (Strings.isNullOrEmpty(value)) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ignored) {
      throw new IllegalArgumentException(
          CommonError.CONFIG_UTILS_INVALID_NUMBER_FORMAT.buildMessage(name, value));
    }
  }

  public static long getLong(Properties properties, String name, long defaultValue) {
    String value = trimAndReplace(properties.getProperty(name));
    if (Strings.isNullOrEmpty(value)) {
      return defaultValue;
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ignored) {
      throw new IllegalArgumentException(
          CommonError.CONFIG_UTILS_INVALID_NUMBER_FORMAT.buildMessage(name, value));
    }
  }

  @Nullable
  public static Long getLong(Properties properties, String name, @Nullable Long defaultValue) {
    String value = trimAndReplace(properties.getProperty(name));
    if (Strings.isNullOrEmpty(value)) {
      return defaultValue;
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ignored) {
      throw new IllegalArgumentException(
          CommonError.CONFIG_UTILS_INVALID_NUMBER_FORMAT.buildMessage(name, value));
    }
  }

  public static boolean getBoolean(Properties properties, String name, boolean defaultValue) {
    String value = trimAndReplace(properties.getProperty(name));
    if (Strings.isNullOrEmpty(value)) {
      return defaultValue;
    }
    if (Boolean.TRUE.toString().equalsIgnoreCase(value)
        || Boolean.FALSE.toString().equalsIgnoreCase(value)) {
      return Boolean.parseBoolean(value);
    } else {
      throw new IllegalArgumentException(
          CommonError.CONFIG_UTILS_INVALID_BOOLEAN_FORMAT.buildMessage(name, value));
    }
  }

  @Nullable
  public static Boolean getBoolean(
      Properties properties, String name, @Nullable Boolean defaultValue) {
    String value = trimAndReplace(properties.getProperty(name));
    if (Strings.isNullOrEmpty(value)) {
      return defaultValue;
    }
    if (Boolean.TRUE.toString().equalsIgnoreCase(value)
        || Boolean.FALSE.toString().equalsIgnoreCase(value)) {
      return Boolean.parseBoolean(value);
    } else {
      throw new IllegalArgumentException(
          CommonError.CONFIG_UTILS_INVALID_BOOLEAN_FORMAT.buildMessage(name, value));
    }
  }

  @Nullable
  public static String[] getStringArray(
      Properties properties, String name, @Nullable String[] defaultValue) {
    String value = trimAndReplace(properties.getProperty(name));
    if (Strings.isNullOrEmpty(value)) {
      return defaultValue;
    }
    return value.split("\\s*,\\s*");
  }

  /**
   * Returns the properties whose names start with the specified prefix as a map. Each key of the
   * map is the remainder of the property name after the prefix. The values are trimmed and the
   * placeholders in them are replaced in the same way as {@link #getString(Properties, String,
   * String)}, and the properties with empty values are excluded.
   *
   * @param properties the properties
   * @param prefix the prefix of the property names
   * @return an immutable map of the remainders of the property names to their values
   * @throws IllegalArgumentException if a property name has no remainder after the prefix
   */
  public static Map<String, String> getStringMapByPrefix(Properties properties, String prefix) {
    Map<String, String> map = new TreeMap<>();
    for (String name : properties.stringPropertyNames()) {
      if (!name.startsWith(prefix)) {
        continue;
      }
      String key = name.substring(prefix.length()).trim();
      if (key.isEmpty()) {
        throw new IllegalArgumentException(
            CommonError.CONFIG_UTILS_NAME_AFTER_PREFIX_REQUIRED.buildMessage(name, prefix));
      }
      String value = getString(properties, name, null);
      if (value != null) {
        map.put(key, value);
      }
    }
    return ImmutableMap.copyOf(map);
  }

  @Nullable
  public static String getStringFromFilePath(
      Properties properties, String pathName, @Nullable String defaultValue) {
    String path = trimAndReplace(properties.getProperty(pathName));
    if (Strings.isNullOrEmpty(path)) {
      return defaultValue;
    }
    try {
      return new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(
          CommonError.CONFIG_UTILS_READING_FILE_FAILED.buildMessage(path), e);
    }
  }

  @VisibleForTesting
  static String trimAndReplace(@Nullable String value) {
    if (value == null) {
      return null;
    }
    return stringSubstitutor.replace(value.trim());
  }
}
