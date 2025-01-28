package com.scalar.dl.ledger.config;

import static com.google.common.base.Preconditions.checkArgument;

import com.scalar.dl.ledger.error.CommonError;
import java.util.Objects;

public class GrpcServerConfig {
  private final int maxInboundMessageSize;
  private final int maxInboundMetadataSize;

  private GrpcServerConfig(GrpcServerConfig.Builder builder) {
    this.maxInboundMessageSize = builder.maxInboundMessageSize;
    this.maxInboundMetadataSize = builder.maxInboundMetadataSize;
  }

  public int getMaxInboundMessageSize() {
    return maxInboundMessageSize;
  }

  public int getMaxInboundMetadataSize() {
    return maxInboundMetadataSize;
  }

  @Override
  public int hashCode() {
    return Objects.hash(maxInboundMessageSize, maxInboundMetadataSize);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof GrpcServerConfig)) {
      return false;
    }
    GrpcServerConfig another = (GrpcServerConfig) o;
    return maxInboundMessageSize == another.maxInboundMessageSize
        && maxInboundMetadataSize == another.maxInboundMetadataSize;
  }

  public static GrpcServerConfig.Builder newBuilder() {
    return new GrpcServerConfig.Builder();
  }

  public static final class Builder {
    private int maxInboundMessageSize;
    private int maxInboundMetadataSize;

    Builder() {}

    public GrpcServerConfig.Builder maxInboundMessageSize(int maxInboundMessageSize) {
      checkArgument(
          maxInboundMessageSize >= 0,
          CommonError.GRPC_MAX_INBOUND_MESSAGE_SIZE_MUST_BE_GREATER_THAN_OR_EQUAL_TO_ZERO
              .buildMessage());
      this.maxInboundMessageSize = maxInboundMessageSize;
      return this;
    }

    public GrpcServerConfig.Builder maxInboundMetadataSize(int maxInboundMetadataSize) {
      checkArgument(
          maxInboundMetadataSize >= 0,
          CommonError.GRPC_MAX_INBOUND_METADATA_SIZE_MUST_BE_GREATER_THAN_OR_EQUAL_TO_ZERO
              .buildMessage());
      this.maxInboundMetadataSize = maxInboundMetadataSize;
      return this;
    }

    public GrpcServerConfig build() {
      return new GrpcServerConfig(this);
    }
  }
}
