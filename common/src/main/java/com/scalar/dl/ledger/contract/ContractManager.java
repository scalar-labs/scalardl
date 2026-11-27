package com.scalar.dl.ledger.contract;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.scalar.dl.ledger.crypto.ClientKeyValidator;
import com.scalar.dl.ledger.crypto.SignatureValidator;
import com.scalar.dl.ledger.database.ContractRegistry;
import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.exception.ContractValidationException;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.MissingContractException;
import com.scalar.dl.ledger.exception.UnloadableContractException;
import com.scalar.dl.ledger.model.ContractRegistrationRequest;
import com.scalar.dl.ledger.namespace.Namespaces;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A manager to store {@link ContractEntry}s in a {@link ContractRegistry}, retrieve {@link
 * ContractEntry}s, instantiate {@link ContractMachine}s from their ids, and also validate contracts
 * (i.e. check if the contract is registered with the correct signature).
 */
@ThreadSafe
public class ContractManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(ContractManager.class.getName());
  private static final int CACHE_SIZE = 1048576;
  private static final int VALIDATION_WINDOW_DAYS = 7;
  private final ContractRegistry registry;
  private final ContractLoader loader;
  private final ClientKeyValidator clientKeyValidator;
  private final ContractValidationFallbackPolicy fallbackPolicy;
  private final ConcurrentMap<String, Cache<ContractEntry.Key, Object>> recentlyValidatedCaches;

  /**
   * Constructs a {@code ContractManager} that does not allow the fallback validation of registered
   * contracts.
   *
   * @param registry a {@link ContractRegistry}
   * @param loader a {@link ContractLoader}
   * @param clientKeyValidator a {@link ClientKeyValidator}
   */
  public ContractManager(
      ContractRegistry registry, ContractLoader loader, ClientKeyValidator clientKeyValidator) {
    this(registry, loader, clientKeyValidator, new DisabledContractValidationFallbackPolicy());
  }

  @Inject
  public ContractManager(
      ContractRegistry registry,
      ContractLoader loader,
      ClientKeyValidator clientKeyValidator,
      ContractValidationFallbackPolicy fallbackPolicy) {
    this(registry, loader, clientKeyValidator, fallbackPolicy, new ConcurrentHashMap<>());
  }

  @VisibleForTesting
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public ContractManager(
      ContractRegistry registry,
      ContractLoader loader,
      ClientKeyValidator clientKeyValidator,
      ContractValidationFallbackPolicy fallbackPolicy,
      ConcurrentMap<String, Cache<ContractEntry.Key, Object>> recentlyValidatedCaches) {
    this.registry = registry;
    this.loader = loader;
    this.clientKeyValidator = clientKeyValidator;
    this.fallbackPolicy = fallbackPolicy;
    this.recentlyValidatedCaches = recentlyValidatedCaches;
  }

  /**
   * Stores a {@link ContractEntry} in a {@link ContractRegistry}. It will first check if the {@link
   * ContractEntry} is stored in the {@link ContractRegistry}, and if so, throw a {@link
   * DatabaseException} and fail. Otherwise, it will store it in the {@link ContractRegistry}.
   *
   * @param namespace a namespace to register the contract
   * @param entry a {@link ContractEntry}
   */
  public void register(String namespace, ContractEntry entry) {
    try {
      // NOTICE: register and get are not linearizable, so get in between successive register
      // operations for the same entry might return inconsistent results.
      // We don't pay much attention to the potential issue since registering the same contract
      // entry multiple times is a wrong usage.
      get(namespace, entry.getKey());
      // Throw an exception because registering the same contract might cause invalidation of ledger
      // entries
      throw new DatabaseException(CommonError.CONTRACT_ALREADY_REGISTERED);
    } catch (MissingContractException e) {
      // ignore
    }

    // validate the signature of a specified contract.
    validateContract(namespace, entry);
    // verify if a specified contract is loadable.
    loadContract(namespace, entry);

    registry.bind(namespace, entry);
  }

  /**
   * Returns a {@link ContractEntry}. It will retrieve it from the {@link ContractRegistry}.
   *
   * @param namespace a namespace for the contract
   * @param key the key of the {@link ContractEntry} to retrieve
   * @return a {@link ContractEntry}
   */
  public ContractEntry get(String namespace, ContractEntry.Key key) {
    return registry.lookup(namespace, key);
  }

  /**
   * Returns a list of all the {@link ContractEntry}s of a specific version registered by a
   * specified entity id.
   *
   * @param namespace a namespace for the contracts
   * @param entityId an entity ID.
   * @param keyVersion the version of a key (certificate or HMAC secret key)
   * @return a list of {@link ContractEntry}s
   */
  public List<ContractEntry> scan(String namespace, String entityId, int keyVersion) {
    return registry.scan(namespace, entityId, keyVersion);
  }

  /**
   * Instantiates a {@link ContractMachine} from the specified contract entry.
   *
   * @param namespace a namespace where the contract exists
   * @param entry the entry of the {@link ContractMachine} to retrieve
   * @return a {@link ContractMachine}
   */
  public ContractMachine getInstance(String namespace, ContractEntry entry) {
    Cache<ContractEntry.Key, Object> cache =
        recentlyValidatedCaches.computeIfAbsent(namespace, this::createCache);
    if (cache.getIfPresent(entry.getKey()) == null) {
      validateRegisteredContract(namespace, entry);
      cache.put(entry.getKey(), new Object());
    }
    return loadContract(namespace, entry);
  }

  @VisibleForTesting
  Class<?> defineClass(ContractEntry entry) {
    try {
      return loader.defineClass(entry);
    } catch (Exception | NoClassDefFoundError e) {
      // Errors should not be caught in general but it is necessary in our case
      // since contract creators and the loader are usually different and the loader should let
      // the creators know what is happening for them to take proper actions.
      throw new UnloadableContractException(CommonError.LOADING_CONTRACT_FAILED, e, e.getMessage());
    }
  }

  private ContractMachine loadContract(String namespace, ContractEntry entry) {
    Class<?> contractClazz = defineClass(entry);
    ContractMachine contract = new ContractMachine(createInstance(contractClazz));
    contract.initialize(this, namespace, entry.getClientIdentityKey());
    return contract;
  }

  private Object createInstance(Class<?> clazz) {
    try {
      return clazz.getConstructor().newInstance();
    } catch (Exception e) {
      throw new UnloadableContractException(CommonError.LOADING_CONTRACT_FAILED, e.getMessage());
    }
  }

  /**
   * Validates the signature of a contract to be registered. The signature must have been created
   * for the specified namespace.
   */
  @VisibleForTesting
  void validateContract(String namespace, ContractEntry entry) {
    SignatureValidator validator =
        clientKeyValidator.getValidator(namespace, entry.getEntityId(), entry.getKeyVersion());

    if (!isSignatureValid(validator, entry, toSignedNamespace(namespace))) {
      throw new ContractValidationException(CommonError.CONTRACT_VALIDATION_FAILED);
    }
  }

  /**
   * Validates the signature of an already registered contract. The signature is first validated for
   * the specified namespace. If it does not match, the specified namespace is not the default
   * namespace, and the {@link ContractValidationFallbackPolicy} allows it for the namespace, the
   * signature is validated again as the one created for the default namespace so that a contract
   * that was registered in the default namespace and then migrated to the specified namespace can
   * be used as it is.
   *
   * <p>This means that a contract entry signed for the default namespace is valid in any namespace
   * where the fallback validation is allowed. Moreover, since the signed data is a concatenation of
   * the fields without delimiters and the default namespace is not included in it, a contract entry
   * signed for a non-default namespace also passes the validation in such a namespace if the name
   * of the namespace that it was signed for is appended to its properties. In both cases, the entry
   * is validated with the key that is registered in the specified namespace for its entity ID and
   * key version, so it is accepted only in a namespace where the same key is registered for them.
   *
   * <p>Replacing a contract entry in one of the Ledger and the Auditor with such an entry is
   * detected as an inconsistency between them when the contract is executed, but it is not detected
   * in the Ledger-only configuration. The fallback validation should be allowed only where the
   * replacement is detected.
   */
  @VisibleForTesting
  void validateRegisteredContract(String namespace, ContractEntry entry) {
    SignatureValidator validator =
        clientKeyValidator.getValidator(namespace, entry.getEntityId(), entry.getKeyVersion());

    if (isSignatureValid(validator, entry, toSignedNamespace(namespace))) {
      return;
    }

    if (!namespace.equals(Namespaces.DEFAULT)
        && fallbackPolicy.isFallbackAllowed(namespace)
        && isSignatureValid(validator, entry, null)) {
      LOGGER.info(
          "The contract was validated with the signature for the default namespace. "
              + "Namespace: {}; Entity ID: {}; Key version: {}; Contract ID: {}",
          namespace,
          entry.getEntityId(),
          entry.getKeyVersion(),
          entry.getId());
      return;
    }

    // The contract was properly registered with the correct signature,
    // so this validation failure indicates potential malicious behavior.
    throw new ContractValidationException(CommonError.CONTRACT_VALIDATION_FAILED);
  }

  @Nullable
  private String toSignedNamespace(String namespace) {
    // The default namespace is not included in the signed data for backward compatibility.
    return namespace.equals(Namespaces.DEFAULT) ? null : namespace;
  }

  private boolean isSignatureValid(
      SignatureValidator validator, ContractEntry entry, @Nullable String signedNamespace) {
    byte[] serialized =
        ContractRegistrationRequest.serialize(
            entry.getId(),
            entry.getBinaryName(),
            entry.getByteCode(),
            entry.getProperties().orElse(null),
            signedNamespace,
            entry.getEntityId(),
            entry.getKeyVersion());

    return validator.validate(serialized, entry.getSignature());
  }

  private Cache<ContractEntry.Key, Object> createCache(String namespace) {
    return CacheBuilder.newBuilder()
        .maximumSize(CACHE_SIZE)
        .expireAfterWrite(VALIDATION_WINDOW_DAYS, TimeUnit.DAYS)
        .build();
  }
}
