// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: scalar.proto

// Protobuf Java Version: 3.25.5
package com.scalar.dl.rpc;

/**
 * Protobuf type {@code rpc.LedgerValidationResponse}
 */
public final class LedgerValidationResponse extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:rpc.LedgerValidationResponse)
    LedgerValidationResponseOrBuilder {
private static final long serialVersionUID = 0L;
  // Use LedgerValidationResponse.newBuilder() to construct.
  private LedgerValidationResponse(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private LedgerValidationResponse() {
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new LedgerValidationResponse();
  }

  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return com.scalar.dl.rpc.ScalarProto.internal_static_rpc_LedgerValidationResponse_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.scalar.dl.rpc.ScalarProto.internal_static_rpc_LedgerValidationResponse_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.scalar.dl.rpc.LedgerValidationResponse.class, com.scalar.dl.rpc.LedgerValidationResponse.Builder.class);
  }

  private int bitField0_;
  public static final int STATUS_CODE_FIELD_NUMBER = 1;
  private int statusCode_ = 0;
  /**
   * <code>uint32 status_code = 1;</code>
   * @return The statusCode.
   */
  @java.lang.Override
  public int getStatusCode() {
    return statusCode_;
  }

  public static final int PROOF_FIELD_NUMBER = 2;
  private com.scalar.dl.rpc.AssetProof proof_;
  /**
   * <pre>
   * a proof given from the ledger server
   * </pre>
   *
   * <code>.rpc.AssetProof proof = 2;</code>
   * @return Whether the proof field is set.
   */
  @java.lang.Override
  public boolean hasProof() {
    return ((bitField0_ & 0x00000001) != 0);
  }
  /**
   * <pre>
   * a proof given from the ledger server
   * </pre>
   *
   * <code>.rpc.AssetProof proof = 2;</code>
   * @return The proof.
   */
  @java.lang.Override
  public com.scalar.dl.rpc.AssetProof getProof() {
    return proof_ == null ? com.scalar.dl.rpc.AssetProof.getDefaultInstance() : proof_;
  }
  /**
   * <pre>
   * a proof given from the ledger server
   * </pre>
   *
   * <code>.rpc.AssetProof proof = 2;</code>
   */
  @java.lang.Override
  public com.scalar.dl.rpc.AssetProofOrBuilder getProofOrBuilder() {
    return proof_ == null ? com.scalar.dl.rpc.AssetProof.getDefaultInstance() : proof_;
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
    if (statusCode_ != 0) {
      output.writeUInt32(1, statusCode_);
    }
    if (((bitField0_ & 0x00000001) != 0)) {
      output.writeMessage(2, getProof());
    }
    getUnknownFields().writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (statusCode_ != 0) {
      size += com.google.protobuf.CodedOutputStream
        .computeUInt32Size(1, statusCode_);
    }
    if (((bitField0_ & 0x00000001) != 0)) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(2, getProof());
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
    if (!(obj instanceof com.scalar.dl.rpc.LedgerValidationResponse)) {
      return super.equals(obj);
    }
    com.scalar.dl.rpc.LedgerValidationResponse other = (com.scalar.dl.rpc.LedgerValidationResponse) obj;

    if (getStatusCode()
        != other.getStatusCode()) return false;
    if (hasProof() != other.hasProof()) return false;
    if (hasProof()) {
      if (!getProof()
          .equals(other.getProof())) return false;
    }
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
    hash = (37 * hash) + STATUS_CODE_FIELD_NUMBER;
    hash = (53 * hash) + getStatusCode();
    if (hasProof()) {
      hash = (37 * hash) + PROOF_FIELD_NUMBER;
      hash = (53 * hash) + getProof().hashCode();
    }
    hash = (29 * hash) + getUnknownFields().hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.scalar.dl.rpc.LedgerValidationResponse parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.scalar.dl.rpc.LedgerValidationResponse parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.scalar.dl.rpc.LedgerValidationResponse parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.scalar.dl.rpc.LedgerValidationResponse parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.scalar.dl.rpc.LedgerValidationResponse parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.scalar.dl.rpc.LedgerValidationResponse parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.scalar.dl.rpc.LedgerValidationResponse parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.scalar.dl.rpc.LedgerValidationResponse parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  public static com.scalar.dl.rpc.LedgerValidationResponse parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }

  public static com.scalar.dl.rpc.LedgerValidationResponse parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.scalar.dl.rpc.LedgerValidationResponse parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.scalar.dl.rpc.LedgerValidationResponse parseFrom(
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
  public static Builder newBuilder(com.scalar.dl.rpc.LedgerValidationResponse prototype) {
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
   * Protobuf type {@code rpc.LedgerValidationResponse}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:rpc.LedgerValidationResponse)
      com.scalar.dl.rpc.LedgerValidationResponseOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.scalar.dl.rpc.ScalarProto.internal_static_rpc_LedgerValidationResponse_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.scalar.dl.rpc.ScalarProto.internal_static_rpc_LedgerValidationResponse_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.scalar.dl.rpc.LedgerValidationResponse.class, com.scalar.dl.rpc.LedgerValidationResponse.Builder.class);
    }

    // Construct using com.scalar.dl.rpc.LedgerValidationResponse.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3
              .alwaysUseFieldBuilders) {
        getProofFieldBuilder();
      }
    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      bitField0_ = 0;
      statusCode_ = 0;
      proof_ = null;
      if (proofBuilder_ != null) {
        proofBuilder_.dispose();
        proofBuilder_ = null;
      }
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return com.scalar.dl.rpc.ScalarProto.internal_static_rpc_LedgerValidationResponse_descriptor;
    }

    @java.lang.Override
    public com.scalar.dl.rpc.LedgerValidationResponse getDefaultInstanceForType() {
      return com.scalar.dl.rpc.LedgerValidationResponse.getDefaultInstance();
    }

    @java.lang.Override
    public com.scalar.dl.rpc.LedgerValidationResponse build() {
      com.scalar.dl.rpc.LedgerValidationResponse result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public com.scalar.dl.rpc.LedgerValidationResponse buildPartial() {
      com.scalar.dl.rpc.LedgerValidationResponse result = new com.scalar.dl.rpc.LedgerValidationResponse(this);
      if (bitField0_ != 0) { buildPartial0(result); }
      onBuilt();
      return result;
    }

    private void buildPartial0(com.scalar.dl.rpc.LedgerValidationResponse result) {
      int from_bitField0_ = bitField0_;
      if (((from_bitField0_ & 0x00000001) != 0)) {
        result.statusCode_ = statusCode_;
      }
      int to_bitField0_ = 0;
      if (((from_bitField0_ & 0x00000002) != 0)) {
        result.proof_ = proofBuilder_ == null
            ? proof_
            : proofBuilder_.build();
        to_bitField0_ |= 0x00000001;
      }
      result.bitField0_ |= to_bitField0_;
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
      if (other instanceof com.scalar.dl.rpc.LedgerValidationResponse) {
        return mergeFrom((com.scalar.dl.rpc.LedgerValidationResponse)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.scalar.dl.rpc.LedgerValidationResponse other) {
      if (other == com.scalar.dl.rpc.LedgerValidationResponse.getDefaultInstance()) return this;
      if (other.getStatusCode() != 0) {
        setStatusCode(other.getStatusCode());
      }
      if (other.hasProof()) {
        mergeProof(other.getProof());
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
            case 8: {
              statusCode_ = input.readUInt32();
              bitField0_ |= 0x00000001;
              break;
            } // case 8
            case 18: {
              input.readMessage(
                  getProofFieldBuilder().getBuilder(),
                  extensionRegistry);
              bitField0_ |= 0x00000002;
              break;
            } // case 18
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

    private int statusCode_ ;
    /**
     * <code>uint32 status_code = 1;</code>
     * @return The statusCode.
     */
    @java.lang.Override
    public int getStatusCode() {
      return statusCode_;
    }
    /**
     * <code>uint32 status_code = 1;</code>
     * @param value The statusCode to set.
     * @return This builder for chaining.
     */
    public Builder setStatusCode(int value) {

      statusCode_ = value;
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }
    /**
     * <code>uint32 status_code = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearStatusCode() {
      bitField0_ = (bitField0_ & ~0x00000001);
      statusCode_ = 0;
      onChanged();
      return this;
    }

    private com.scalar.dl.rpc.AssetProof proof_;
    private com.google.protobuf.SingleFieldBuilderV3<
        com.scalar.dl.rpc.AssetProof, com.scalar.dl.rpc.AssetProof.Builder, com.scalar.dl.rpc.AssetProofOrBuilder> proofBuilder_;
    /**
     * <pre>
     * a proof given from the ledger server
     * </pre>
     *
     * <code>.rpc.AssetProof proof = 2;</code>
     * @return Whether the proof field is set.
     */
    public boolean hasProof() {
      return ((bitField0_ & 0x00000002) != 0);
    }
    /**
     * <pre>
     * a proof given from the ledger server
     * </pre>
     *
     * <code>.rpc.AssetProof proof = 2;</code>
     * @return The proof.
     */
    public com.scalar.dl.rpc.AssetProof getProof() {
      if (proofBuilder_ == null) {
        return proof_ == null ? com.scalar.dl.rpc.AssetProof.getDefaultInstance() : proof_;
      } else {
        return proofBuilder_.getMessage();
      }
    }
    /**
     * <pre>
     * a proof given from the ledger server
     * </pre>
     *
     * <code>.rpc.AssetProof proof = 2;</code>
     */
    public Builder setProof(com.scalar.dl.rpc.AssetProof value) {
      if (proofBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        proof_ = value;
      } else {
        proofBuilder_.setMessage(value);
      }
      bitField0_ |= 0x00000002;
      onChanged();
      return this;
    }
    /**
     * <pre>
     * a proof given from the ledger server
     * </pre>
     *
     * <code>.rpc.AssetProof proof = 2;</code>
     */
    public Builder setProof(
        com.scalar.dl.rpc.AssetProof.Builder builderForValue) {
      if (proofBuilder_ == null) {
        proof_ = builderForValue.build();
      } else {
        proofBuilder_.setMessage(builderForValue.build());
      }
      bitField0_ |= 0x00000002;
      onChanged();
      return this;
    }
    /**
     * <pre>
     * a proof given from the ledger server
     * </pre>
     *
     * <code>.rpc.AssetProof proof = 2;</code>
     */
    public Builder mergeProof(com.scalar.dl.rpc.AssetProof value) {
      if (proofBuilder_ == null) {
        if (((bitField0_ & 0x00000002) != 0) &&
          proof_ != null &&
          proof_ != com.scalar.dl.rpc.AssetProof.getDefaultInstance()) {
          getProofBuilder().mergeFrom(value);
        } else {
          proof_ = value;
        }
      } else {
        proofBuilder_.mergeFrom(value);
      }
      if (proof_ != null) {
        bitField0_ |= 0x00000002;
        onChanged();
      }
      return this;
    }
    /**
     * <pre>
     * a proof given from the ledger server
     * </pre>
     *
     * <code>.rpc.AssetProof proof = 2;</code>
     */
    public Builder clearProof() {
      bitField0_ = (bitField0_ & ~0x00000002);
      proof_ = null;
      if (proofBuilder_ != null) {
        proofBuilder_.dispose();
        proofBuilder_ = null;
      }
      onChanged();
      return this;
    }
    /**
     * <pre>
     * a proof given from the ledger server
     * </pre>
     *
     * <code>.rpc.AssetProof proof = 2;</code>
     */
    public com.scalar.dl.rpc.AssetProof.Builder getProofBuilder() {
      bitField0_ |= 0x00000002;
      onChanged();
      return getProofFieldBuilder().getBuilder();
    }
    /**
     * <pre>
     * a proof given from the ledger server
     * </pre>
     *
     * <code>.rpc.AssetProof proof = 2;</code>
     */
    public com.scalar.dl.rpc.AssetProofOrBuilder getProofOrBuilder() {
      if (proofBuilder_ != null) {
        return proofBuilder_.getMessageOrBuilder();
      } else {
        return proof_ == null ?
            com.scalar.dl.rpc.AssetProof.getDefaultInstance() : proof_;
      }
    }
    /**
     * <pre>
     * a proof given from the ledger server
     * </pre>
     *
     * <code>.rpc.AssetProof proof = 2;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
        com.scalar.dl.rpc.AssetProof, com.scalar.dl.rpc.AssetProof.Builder, com.scalar.dl.rpc.AssetProofOrBuilder> 
        getProofFieldBuilder() {
      if (proofBuilder_ == null) {
        proofBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            com.scalar.dl.rpc.AssetProof, com.scalar.dl.rpc.AssetProof.Builder, com.scalar.dl.rpc.AssetProofOrBuilder>(
                getProof(),
                getParentForChildren(),
                isClean());
        proof_ = null;
      }
      return proofBuilder_;
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


    // @@protoc_insertion_point(builder_scope:rpc.LedgerValidationResponse)
  }

  // @@protoc_insertion_point(class_scope:rpc.LedgerValidationResponse)
  private static final com.scalar.dl.rpc.LedgerValidationResponse DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new com.scalar.dl.rpc.LedgerValidationResponse();
  }

  public static com.scalar.dl.rpc.LedgerValidationResponse getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<LedgerValidationResponse>
      PARSER = new com.google.protobuf.AbstractParser<LedgerValidationResponse>() {
    @java.lang.Override
    public LedgerValidationResponse parsePartialFrom(
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

  public static com.google.protobuf.Parser<LedgerValidationResponse> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<LedgerValidationResponse> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public com.scalar.dl.rpc.LedgerValidationResponse getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

