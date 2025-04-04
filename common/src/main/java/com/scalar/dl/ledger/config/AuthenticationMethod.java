package com.scalar.dl.ledger.config;

import com.scalar.dl.ledger.error.CommonError;
import java.util.Arrays;

public enum AuthenticationMethod {
  DIGITAL_SIGNATURE("digital-signature"),
  HMAC("hmac"),
  PASS_THROUGH("pass-through");

  private final String method;

  AuthenticationMethod(String method) {
    this.method = method;
  }

  public String getMethod() {
    return method;
  }

  public static AuthenticationMethod get(String method) {
    return Arrays.stream(AuthenticationMethod.values())
        .filter(v -> v.method.equals(method))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    CommonError.INVALID_AUTHENTICATION_METHOD.buildMessage(method)));
  }
}
