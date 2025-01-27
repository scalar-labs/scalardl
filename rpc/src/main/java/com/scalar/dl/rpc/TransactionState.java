// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: scalar.proto

// Protobuf Java Version: 3.25.5
package com.scalar.dl.rpc;

/**
 * Protobuf enum {@code rpc.TransactionState}
 */
public enum TransactionState
    implements com.google.protobuf.ProtocolMessageEnum {
  /**
   * <code>TRANSACTION_STATE_UNSPECIFIED = 0;</code>
   */
  TRANSACTION_STATE_UNSPECIFIED(0),
  /**
   * <code>TRANSACTION_STATE_COMMITTED = 1;</code>
   */
  TRANSACTION_STATE_COMMITTED(1),
  /**
   * <code>TRANSACTION_STATE_ABORTED = 2;</code>
   */
  TRANSACTION_STATE_ABORTED(2),
  /**
   * <code>TRANSACTION_STATE_UNKNOWN = 3;</code>
   */
  TRANSACTION_STATE_UNKNOWN(3),
  UNRECOGNIZED(-1),
  ;

  /**
   * <code>TRANSACTION_STATE_UNSPECIFIED = 0;</code>
   */
  public static final int TRANSACTION_STATE_UNSPECIFIED_VALUE = 0;
  /**
   * <code>TRANSACTION_STATE_COMMITTED = 1;</code>
   */
  public static final int TRANSACTION_STATE_COMMITTED_VALUE = 1;
  /**
   * <code>TRANSACTION_STATE_ABORTED = 2;</code>
   */
  public static final int TRANSACTION_STATE_ABORTED_VALUE = 2;
  /**
   * <code>TRANSACTION_STATE_UNKNOWN = 3;</code>
   */
  public static final int TRANSACTION_STATE_UNKNOWN_VALUE = 3;


  public final int getNumber() {
    if (this == UNRECOGNIZED) {
      throw new java.lang.IllegalArgumentException(
          "Can't get the number of an unknown enum value.");
    }
    return value;
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   * @deprecated Use {@link #forNumber(int)} instead.
   */
  @java.lang.Deprecated
  public static TransactionState valueOf(int value) {
    return forNumber(value);
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   */
  public static TransactionState forNumber(int value) {
    switch (value) {
      case 0: return TRANSACTION_STATE_UNSPECIFIED;
      case 1: return TRANSACTION_STATE_COMMITTED;
      case 2: return TRANSACTION_STATE_ABORTED;
      case 3: return TRANSACTION_STATE_UNKNOWN;
      default: return null;
    }
  }

  public static com.google.protobuf.Internal.EnumLiteMap<TransactionState>
      internalGetValueMap() {
    return internalValueMap;
  }
  private static final com.google.protobuf.Internal.EnumLiteMap<
      TransactionState> internalValueMap =
        new com.google.protobuf.Internal.EnumLiteMap<TransactionState>() {
          public TransactionState findValueByNumber(int number) {
            return TransactionState.forNumber(number);
          }
        };

  public final com.google.protobuf.Descriptors.EnumValueDescriptor
      getValueDescriptor() {
    if (this == UNRECOGNIZED) {
      throw new java.lang.IllegalStateException(
          "Can't get the descriptor of an unrecognized enum value.");
    }
    return getDescriptor().getValues().get(ordinal());
  }
  public final com.google.protobuf.Descriptors.EnumDescriptor
      getDescriptorForType() {
    return getDescriptor();
  }
  public static final com.google.protobuf.Descriptors.EnumDescriptor
      getDescriptor() {
    return com.scalar.dl.rpc.ScalarProto.getDescriptor().getEnumTypes().get(0);
  }

  private static final TransactionState[] VALUES = values();

  public static TransactionState valueOf(
      com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
    if (desc.getType() != getDescriptor()) {
      throw new java.lang.IllegalArgumentException(
        "EnumValueDescriptor is not for this type.");
    }
    if (desc.getIndex() == -1) {
      return UNRECOGNIZED;
    }
    return VALUES[desc.getIndex()];
  }

  private final int value;

  private TransactionState(int value) {
    this.value = value;
  }

  // @@protoc_insertion_point(enum_scope:rpc.TransactionState)
}

