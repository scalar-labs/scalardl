package com.scalar.dl.ledger.contract;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Splitter;
import com.scalar.dl.ledger.crypto.ClientIdentityKey;
import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.exception.ContractValidationException;
import com.scalar.dl.ledger.model.ContractExecutionRequest;
import com.scalar.dl.ledger.model.ContractRegistrationRequest;
import com.scalar.dl.ledger.util.Time;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A container for (the bytecode of) a registered contract and data about that contract. In addition
 * to the contract, a {@code ContractEntry} will keep track of:
 *
 * <ul>
 *   <li>the id of an entity who registered the contract
 *   <li>a certificate version
 *   <li>a contract id
 *   <li>the binary name of the contract
 *   <li>any corresponding properties of the contract
 *   <li>the time in which the contract was registered
 *   <li>the signature of the contract
 * </ul>
 */
@ThreadSafe
public class ContractEntry {
  public static final String ENTITY_ID = "cert_holder_id";
  public static final String KEY_VERSION = "cert_version";
  public static final String ID = "id";
  public static final String BINARY_NAME = "binary_name";
  public static final String BYTE_CODE = "byte_code";
  public static final String PROPERTIES = "properties";
  public static final String REGISTERED_AT = "registered_at";
  public static final String SIGNATURE = "signature";
  private final String entityId;
  private final int keyVersion;
  private final String id;
  private final String binaryName;
  private final byte[] byteCode;
  @Nullable private final String properties;
  private final long registeredAt;
  private final byte[] signature;
  private final Key key;
  private final ClientIdentityKey clientIdentityKey;

  /**
   * Constructs a {@code ContractEntry} with the specified contract id, contract binary name, entity
   * id, certificate version, bytecode of the contract, properties of the contract, the time the
   * contract was registered at, and the signature of the contract.
   *
   * @param id a contract id
   * @param binaryName a binary name of the contract
   * @param entityId an entity id
   * @param keyVersion a certificate version
   * @param byteCode bytecode of the contract
   * @param properties properties of the contract
   * @param registeredAt the time the contract was registered
   * @param signature the signature of the contract
   */
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public ContractEntry(
      String id,
      String binaryName,
      String entityId,
      int keyVersion,
      byte[] byteCode,
      @Nullable String properties,
      long registeredAt,
      byte[] signature) {
    this.id = checkNotNull(id);
    this.binaryName = checkNotNull(binaryName);
    this.entityId = checkNotNull(entityId);
    checkArgument(keyVersion > 0);
    this.keyVersion = keyVersion;
    this.byteCode = checkNotNull(byteCode);
    this.properties = properties;
    this.registeredAt = registeredAt;
    this.signature = checkNotNull(signature);
    this.clientIdentityKey = createClientIdentityKey(entityId, keyVersion);
    this.key = new Key(id, clientIdentityKey);
  }

  /**
   * Returns the {@code Key} of the {@code ContractEntry}. A Key is made up of the contract id and a
   * {@code CertificateEntry.Key}.
   *
   * @return the {@code Key} of the {@code ContractEntry}
   */
  public Key getKey() {
    return key;
  }

  /**
   * Returns the {@link ClientIdentityKey} of the {@code ContractEntry}. The {@link
   * ClientIdentityKey} is made up of the entity id and version.
   *
   * @return the {@link ClientIdentityKey} of the {@code ContractEntry}
   */
  public ClientIdentityKey getClientIdentityKey() {
    return clientIdentityKey;
  }

  /**
   * Returns the id of the {@code ContractEntry}.
   *
   * @return the id of the {@code ContractEntry}
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the binary name of the {@code ContractEntry}.
   *
   * @return the binary name of the {@code ContractEntry}
   */
  public String getBinaryName() {
    return binaryName;
  }

  /**
   * Returns the entity id that holds the certificate of the {@code ContractEntry}.
   *
   * @return the entity id of the {@code ContractEntry}
   */
  public String getEntityId() {
    return entityId;
  }

  /**
   * Returns the version of the certificate or the HMAC secret key of the {@code ContractEntry}.
   *
   * @return the version of the certificate or the HMAC secret key of the {@code ContractEntry}
   */
  public int getKeyVersion() {
    return keyVersion;
  }

  /**
   * Returns the bytecode of the contract contained in the {@code ContractEntry}.
   *
   * @return the bytecode of the contract contained in the {@code ContractEntry}
   */
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public byte[] getByteCode() {
    return byteCode;
  }

  /**
   * Returns the properties of the {@code ContractEntry} (may be empty).
   *
   * @return the properties of the {@code ContractEntry}
   */
  public Optional<String> getProperties() {
    return Optional.ofNullable(properties);
  }

  /**
   * Returns the registered at time of the {@code ContractEntry}.
   *
   * @return the registered at time of the {@code ContractEntry}
   */
  public long getRegisteredAt() {
    return registeredAt;
  }

  /**
   * Returns the properties of the {@code ContractEntry} (may be empty).
   *
   * @return the properties of the {@code ContractEntry}
   */
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public byte[] getSignature() {
    return signature;
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return Objects.hash(
        entityId,
        keyVersion,
        id,
        binaryName,
        Arrays.hashCode(byteCode),
        properties,
        registeredAt,
        Arrays.hashCode(signature));
  }

  /**
   * Indicates whether some other object is "equal to" this object. The other object is considered
   * equal if it is also an {@code ContractEntry} and both instances have the same
   *
   * <ul>
   *   <li>id
   *   <li>entity id
   *   <li>key version
   *   <li>contract bytecode
   *   <li>contract properties
   *   <li>registered at time
   *   <li>signature
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
    if (!(o instanceof ContractEntry)) {
      return false;
    }
    ContractEntry another = (ContractEntry) o;
    return this.id.equals(another.id)
        && this.entityId.equals(another.entityId)
        && this.keyVersion == another.keyVersion
        && Arrays.equals(this.byteCode, another.byteCode)
        && Objects.equals(this.properties, another.properties)
        && this.registeredAt == another.registeredAt
        && Arrays.equals(this.signature, another.signature);
  }

  /**
   * Turns a {@code ContractRegistrationRequest} into a {@code ContractEntry} by adding a registered
   * at time, and returns it.
   *
   * @param request a {@link ContractRegistrationRequest}
   * @return a {@code ContractEntry}
   */
  public static ContractEntry from(ContractRegistrationRequest request) {
    return new ContractEntry(
        request.getContractId(),
        request.getContractBinaryName(),
        request.getEntityId(),
        request.getKeyVersion(),
        request.getContractByteCode(),
        request.getContractProperties().orElse(null),
        Time.getCurrentUtcTimeInMillis(),
        request.getSignature());
  }

  private static ClientIdentityKey createClientIdentityKey(String entityId, int keyVersion) {
    return new ClientIdentityKey() {
      @Override
      public String getEntityId() {
        return entityId;
      }

      @Override
      public int getKeyVersion() {
        return keyVersion;
      }
    };
  }

  /**
   * A {@code Key} to associate a contract id with a {@link ClientIdentityKey}. A {@link
   * ClientIdentityKey} contains an entity id and certificate version.
   */
  @Immutable
  public static class Key {
    private final String id;
    private final String entityId;
    private final int keyVersion;
    private static final String DELIMITER = "/";

    public Key(String id, ClientIdentityKey clientIdentityKey) {
      this.id = id;
      this.entityId = clientIdentityKey.getEntityId();
      this.keyVersion = clientIdentityKey.getKeyVersion();
    }

    public Key(String id, String entityId, int keyVersion) {
      this.id = id;
      this.entityId = entityId;
      this.keyVersion = keyVersion;
    }

    public String getId() {
      return id;
    }

    public ClientIdentityKey getClientIdentityKey() {
      return createClientIdentityKey(entityId, keyVersion);
    }

    public String serialize() {
      return entityId + DELIMITER + keyVersion + DELIMITER + id;
    }

    public static Key deserialize(String keyString) {
      List<String> parts = Splitter.on(DELIMITER).splitToList(keyString);
      if (parts.size() != 3) {
        throw new ContractValidationException(CommonError.INVALID_CONTRACT_ID_FORMAT);
      }
      return new Key(parts.get(2), parts.get(0), Integer.parseInt(parts.get(1)));
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, entityId, keyVersion);
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (!(o instanceof ContractEntry.Key)) {
        return false;
      }
      Key another = (Key) o;
      return this.id.equals(another.id)
          && this.entityId.equals(another.entityId)
          && this.keyVersion == another.keyVersion;
    }

    public static Key from(ContractExecutionRequest request) {
      return new ContractEntry.Key(
          request.getContractId(), request.getEntityId(), request.getKeyVersion());
    }
  }
}
