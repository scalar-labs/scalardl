package com.scalar.dl.ledger.exception;

import com.google.common.collect.ImmutableMap;
import com.scalar.dl.ledger.error.ScalarDlError;
import com.scalar.dl.ledger.service.StatusCode;
import java.util.Map;

public class ConflictException extends DatabaseException {
  private ImmutableMap<String, Integer> ids = ImmutableMap.of();

  public ConflictException(String message) {
    super(message, StatusCode.CONFLICT);
  }

  public ConflictException(String message, Map<String, Integer> ids) {
    super(message, StatusCode.CONFLICT);
    this.ids = ImmutableMap.copyOf(ids);
  }

  public ConflictException(String message, Throwable cause) {
    super(message, cause, StatusCode.CONFLICT);
  }

  public ConflictException(String message, Throwable cause, Map<String, Integer> ids) {
    super(message, cause, StatusCode.CONFLICT);
    this.ids = ImmutableMap.copyOf(ids);
  }

  public ConflictException(ScalarDlError error, Map<String, Integer> ids) {
    super(error.buildMessage(), error.getStatusCode());
    this.ids = ImmutableMap.copyOf(ids);
  }

  public ConflictException(
      ScalarDlError error, Throwable cause, Map<String, Integer> ids, Object... args) {
    super(error.buildMessage(args), cause, error.getStatusCode());
    this.ids = ImmutableMap.copyOf(ids);
  }

  public ConflictException(ScalarDlError error, Object... args) {
    super(error.buildMessage(args), error.getStatusCode());
  }

  public ConflictException(ScalarDlError error, Throwable cause, Object... args) {
    super(error.buildMessage(args), cause, error.getStatusCode());
  }

  public Map<String, Integer> getIds() {
    return ids;
  }
}
