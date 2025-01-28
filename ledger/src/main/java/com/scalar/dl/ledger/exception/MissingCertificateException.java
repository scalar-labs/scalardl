package com.scalar.dl.ledger.exception;

import com.scalar.dl.ledger.error.ScalarDlError;
import com.scalar.dl.ledger.service.StatusCode;

public class MissingCertificateException extends DatabaseException {

  public MissingCertificateException(String message) {
    super(message, StatusCode.CERTIFICATE_NOT_FOUND);
  }

  public MissingCertificateException(String message, Throwable cause) {
    super(message, cause, StatusCode.CERTIFICATE_NOT_FOUND);
  }

  public MissingCertificateException(ScalarDlError error, Object... args) {
    super(error.buildMessage(args), error.getStatusCode());
  }
}
