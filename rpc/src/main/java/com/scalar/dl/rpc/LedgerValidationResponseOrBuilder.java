// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: scalar.proto

// Protobuf Java Version: 3.25.5
package com.scalar.dl.rpc;

public interface LedgerValidationResponseOrBuilder extends
    // @@protoc_insertion_point(interface_extends:rpc.LedgerValidationResponse)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>uint32 status_code = 1;</code>
   * @return The statusCode.
   */
  int getStatusCode();

  /**
   * <pre>
   * a proof given from the ledger server
   * </pre>
   *
   * <code>.rpc.AssetProof proof = 2;</code>
   * @return Whether the proof field is set.
   */
  boolean hasProof();
  /**
   * <pre>
   * a proof given from the ledger server
   * </pre>
   *
   * <code>.rpc.AssetProof proof = 2;</code>
   * @return The proof.
   */
  com.scalar.dl.rpc.AssetProof getProof();
  /**
   * <pre>
   * a proof given from the ledger server
   * </pre>
   *
   * <code>.rpc.AssetProof proof = 2;</code>
   */
  com.scalar.dl.rpc.AssetProofOrBuilder getProofOrBuilder();
}
