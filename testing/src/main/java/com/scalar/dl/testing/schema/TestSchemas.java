package com.scalar.dl.testing.schema;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Utility class for loading test schema files from resources. */
public final class TestSchemas {
  private static final String LEDGER_SCHEMA = "/com/scalar/dl/testing/schema/ledger-schema.json";
  private static final String AUDITOR_SCHEMA = "/com/scalar/dl/testing/schema/auditor-schema.json";

  /**
   * Returns the Ledger schema as a JSON string.
   *
   * @return the Ledger schema JSON
   */
  public static String getLedgerSchema() {
    return loadResource(LEDGER_SCHEMA);
  }

  /**
   * Returns the Auditor schema as a JSON string.
   *
   * @return the Auditor schema JSON
   */
  public static String getAuditorSchema() {
    return loadResource(AUDITOR_SCHEMA);
  }

  private static String loadResource(String path) {
    try (InputStream is = TestSchemas.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IllegalStateException("Resource not found: " + path);
      }
      return new String(readAllBytes(is), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to load resource: " + path, e);
    }
  }

  private static byte[] readAllBytes(InputStream is) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] data = new byte[4096];
    int bytesRead;
    while ((bytesRead = is.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, bytesRead);
    }
    return buffer.toByteArray();
  }

  private TestSchemas() {}
}
