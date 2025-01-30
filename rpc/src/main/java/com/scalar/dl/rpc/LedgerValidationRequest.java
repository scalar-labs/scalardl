// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: scalar.proto

// Protobuf Java Version: 3.25.5
package com.scalar.dl.rpc;

/**
 * Protobuf type {@code rpc.LedgerValidationRequest}
 */
public final class LedgerValidationRequest extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:rpc.LedgerValidationRequest)
    LedgerValidationRequestOrBuilder {
private static final long serialVersionUID = 0L;
  // Use LedgerValidationRequest.newBuilder() to construct.
  private LedgerValidationRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private LedgerValidationRequest() {
    assetId_ = "";
    entityId_ = "";
    signature_ = com.google.protobuf.ByteString.EMPTY;
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new LedgerValidationRequest();
  }

  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return com.scalar.dl.rpc.ScalarProto.internal_static_rpc_LedgerValidationRequest_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.scalar.dl.rpc.ScalarProto.internal_static_rpc_LedgerValidationRequest_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.scalar.dl.rpc.LedgerValidationRequest.class, com.scalar.dl.rpc.LedgerValidationRequest.Builder.class);
  }

  public static final int ASSET_ID_FIELD_NUMBER = 1;
  @SuppressWarnings("serial")
  private volatile java.lang.Object assetId_ = "";
  /**
   * <code>string asset_id = 1;</code>
   * @return The assetId.
   */
  @java.lang.Override
  public java.lang.String getAssetId() {
    java.lang.Object ref = assetId_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      assetId_ = s;
      return s;
    }
  }
  /**
   * <code>string asset_id = 1;</code>
   * @return The bytes for assetId.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getAssetIdBytes() {
    java.lang.Object ref = assetId_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      assetId_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int START_AGE_FIELD_NUMBER = 2;
  private int startAge_ = 0;
  /**
   * <code>uint32 start_age = 2;</code>
   * @return The startAge.
   */
  @java.lang.Override
  public int getStartAge() {
    return startAge_;
  }

  public static final int END_AGE_FIELD_NUMBER = 3;
  private int endAge_ = 0;
  /**
   * <code>uint32 end_age = 3;</code>
   * @return The endAge.
   */
  @java.lang.Override
  public int getEndAge() {
    return endAge_;
  }

  public static final int ENTITY_ID_FIELD_NUMBER = 4;
  @SuppressWarnings("serial")
  private volatile java.lang.Object entityId_ = "";
  /**
   * <code>string entity_id = 4;</code>
   * @return The entityId.
   */
  @java.lang.Override
  public java.lang.String getEntityId() {
    java.lang.Object ref = entityId_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      entityId_ = s;
      return s;
    }
  }
  /**
   * <code>string entity_id = 4;</code>
   * @return The bytes for entityId.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getEntityIdBytes() {
    java.lang.Object ref = entityId_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      entityId_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int KEY_VERSION_FIELD_NUMBER = 5;
  private int keyVersion_ = 0;
  /**
   * <code>uint32 key_version = 5;</code>
   * @return The keyVersion.
   */
  @java.lang.Override
  public int getKeyVersion() {
    return keyVersion_;
  }

  public static final int SIGNATURE_FIELD_NUMBER = 6;
  private com.google.protobuf.ByteString signature_ = com.google.protobuf.ByteString.EMPTY;
  /**
   * <code>bytes signature = 6;</code>
   * @return The signature.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString getSignature() {
    return signature_;
  }

  private byte memoizedIsInitialized = -1;
  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(assetId_)) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, assetId_);
    }
    if (startAge_ != 0) {
      output.writeUInt32(2, startAge_);
    }
    if (endAge_ != 0) {
      output.writeUInt32(3, endAge_);
    }
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(entityId_)) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 4, entityId_);
    }
    if (keyVersion_ != 0) {
      output.writeUInt32(5, keyVersion_);
    }
    if (!signature_.isEmpty()) {
      output.writeBytes(6, signature_);
    }
    getUnknownFields().writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(assetId_)) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, assetId_);
    }
    if (startAge_ != 0) {
      size += com.google.protobuf.CodedOutputStream
        .computeUInt32Size(2, startAge_);
    }
    if (endAge_ != 0) {
      size += com.google.protobuf.CodedOutputStream
        .computeUInt32Size(3, endAge_);
    }
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(entityId_)) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(4, entityId_);
    }
    if (keyVersion_ != 0) {
      size += com.google.protobuf.CodedOutputStream
        .computeUInt32Size(5, keyVersion_);
    }
    if (!signature_.isEmpty()) {
      size += com.google.protobuf.CodedOutputStream
        .computeBytesSize(6, signature_);
    }
    size += getUnknownFields().getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof com.scalar.dl.rpc.LedgerValidationRequest)) {
      return super.equals(obj);
    }
    com.scalar.dl.rpc.LedgerValidationRequest other = (com.scalar.dl.rpc.LedgerValidationRequest) obj;

    if (!getAssetId()
        .equals(other.getAssetId())) return false;
    if (getStartAge()
        != other.getStartAge()) return false;
    if (getEndAge()
        != other.getEndAge()) return false;
    if (!getEntityId()
        .equals(other.getEntityId())) return false;
    if (getKeyVersion()
        != other.getKeyVersion()) return false;
    if (!getSignature()
        .equals(other.getSignature())) return false;
    if (!getUnknownFields().equals(other.getUnknownFields())) return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    hash = (37 * hash) + ASSET_ID_FIELD_NUMBER;
    hash = (53 * hash) + getAssetId().hashCode();
    hash = (37 * hash) + START_AGE_FIELD_NUMBER;
    hash = (53 * hash) + getStartAge();
    hash = (37 * hash) + END_AGE_FIELD_NUMBER;
    hash = (53 * hash) + getEndAge();
    hash = (37 * hash) + ENTITY_ID_FIELD_NUMBER;
    hash = (53 * hash) + getEntityId().hashCode();
    hash = (37 * hash) + KEY_VERSION_FIELD_NUMBER;
    hash = (53 * hash) + getKeyVersion();
    hash = (37 * hash) + SIGNATURE_FIELD_NUMBER;
    hash = (53 * hash) + getSignature().hashCode();
    hash = (29 * hash) + getUnknownFields().hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.scalar.dl.rpc.LedgerValidationRequest parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.scalar.dl.rpc.LedgerValidationRequest parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.scalar.dl.rpc.LedgerValidationRequest parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.scalar.dl.rpc.LedgerValidationRequest parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.scalar.dl.rpc.LedgerValidationRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.scalar.dl.rpc.LedgerValidationRequest parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.scalar.dl.rpc.LedgerValidationRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.scalar.dl.rpc.LedgerValidationRequest parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  public static com.scalar.dl.rpc.LedgerValidationRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }

  public static com.scalar.dl.rpc.LedgerValidationRequest parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.scalar.dl.rpc.LedgerValidationRequest parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.scalar.dl.rpc.LedgerValidationRequest parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(com.scalar.dl.rpc.LedgerValidationRequest prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code rpc.LedgerValidationRequest}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:rpc.LedgerValidationRequest)
      com.scalar.dl.rpc.LedgerValidationRequestOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.scalar.dl.rpc.ScalarProto.internal_static_rpc_LedgerValidationRequest_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.scalar.dl.rpc.ScalarProto.internal_static_rpc_LedgerValidationRequest_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.scalar.dl.rpc.LedgerValidationRequest.class, com.scalar.dl.rpc.LedgerValidationRequest.Builder.class);
    }

    // Construct using com.scalar.dl.rpc.LedgerValidationRequest.newBuilder()
    private Builder() {

    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);

    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      bitField0_ = 0;
      assetId_ = "";
      startAge_ = 0;
      endAge_ = 0;
      entityId_ = "";
      keyVersion_ = 0;
      signature_ = com.google.protobuf.ByteString.EMPTY;
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return com.scalar.dl.rpc.ScalarProto.internal_static_rpc_LedgerValidationRequest_descriptor;
    }

    @java.lang.Override
    public com.scalar.dl.rpc.LedgerValidationRequest getDefaultInstanceForType() {
      return com.scalar.dl.rpc.LedgerValidationRequest.getDefaultInstance();
    }

    @java.lang.Override
    public com.scalar.dl.rpc.LedgerValidationRequest build() {
      com.scalar.dl.rpc.LedgerValidationRequest result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public com.scalar.dl.rpc.LedgerValidationRequest buildPartial() {
      com.scalar.dl.rpc.LedgerValidationRequest result = new com.scalar.dl.rpc.LedgerValidationRequest(this);
      if (bitField0_ != 0) { buildPartial0(result); }
      onBuilt();
      return result;
    }

    private void buildPartial0(com.scalar.dl.rpc.LedgerValidationRequest result) {
      int from_bitField0_ = bitField0_;
      if (((from_bitField0_ & 0x00000001) != 0)) {
        result.assetId_ = assetId_;
      }
      if (((from_bitField0_ & 0x00000002) != 0)) {
        result.startAge_ = startAge_;
      }
      if (((from_bitField0_ & 0x00000004) != 0)) {
        result.endAge_ = endAge_;
      }
      if (((from_bitField0_ & 0x00000008) != 0)) {
        result.entityId_ = entityId_;
      }
      if (((from_bitField0_ & 0x00000010) != 0)) {
        result.keyVersion_ = keyVersion_;
      }
      if (((from_bitField0_ & 0x00000020) != 0)) {
        result.signature_ = signature_;
      }
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }
    @java.lang.Override
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.setField(field, value);
    }
    @java.lang.Override
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @java.lang.Override
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @java.lang.Override
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }
    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof com.scalar.dl.rpc.LedgerValidationRequest) {
        return mergeFrom((com.scalar.dl.rpc.LedgerValidationRequest)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.scalar.dl.rpc.LedgerValidationRequest other) {
      if (other == com.scalar.dl.rpc.LedgerValidationRequest.getDefaultInstance()) return this;
      if (!other.getAssetId().isEmpty()) {
        assetId_ = other.assetId_;
        bitField0_ |= 0x00000001;
        onChanged();
      }
      if (other.getStartAge() != 0) {
        setStartAge(other.getStartAge());
      }
      if (other.getEndAge() != 0) {
        setEndAge(other.getEndAge());
      }
      if (!other.getEntityId().isEmpty()) {
        entityId_ = other.entityId_;
        bitField0_ |= 0x00000008;
        onChanged();
      }
      if (other.getKeyVersion() != 0) {
        setKeyVersion(other.getKeyVersion());
      }
      if (other.getSignature() != com.google.protobuf.ByteString.EMPTY) {
        setSignature(other.getSignature());
      }
      this.mergeUnknownFields(other.getUnknownFields());
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      if (extensionRegistry == null) {
        throw new java.lang.NullPointerException();
      }
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            case 10: {
              assetId_ = input.readStringRequireUtf8();
              bitField0_ |= 0x00000001;
              break;
            } // case 10
            case 16: {
              startAge_ = input.readUInt32();
              bitField0_ |= 0x00000002;
              break;
            } // case 16
            case 24: {
              endAge_ = input.readUInt32();
              bitField0_ |= 0x00000004;
              break;
            } // case 24
            case 34: {
              entityId_ = input.readStringRequireUtf8();
              bitField0_ |= 0x00000008;
              break;
            } // case 34
            case 40: {
              keyVersion_ = input.readUInt32();
              bitField0_ |= 0x00000010;
              break;
            } // case 40
            case 50: {
              signature_ = input.readBytes();
              bitField0_ |= 0x00000020;
              break;
            } // case 50
            default: {
              if (!super.parseUnknownField(input, extensionRegistry, tag)) {
                done = true; // was an endgroup tag
              }
              break;
            } // default:
          } // switch (tag)
        } // while (!done)
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.unwrapIOException();
      } finally {
        onChanged();
      } // finally
      return this;
    }
    private int bitField0_;

    private java.lang.Object assetId_ = "";
    /**
     * <code>string asset_id = 1;</code>
     * @return The assetId.
     */
    public java.lang.String getAssetId() {
      java.lang.Object ref = assetId_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        assetId_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string asset_id = 1;</code>
     * @return The bytes for assetId.
     */
    public com.google.protobuf.ByteString
        getAssetIdBytes() {
      java.lang.Object ref = assetId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        assetId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string asset_id = 1;</code>
     * @param value The assetId to set.
     * @return This builder for chaining.
     */
    public Builder setAssetId(
        java.lang.String value) {
      if (value == null) { throw new NullPointerException(); }
      assetId_ = value;
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }
    /**
     * <code>string asset_id = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearAssetId() {
      assetId_ = getDefaultInstance().getAssetId();
      bitField0_ = (bitField0_ & ~0x00000001);
      onChanged();
      return this;
    }
    /**
     * <code>string asset_id = 1;</code>
     * @param value The bytes for assetId to set.
     * @return This builder for chaining.
     */
    public Builder setAssetIdBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) { throw new NullPointerException(); }
      checkByteStringIsUtf8(value);
      assetId_ = value;
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }

    private int startAge_ ;
    /**
     * <code>uint32 start_age = 2;</code>
     * @return The startAge.
     */
    @java.lang.Override
    public int getStartAge() {
      return startAge_;
    }
    /**
     * <code>uint32 start_age = 2;</code>
     * @param value The startAge to set.
     * @return This builder for chaining.
     */
    public Builder setStartAge(int value) {

      startAge_ = value;
      bitField0_ |= 0x00000002;
      onChanged();
      return this;
    }
    /**
     * <code>uint32 start_age = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearStartAge() {
      bitField0_ = (bitField0_ & ~0x00000002);
      startAge_ = 0;
      onChanged();
      return this;
    }

    private int endAge_ ;
    /**
     * <code>uint32 end_age = 3;</code>
     * @return The endAge.
     */
    @java.lang.Override
    public int getEndAge() {
      return endAge_;
    }
    /**
     * <code>uint32 end_age = 3;</code>
     * @param value The endAge to set.
     * @return This builder for chaining.
     */
    public Builder setEndAge(int value) {

      endAge_ = value;
      bitField0_ |= 0x00000004;
      onChanged();
      return this;
    }
    /**
     * <code>uint32 end_age = 3;</code>
     * @return This builder for chaining.
     */
    public Builder clearEndAge() {
      bitField0_ = (bitField0_ & ~0x00000004);
      endAge_ = 0;
      onChanged();
      return this;
    }

    private java.lang.Object entityId_ = "";
    /**
     * <code>string entity_id = 4;</code>
     * @return The entityId.
     */
    public java.lang.String getEntityId() {
      java.lang.Object ref = entityId_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        entityId_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string entity_id = 4;</code>
     * @return The bytes for entityId.
     */
    public com.google.protobuf.ByteString
        getEntityIdBytes() {
      java.lang.Object ref = entityId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        entityId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string entity_id = 4;</code>
     * @param value The entityId to set.
     * @return This builder for chaining.
     */
    public Builder setEntityId(
        java.lang.String value) {
      if (value == null) { throw new NullPointerException(); }
      entityId_ = value;
      bitField0_ |= 0x00000008;
      onChanged();
      return this;
    }
    /**
     * <code>string entity_id = 4;</code>
     * @return This builder for chaining.
     */
    public Builder clearEntityId() {
      entityId_ = getDefaultInstance().getEntityId();
      bitField0_ = (bitField0_ & ~0x00000008);
      onChanged();
      return this;
    }
    /**
     * <code>string entity_id = 4;</code>
     * @param value The bytes for entityId to set.
     * @return This builder for chaining.
     */
    public Builder setEntityIdBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) { throw new NullPointerException(); }
      checkByteStringIsUtf8(value);
      entityId_ = value;
      bitField0_ |= 0x00000008;
      onChanged();
      return this;
    }

    private int keyVersion_ ;
    /**
     * <code>uint32 key_version = 5;</code>
     * @return The keyVersion.
     */
    @java.lang.Override
    public int getKeyVersion() {
      return keyVersion_;
    }
    /**
     * <code>uint32 key_version = 5;</code>
     * @param value The keyVersion to set.
     * @return This builder for chaining.
     */
    public Builder setKeyVersion(int value) {

      keyVersion_ = value;
      bitField0_ |= 0x00000010;
      onChanged();
      return this;
    }
    /**
     * <code>uint32 key_version = 5;</code>
     * @return This builder for chaining.
     */
    public Builder clearKeyVersion() {
      bitField0_ = (bitField0_ & ~0x00000010);
      keyVersion_ = 0;
      onChanged();
      return this;
    }

    private com.google.protobuf.ByteString signature_ = com.google.protobuf.ByteString.EMPTY;
    /**
     * <code>bytes signature = 6;</code>
     * @return The signature.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString getSignature() {
      return signature_;
    }
    /**
     * <code>bytes signature = 6;</code>
     * @param value The signature to set.
     * @return This builder for chaining.
     */
    public Builder setSignature(com.google.protobuf.ByteString value) {
      if (value == null) { throw new NullPointerException(); }
      signature_ = value;
      bitField0_ |= 0x00000020;
      onChanged();
      return this;
    }
    /**
     * <code>bytes signature = 6;</code>
     * @return This builder for chaining.
     */
    public Builder clearSignature() {
      bitField0_ = (bitField0_ & ~0x00000020);
      signature_ = getDefaultInstance().getSignature();
      onChanged();
      return this;
    }
    @java.lang.Override
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:rpc.LedgerValidationRequest)
  }

  // @@protoc_insertion_point(class_scope:rpc.LedgerValidationRequest)
  private static final com.scalar.dl.rpc.LedgerValidationRequest DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new com.scalar.dl.rpc.LedgerValidationRequest();
  }

  public static com.scalar.dl.rpc.LedgerValidationRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<LedgerValidationRequest>
      PARSER = new com.google.protobuf.AbstractParser<LedgerValidationRequest>() {
    @java.lang.Override
    public LedgerValidationRequest parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      Builder builder = newBuilder();
      try {
        builder.mergeFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(builder.buildPartial());
      } catch (com.google.protobuf.UninitializedMessageException e) {
        throw e.asInvalidProtocolBufferException().setUnfinishedMessage(builder.buildPartial());
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e)
            .setUnfinishedMessage(builder.buildPartial());
      }
      return builder.buildPartial();
    }
  };

  public static com.google.protobuf.Parser<LedgerValidationRequest> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<LedgerValidationRequest> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public com.scalar.dl.rpc.LedgerValidationRequest getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

