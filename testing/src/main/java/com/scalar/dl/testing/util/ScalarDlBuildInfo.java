package com.scalar.dl.testing.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/** Provides build information such as project version. */
public final class ScalarDlBuildInfo {

  private static final String BUILD_PROPERTIES_PATH = "/com/scalar/dl/testing/build.properties";
  private static final String VERSION = loadVersion();

  private ScalarDlBuildInfo() {}

  /**
   * Returns the project version.
   *
   * @return The project version
   * @throws IllegalStateException if the version is not set or build.properties is not found
   */
  public static String getVersion() {
    return VERSION;
  }

  private static String loadVersion() {
    try (InputStream is = ScalarDlBuildInfo.class.getResourceAsStream(BUILD_PROPERTIES_PATH)) {
      if (is == null) {
        throw new IllegalStateException(
            "build.properties not found. Ensure the project is built with Gradle.");
      }
      Properties props = new Properties();
      props.load(is);
      String version = props.getProperty("version");
      if (version == null || version.isEmpty() || version.equals("${version}")) {
        throw new IllegalStateException(
            "Project version not set in build.properties. Ensure the project is built with Gradle.");
      }
      return version;
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to load build.properties", e);
    }
  }
}
