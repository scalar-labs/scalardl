// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: scalar.proto

// Protobuf Java Version: 3.25.5
package com.scalar.dl.rpc;

public interface CertificateRegistrationRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:rpc.CertificateRegistrationRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>string entity_id = 1;</code>
   * @return The entityId.
   */
  java.lang.String getEntityId();
  /**
   * <code>string entity_id = 1;</code>
   * @return The bytes for entityId.
   */
  com.google.protobuf.ByteString
      getEntityIdBytes();

  /**
   * <code>uint32 key_version = 2;</code>
   * @return The keyVersion.
   */
  int getKeyVersion();

  /**
   * <code>string cert_pem = 3;</code>
   * @return The certPem.
   */
  java.lang.String getCertPem();
  /**
   * <code>string cert_pem = 3;</code>
   * @return The bytes for certPem.
   */
  com.google.protobuf.ByteString
      getCertPemBytes();
}
