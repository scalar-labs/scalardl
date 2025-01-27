package com.scalar.dl.ledger.service;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.config.DatabaseConfig;
import com.scalar.db.service.StorageModule;
import com.scalar.db.service.TransactionModule;
import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.config.ServersHmacAuthenticatable;
import com.scalar.dl.ledger.contract.ContractLoader;
import com.scalar.dl.ledger.contract.ContractManager;
import com.scalar.dl.ledger.crypto.AuditorKeyValidator;
import com.scalar.dl.ledger.crypto.CertificateManager;
import com.scalar.dl.ledger.crypto.Cipher;
import com.scalar.dl.ledger.crypto.CipherFactory;
import com.scalar.dl.ledger.crypto.ClientKeyValidator;
import com.scalar.dl.ledger.crypto.DigitalSignatureSigner;
import com.scalar.dl.ledger.crypto.HmacSigner;
import com.scalar.dl.ledger.crypto.SecretManager;
import com.scalar.dl.ledger.crypto.SignatureSigner;
import com.scalar.dl.ledger.database.CertificateRegistry;
import com.scalar.dl.ledger.database.ContractRegistry;
import com.scalar.dl.ledger.database.FunctionRegistry;
import com.scalar.dl.ledger.database.SecretRegistry;
import com.scalar.dl.ledger.database.TransactionManager;
import com.scalar.dl.ledger.database.scalardb.DefaultTamperEvidentAssetComposer;
import com.scalar.dl.ledger.database.scalardb.ScalarCertificateRegistry;
import com.scalar.dl.ledger.database.scalardb.ScalarContractRegistry;
import com.scalar.dl.ledger.database.scalardb.ScalarFunctionRegistry;
import com.scalar.dl.ledger.database.scalardb.ScalarSecretRegistry;
import com.scalar.dl.ledger.database.scalardb.ScalarTransactionManager;
import com.scalar.dl.ledger.database.scalardb.TamperEvidentAssetComposer;
import com.scalar.dl.ledger.database.scalardb.TransactionStateManager;
import com.scalar.dl.ledger.function.FunctionLoader;
import com.scalar.dl.ledger.function.FunctionManager;
import java.net.SocketPermission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.util.Base64;
import java.util.Locale;
import java.util.PropertyPermission;
import javax.annotation.Nullable;
import javax.inject.Named;

public class LedgerModule extends AbstractModule {
  private static final String DUMMY_CIPHER_KEY = "dummy-cipher-key";
  private static final String SALT = "RnW+ANA7ucRdXmRI125kMimsSos=";
  private final LedgerConfig config;
  private final DatabaseConfig databaseConfig;

  public LedgerModule(LedgerConfig config) {
    this.config = config;
    this.databaseConfig = config.getDatabaseConfig();
  }

  @Override
  protected void configure() {
    bind(CertificateManager.class).in(Singleton.class);
    bind(CertificateRegistry.class).to(ScalarCertificateRegistry.class).in(Singleton.class);
    bind(SecretManager.class).in(Singleton.class);
    bind(SecretRegistry.class).to(ScalarSecretRegistry.class).in(Singleton.class);
    bind(ContractManager.class).in(Singleton.class);
    bind(ContractRegistry.class).to(ScalarContractRegistry.class).in(Singleton.class);
    bind(ContractLoader.class).in(Singleton.class);
    bind(FunctionManager.class).in(Singleton.class);
    bind(FunctionRegistry.class).to(ScalarFunctionRegistry.class).in(Singleton.class);
    bind(FunctionLoader.class).in(Singleton.class);
    bind(TransactionManager.class).to(ScalarTransactionManager.class).in(Singleton.class);
    bind(TamperEvidentAssetComposer.class).to(DefaultTamperEvidentAssetComposer.class);
    bind(TransactionStateManager.class).in(Singleton.class);
    bind(ClientKeyValidator.class).in(Singleton.class);
    bind(AuditorKeyValidator.class).in(Singleton.class);
  }

  @Provides
  DatabaseConfig provideDatabaseConfig() {
    return databaseConfig;
  }

  @Provides
  LedgerConfig provideLedgerConfig() {
    return config;
  }

  @Provides
  AuthenticationMethod provideAuthenticationMethod() {
    return config.getAuthenticationMethod();
  }

  @Provides
  ServersHmacAuthenticatable getServersHmacAuthenticatable() {
    return config;
  }

  @Nullable
  @Singleton
  @Provides
  SignatureSigner provideSignatureSigner() {
    if (!config.isProofEnabled()) {
      return null;
    }
    if (config.getServersAuthenticationHmacSecretKey() == null) {
      return new DigitalSignatureSigner(config.getProofPrivateKey());
    } else {
      return new HmacSigner(config.getServersAuthenticationHmacSecretKey());
    }
  }

  @Provides
  @Singleton
  DistributedStorage provideDistributedStorage() {
    Injector injector = Guice.createInjector(new StorageModule(databaseConfig));
    DistributedStorage storage = injector.getInstance(DistributedStorage.class);
    storage.withNamespace(config.getNamespace());
    return storage;
  }

  @Provides
  @Singleton
  DistributedTransactionManager provideDistributedTransactionManager() {
    Injector injector = Guice.createInjector(new TransactionModule(databaseConfig));
    DistributedTransactionManager manager =
        injector.getInstance(DistributedTransactionManager.class);
    manager.withNamespace(config.getNamespace());
    return manager;
  }

  @Provides
  @Singleton
  ProtectionDomain provideProtectionDomain() {
    return getProtectionDomain(databaseConfig);
  }

  @Provides
  @Singleton
  @Named("SecretRegistry")
  @SuppressWarnings("unused")
  Cipher provideCipher() {
    if (config.getAuthenticationMethod() == AuthenticationMethod.HMAC) {
      return CipherFactory.create(config.getHmacCipherKey(), Base64.getDecoder().decode(SALT));
    } else {
      // this cipher key won't be used.
      return CipherFactory.create(DUMMY_CIPHER_KEY, Base64.getDecoder().decode(SALT));
    }
  }

  public static ProtectionDomain getProtectionDomain(DatabaseConfig databaseConfig) {
    // (null, null) means that it denies all if java.security.manager is enabled
    PermissionCollection permissionCollection = new Permissions();
    permissionCollection.add(new RuntimePermission("createClassLoader"));
    // For Scalar DB on Cosmos DB
    permissionCollection.add(new RuntimePermission("getenv.*"));
    permissionCollection.add(new PropertyPermission("log4j2.flowMessageFactory", "read"));
    permissionCollection.add(new PropertyPermission("org.jooq.settings", "read"));
    permissionCollection.add(new PropertyPermission("org.jooq.no-logo", "read"));
    permissionCollection.add(new PropertyPermission("COSMOS.*", "read"));
    permissionCollection.add(new PropertyPermission("line.separator", "read"));
    // For Scalar DB on DynamoDB
    permissionCollection.add(new PropertyPermission("aws.executionEnvironment", "read"));
    permissionCollection.add(new PropertyPermission("com.amazonaws.xray.traceHeader", "read"));
    if (databaseConfig.getStorage().toLowerCase(Locale.ROOT).equals("dynamo")) {
      permissionCollection.add(new SocketPermission("*", "connect,resolve"));
    }
    return new ProtectionDomain(null, permissionCollection);
  }
}
