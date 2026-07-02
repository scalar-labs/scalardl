package com.scalar.dl.ledger.lease.scalardb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.DistributedStorageAdmin;
import com.scalar.db.config.DatabaseConfig;
import com.scalar.db.service.StorageFactory;
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.database.scalardb.ScalarNamespaceResolver;
import com.scalar.dl.ledger.lease.LeaseEntry;
import com.scalar.dl.ledger.lease.LeaseTableNotFoundException;
import com.scalar.dl.ledger.namespace.Namespaces;
import java.util.Properties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Integration test for {@link ScalarLease} against a real storage backend. Unit tests exercise the
 * conditional-mutation logic with a mocked storage; this test proves the compare-and-swap semantics
 * that only a real linearizable backend can validate:
 *
 * <ul>
 *   <li>an absent lease can be acquired by exactly one holder ({@code PutIfNotExists});
 *   <li>a renew/takeover succeeds only when the observed holder AND expiry still match ({@code
 *       PutIf}), so a stale observation cannot resurrect a lease that was already taken over.
 * </ul>
 *
 * <p>Storage connection settings come from {@code scalardb.*} system properties (default: Cassandra
 * on localhost), matching the other ledger integration tests.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ScalarLeaseIntegrationTest {
  private static final String PROP_STORAGE = "scalardb.storage";
  private static final String PROP_CONTACT_POINTS = "scalardb.contact_points";
  private static final String PROP_USERNAME = "scalardb.username";
  private static final String PROP_PASSWORD = "scalardb.password";
  private static final String DEFAULT_STORAGE = "cassandra";
  private static final String DEFAULT_CONTACT_POINTS = "localhost";
  private static final String DEFAULT_USERNAME = "cassandra";
  private static final String DEFAULT_PASSWORD = "cassandra";

  private static final String NAMESPACE = "lease_it";
  private static final String LEASE = "test_lease";
  private static final String HOLDER_A = "auditor/A";
  private static final String HOLDER_B = "auditor/B";

  private Properties props;
  private DistributedStorage storage;
  private DistributedStorageAdmin storageAdmin;
  private ScalarNamespaceResolver resolver;
  private ScalarLease lease;
  private String physicalNamespace;

  @BeforeAll
  public void setUpBeforeClass() throws Exception {
    props = createProperties();
    StorageFactory factory = StorageFactory.create(props);
    storage = factory.getStorage();
    storageAdmin = factory.getStorageAdmin();
    resolver = new ScalarNamespaceResolver(new LedgerConfig(props));
    physicalNamespace = resolver.resolve(Namespaces.DEFAULT);
    lease = new ScalarLease(storage, storageAdmin, resolver);

    storageAdmin.createNamespace(physicalNamespace, true);
    lease.createTable();
  }

  @AfterAll
  public void tearDownAfterClass() throws Exception {
    if (storageAdmin != null) {
      storageAdmin.dropTable(physicalNamespace, ScalarLease.TABLE, true);
      storageAdmin.dropNamespace(physicalNamespace, true);
      storageAdmin.close();
    }
    if (storage != null) {
      storage.close();
    }
  }

  @AfterEach
  public void tearDown() throws Exception {
    // Reset the single lease record between tests.
    storageAdmin.truncateTable(physicalNamespace, ScalarLease.TABLE);
  }

  private Properties createProperties() {
    Properties props = new Properties();
    props.put(LedgerConfig.NAMESPACE, NAMESPACE);
    props.put(DatabaseConfig.STORAGE, System.getProperty(PROP_STORAGE, DEFAULT_STORAGE));
    props.put(
        DatabaseConfig.CONTACT_POINTS,
        System.getProperty(PROP_CONTACT_POINTS, DEFAULT_CONTACT_POINTS));
    props.put(DatabaseConfig.USERNAME, System.getProperty(PROP_USERNAME, DEFAULT_USERNAME));
    props.put(DatabaseConfig.PASSWORD, System.getProperty(PROP_PASSWORD, DEFAULT_PASSWORD));
    return props;
  }

  @Test
  public void get_WhenLeaseNeverAcquired_ShouldReturnEmpty() {
    assertThat(lease.get(LEASE)).isEmpty();
  }

  @Test
  public void tryAcquireOrRenew_WhenAbsent_ShouldLetExactlyOneHolderAcquire() {
    // The first holder acquires the absent lease; a second holder's PutIfNotExists must fail
    // because
    // the record now exists. This proves the acquire is a real compare-and-swap.
    assertThat(lease.tryAcquireOrRenew(LEASE, null, HOLDER_A, 1000L)).isTrue();
    assertThat(lease.tryAcquireOrRenew(LEASE, null, HOLDER_B, 1000L)).isFalse();

    LeaseEntry entry = lease.get(LEASE).orElseThrow(AssertionError::new);
    assertThat(entry.getHolder()).isEqualTo(HOLDER_A);
    assertThat(entry.getExpiry()).isEqualTo(1000L);
  }

  @Test
  public void tryAcquireOrRenew_WhenHolderRenewsWithMatchingObserved_ShouldSucceed() {
    assertThat(lease.tryAcquireOrRenew(LEASE, null, HOLDER_A, 1000L)).isTrue();
    LeaseEntry observed = lease.get(LEASE).orElseThrow(AssertionError::new);

    assertThat(lease.tryAcquireOrRenew(LEASE, observed, HOLDER_A, 2000L)).isTrue();

    assertThat(lease.get(LEASE).orElseThrow(AssertionError::new).getExpiry()).isEqualTo(2000L);
  }

  @Test
  public void tryAcquireOrRenew_WhenRenewingWithStaleObserved_ShouldFail() {
    assertThat(lease.tryAcquireOrRenew(LEASE, null, HOLDER_A, 1000L)).isTrue();
    LeaseEntry stale = lease.get(LEASE).orElseThrow(AssertionError::new);

    // Renew once so the stored expiry moves from 1000 to 2000.
    assertThat(lease.tryAcquireOrRenew(LEASE, stale, HOLDER_A, 2000L)).isTrue();

    // A second attempt conditioned on the now-stale observation (expiry 1000) must fail: the
    // compare-and-swap on expiry prevents resurrecting a superseded lease.
    assertThat(lease.tryAcquireOrRenew(LEASE, stale, HOLDER_A, 3000L)).isFalse();
    assertThat(lease.get(LEASE).orElseThrow(AssertionError::new).getExpiry()).isEqualTo(2000L);
  }

  @Test
  public void tryAcquireOrRenew_WhenTakingOverExpiredLease_ShouldSucceedAndBlockStaleOwner() {
    assertThat(lease.tryAcquireOrRenew(LEASE, null, HOLDER_A, 1000L)).isTrue();
    LeaseEntry observedByBoth = lease.get(LEASE).orElseThrow(AssertionError::new);

    // Failover: B observes the (expired) lease and takes it over.
    assertThat(lease.tryAcquireOrRenew(LEASE, observedByBoth, HOLDER_B, 5000L)).isTrue();
    assertThat(lease.get(LEASE).orElseThrow(AssertionError::new).getHolder()).isEqualTo(HOLDER_B);

    // The old holder, still holding its stale observation, cannot reclaim the lease.
    assertThat(lease.tryAcquireOrRenew(LEASE, observedByBoth, HOLDER_A, 6000L)).isFalse();
  }

  @Test
  public void createTable_WhenAlreadyExists_ShouldBeIdempotent() {
    assertThat(lease.get(LEASE)).isEmpty();
    // The table was created in setUp; calling again must be a no-op (ifNotExists), and the lease is
    // still usable afterwards.
    lease.createTable();
    assertThat(lease.tryAcquireOrRenew(LEASE, null, HOLDER_A, 1000L)).isTrue();
  }

  @Test
  @SuppressWarnings("resource")
  public void get_WhenTableDropped_ShouldThrowLeaseTableNotFoundException() throws Exception {
    storageAdmin.dropTable(physicalNamespace, ScalarLease.TABLE, true);

    // Read through a fresh storage instance with no cached table metadata, so the drop is observed
    // as a genuinely missing table (a warm storage cache would instead surface a lower-level
    // error).
    StorageFactory freshFactory = StorageFactory.create(props);
    DistributedStorage freshStorage = freshFactory.getStorage();
    DistributedStorageAdmin freshStorageAdmin = freshFactory.getStorageAdmin();
    try {
      ScalarLease freshLease = new ScalarLease(freshStorage, freshStorageAdmin, resolver);
      assertThatThrownBy(() -> freshLease.get(LEASE))
          .isInstanceOf(LeaseTableNotFoundException.class);
    } finally {
      // Restore the table for the remaining tests / teardown.
      lease.createTable();
      freshStorage.close();
      freshStorageAdmin.close();
    }
  }
}
