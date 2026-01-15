package com.scalar.dl.ledger.function;

import static com.google.common.base.Preconditions.checkNotNull;

import com.scalar.dl.ledger.model.FunctionRegistrationRequest;
import com.scalar.dl.ledger.util.Time;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;

@Immutable
public class FunctionEntry {
  public static final String ID = "id";
  public static final String BINARY_NAME = "binary_name";
  public static final String BYTE_CODE = "byte_code";
  public static final String REGISTERED_AT = "registered_at";
  private final String id;
  private final String binaryName;
  private final byte[] byteCode;
  private final long registeredAt;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public FunctionEntry(String id, String binaryName, byte[] byteCode, long registeredAt) {
    this.id = checkNotNull(id);
    this.binaryName = checkNotNull(binaryName);
    this.byteCode = checkNotNull(byteCode);
    this.registeredAt = registeredAt;
  }

  /**
   * Returns the id of the {@code FunctionEntry}.
   *
   * @return the id of the {@code FunctionEntry}
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the binary name of the {@code FunctionEntry}.
   *
   * @return the binary name of the {@code FunctionEntry}
   */
  public String getBinaryName() {
    return binaryName;
  }

  /**
   * Returns the bytecode of the function contained in the {@code FunctionEntry}.
   *
   * @return the bytecode of the function contained in the {@code FunctionEntry}
   */
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public byte[] getByteCode() {
    return byteCode;
  }

  /**
   * Returns the registered at time of the {@code FunctionEntry}.
   *
   * @return the registered at time of the {@code FunctionEntry}
   */
  public long getRegisteredAt() {
    return registeredAt;
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return Objects.hash(id, binaryName, Arrays.hashCode(byteCode), registeredAt);
  }

  /**
   * Indicates whether some other object is "equal to" this object. The other object is considered
   * equal if it is also an {@code FunctionEntry} and both instances have the same
   *
   * <ul>
   *   <li>id
   *   <li>binary name
   *   <li>function bytecode
   *   <li>registered at time
   * </ul>
   *
   * @param o an object to be tested for equality
   * @return {@code true} if the other object is "equal to" this object otherwise {@code false}
   */
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof FunctionEntry)) {
      return false;
    }
    FunctionEntry another = (FunctionEntry) o;
    return this.id.equals(another.id)
        && this.binaryName.equals(another.binaryName)
        && Arrays.equals(this.byteCode, another.byteCode)
        && this.registeredAt == another.registeredAt;
  }

  public static FunctionEntry from(FunctionRegistrationRequest request) {
    return new FunctionEntry(
        request.getFunctionId(),
        request.getFunctionBinaryName(),
        request.getFunctionByteCode(),
        Time.getCurrentUtcTimeInMillis());
  }
}
