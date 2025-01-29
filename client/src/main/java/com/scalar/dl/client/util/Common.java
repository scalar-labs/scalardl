package com.scalar.dl.client.util;

import com.scalar.dl.client.error.ClientError;
import com.scalar.dl.client.exception.ClientException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class Common {

  public static byte[] fileToBytes(String filePath) {
    try {
      return Files.readAllBytes(new File(filePath).toPath());
    } catch (IOException e) {
      throw new ClientException(ClientError.READING_FILE_FAILED, e, filePath, e.getMessage());
    }
  }
}
