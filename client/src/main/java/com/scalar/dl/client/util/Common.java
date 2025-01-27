package com.scalar.dl.client.util;

import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.ledger.service.StatusCode;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class Common {

  public static byte[] fileToBytes(String filePath) {
    try {
      return Files.readAllBytes(new File(filePath).toPath());
    } catch (IOException e) {
      throw new ClientException("can't read " + filePath, e, StatusCode.RUNTIME_ERROR);
    }
  }
}
