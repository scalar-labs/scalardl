package com.scalar.dl.ledger.proof;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.dl.ledger.namespace.Namespaces;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class AssetProofTest {

  /**
   * This test verifies backward compatibility of the serialize() method. Before namespace support
   * was added, the serialization format was: id + age + nonce + input + hash + prevHash (without
   * namespace). For the default namespace, we must maintain this legacy format to ensure
   * compatibility with older Auditor versions that don't include namespace in signature
   * verification.
   */
  @Test
  public void serialize_withDefaultNamespace_shouldMatchLegacyFormatWithoutNamespace() {
    // Arrange
    String id = "asset-id";
    int age = 1;
    String nonce = "nonce123";
    String input = "input-data";
    byte[] hash = new byte[] {0x01, 0x02, 0x03, 0x04};
    byte[] prevHash = new byte[] {0x05, 0x06, 0x07, 0x08};

    // Build expected bytes in legacy format (without namespace)
    byte[] idBytes = id.getBytes(StandardCharsets.UTF_8);
    byte[] nonceBytes = nonce.getBytes(StandardCharsets.UTF_8);
    byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
    ByteBuffer expectedBuffer =
        ByteBuffer.allocate(
            idBytes.length
                + Integer.BYTES
                + nonceBytes.length
                + inputBytes.length
                + hash.length
                + prevHash.length);
    expectedBuffer.put(idBytes);
    expectedBuffer.putInt(age);
    expectedBuffer.put(nonceBytes);
    expectedBuffer.put(inputBytes);
    expectedBuffer.put(hash);
    expectedBuffer.put(prevHash);
    byte[] expected = expectedBuffer.array();

    // Act
    byte[] actual = AssetProof.serialize(Namespaces.DEFAULT, id, age, nonce, input, hash, prevHash);

    // Assert
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void serialize_withNonDefaultNamespace_shouldIncludeNamespaceInSerialization() {
    // Arrange
    String namespace = "custom-namespace";
    String id = "asset-id";
    int age = 1;
    String nonce = "nonce123";
    String input = "input-data";
    byte[] hash = new byte[] {0x01, 0x02, 0x03, 0x04};
    byte[] prevHash = new byte[] {0x05, 0x06, 0x07, 0x08};

    // Build expected bytes with namespace included
    byte[] namespaceBytes = namespace.getBytes(StandardCharsets.UTF_8);
    byte[] idBytes = id.getBytes(StandardCharsets.UTF_8);
    byte[] nonceBytes = nonce.getBytes(StandardCharsets.UTF_8);
    byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
    ByteBuffer expectedBuffer =
        ByteBuffer.allocate(
            namespaceBytes.length
                + idBytes.length
                + Integer.BYTES
                + nonceBytes.length
                + inputBytes.length
                + hash.length
                + prevHash.length);
    expectedBuffer.put(namespaceBytes);
    expectedBuffer.put(idBytes);
    expectedBuffer.putInt(age);
    expectedBuffer.put(nonceBytes);
    expectedBuffer.put(inputBytes);
    expectedBuffer.put(hash);
    expectedBuffer.put(prevHash);
    byte[] expected = expectedBuffer.array();

    // Act
    byte[] actual = AssetProof.serialize(namespace, id, age, nonce, input, hash, prevHash);

    // Assert
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void serialize_withDefaultNamespace_shouldBeShorterThanNonDefaultNamespace() {
    // Arrange
    String nonDefaultNamespace = "custom";
    String id = "asset-id";
    int age = 0;
    String nonce = "nonce";
    String input = "input";
    byte[] hash = new byte[] {0x01, 0x02};
    byte[] prevHash = null;

    // Act
    byte[] defaultResult =
        AssetProof.serialize(Namespaces.DEFAULT, id, age, nonce, input, hash, prevHash);
    byte[] nonDefaultResult =
        AssetProof.serialize(nonDefaultNamespace, id, age, nonce, input, hash, prevHash);

    // Assert
    assertThat(defaultResult.length + nonDefaultNamespace.length())
        .isEqualTo(nonDefaultResult.length);
  }
}
