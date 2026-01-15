package com.scalar.dl.ledger.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;

@Immutable
public class FunctionRegistrationRequest {
  private final String functionId;
  private final String functionBinaryName;
  private final byte[] functionByteCode;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public FunctionRegistrationRequest(
      String functionId, String functionBinaryName, byte[] functionByteCode) {
    this.functionId = functionId;
    this.functionBinaryName = functionBinaryName;
    this.functionByteCode = functionByteCode;
  }

  public String getFunctionId() {
    return functionId;
  }

  public String getFunctionBinaryName() {
    return functionBinaryName;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public byte[] getFunctionByteCode() {
    return functionByteCode;
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return Objects.hash(functionId, functionBinaryName, Arrays.hashCode(functionByteCode));
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof FunctionRegistrationRequest)) {
      return false;
    }
    FunctionRegistrationRequest other = (FunctionRegistrationRequest) o;
    return (this.functionId.equals(other.functionId)
        && this.functionBinaryName.equals(other.functionBinaryName)
        && Arrays.equals(this.functionByteCode, other.functionByteCode));
  }
}
