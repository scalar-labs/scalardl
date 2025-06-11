package com.scalar.dl.client.util;

import com.scalar.dl.client.error.ClientError;
import com.scalar.dl.client.exception.ClientException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class Common {
  private static final int CLASS_LOAD_BUFFER_SIZE = 4096;

  public static byte[] fileToBytes(String filePath) {
    try {
      return Files.readAllBytes(new File(filePath).toPath());
    } catch (IOException e) {
      throw new ClientException(ClientError.READING_FILE_FAILED, e, filePath, e.getMessage());
    }
  }

  public static byte[] getClassBytes(Class<?> clazz) {
    String classResourcePath = clazz.getName().replace('.', '/') + ".class";
    try (InputStream is = clazz.getClassLoader().getResourceAsStream(classResourcePath)) {
      if (is == null) {
        throw new RuntimeException(
            ClientError.CLASS_FILE_LOAD_FAILED.buildMessage(clazz.getName()));
      }

      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      byte[] tmp = new byte[CLASS_LOAD_BUFFER_SIZE];
      int bytesRead;
      while ((bytesRead = is.read(tmp)) != -1) {
        buffer.write(tmp, 0, bytesRead);
      }
      return buffer.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(
          ClientError.CLASS_FILE_LOAD_FAILED.buildMessage(clazz.getName()), e);
    }
  }
}
