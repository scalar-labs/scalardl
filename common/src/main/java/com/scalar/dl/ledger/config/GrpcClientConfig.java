package com.scalar.dl.ledger.config;

import static com.google.common.base.Preconditions.checkArgument;

import com.scalar.dl.ledger.error.CommonError;
import java.util.Objects;

public class GrpcClientConfig {
  private final long deadlineDurationMillis;
  private final int maxInboundMessageSize;
  private final int maxInboundMetadataSize;

  private GrpcClientConfig(GrpcClientConfig.Builder builder) {
    this.deadlineDurationMillis = builder.deadlineDurationMillis;
    this.maxInboundMessageSize = builder.maxInboundMessageSize;
    this.maxInboundMetadataSize = builder.maxInboundMetadataSize;
  }

  public long getDeadlineDurationMillis() {
    return deadlineDurationMillis;
  }

  public int getMaxInboundMessageSize() {
    return maxInboundMessageSize;
  }

  public int getMaxInboundMetadataSize() {
    return maxInboundMetadataSize;
  }

  @Override
  public int hashCode() {
    return Objects.hash(deadlineDurationMillis, maxInboundMessageSize, maxInboundMetadataSize);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof GrpcClientConfig)) {
      return false;
    }
    GrpcClientConfig another = (GrpcClientConfig) o;
    return deadlineDurationMillis == another.deadlineDurationMillis
        && maxInboundMessageSize == another.maxInboundMessageSize
        && maxInboundMetadataSize == another.maxInboundMetadataSize;
  }

  public static GrpcClientConfig.Builder newBuilder() {
    return new GrpcClientConfig.Builder();
  }

  public static final class Builder {
    private long deadlineDurationMillis;
    private int maxInboundMessageSize;
    private int maxInboundMetadataSize;

    Builder() {}

    public GrpcClientConfig.Builder deadlineDurationMillis(long deadlineDurationMillis) {
      checkArgument(
          deadlineDurationMillis >= 0,
          CommonError.GRPC_DEADLINE_DURATION_MUST_BE_GREATER_THAN_OR_EQUAL_TO_ZERO.buildMessage());
      this.deadlineDurationMillis = deadlineDurationMillis;
      return this;
    }

    public GrpcClientConfig.Builder maxInboundMessageSize(int maxInboundMessageSize) {
      checkArgument(
          maxInboundMessageSize >= 0,
          CommonError.GRPC_MAX_INBOUND_MESSAGE_SIZE_MUST_BE_GREATER_THAN_OR_EQUAL_TO_ZERO
              .buildMessage());
      this.maxInboundMessageSize = maxInboundMessageSize;
      return this;
    }

    public GrpcClientConfig.Builder maxInboundMetadataSize(int maxInboundMetadataSize) {
      checkArgument(
          maxInboundMetadataSize >= 0,
          CommonError.GRPC_MAX_INBOUND_METADATA_SIZE_MUST_BE_GREATER_THAN_OR_EQUAL_TO_ZERO
              .buildMessage());
      this.maxInboundMetadataSize = maxInboundMetadataSize;
      return this;
    }

    public GrpcClientConfig build() {
      return new GrpcClientConfig(this);
    }
  }
}
