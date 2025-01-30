package com.scalar.dl.ledger.crypto;

import com.scalar.dl.ledger.exception.SignatureException;
import com.scalar.dl.ledger.exception.UnloadableKeyException;
import com.scalar.dl.ledger.util.CryptoUtils;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import javax.annotation.concurrent.Immutable;

/** A signer for a byte array with a specified private key supplied in pem format. */
@Immutable
public class DigitalSignatureSigner implements SignatureSigner {
  private static final String DEFAULT_ALGORITHM = "SHA256withECDSA";
  private final PrivateKey privateKey;
  private final String algorithm;

  public DigitalSignatureSigner(Path privateKeyPath) {
    this(privateKeyPath, DEFAULT_ALGORITHM);
  }

  public DigitalSignatureSigner(Path privateKeyPath, String algorithm) {
    this(readKeyFile(privateKeyPath), algorithm);
  }

  public DigitalSignatureSigner(String privateKeyInPem) {
    this(privateKeyInPem, DEFAULT_ALGORITHM);
  }

  public DigitalSignatureSigner(String privateKeyInPem, String algorithm) {
    this.algorithm = algorithm;
    try {
      this.privateKey = CryptoUtils.getPrivateKey(new StringReader(privateKeyInPem));
    } catch (IOException | IllegalArgumentException e) {
      throw new UnloadableKeyException(e.getMessage());
    }
  }

  /**
   * Signs the given byte array with a {@link Signature}.
   *
   * @param bytes the byte array to sign
   * @return the signature bytes after signing
   * @throws SignatureException if it fails to sign
   */
  @Override
  public byte[] sign(byte[] bytes) {
    try {
      Signature signature = Signature.getInstance(algorithm);
      signature.initSign(privateKey);
      signature.update(bytes);
      return signature.sign();
    } catch (java.security.SignatureException | NoSuchAlgorithmException | InvalidKeyException e) {
      throw new SignatureException(e.getMessage());
    }
  }

  private static String readKeyFile(Path file) {
    try {
      return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UnloadableKeyException(e.getMessage());
    }
  }
}
