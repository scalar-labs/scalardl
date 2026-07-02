package com.scalar.dl.ledger.lease.scalardb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.DistributedStorageAdmin;
import com.scalar.db.api.Put;
import com.scalar.db.api.PutIf;
import com.scalar.db.api.PutIfNotExists;
import com.scalar.db.api.Result;
import com.scalar.db.common.CoreError;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.exception.storage.NoMutationException;
import com.scalar.dl.ledger.database.scalardb.ScalarNamespaceResolver;
import com.scalar.dl.ledger.lease.LeaseEntry;
import com.scalar.dl.ledger.lease.LeaseException;
import com.scalar.dl.ledger.lease.LeaseTableNotFoundException;
import com.scalar.dl.ledger.namespace.Namespaces;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ScalarLeaseTest {
  private static final String RESOLVED_NAMESPACE = "resolved_namespace";
  private static final String ANY_LEASE_NAME = "transaction_state_purge";
  private static final String ANY_HOLDER = "auditor/uuid-A";
  private static final long ANY_EXPIRY = 1000L;

  @Mock private DistributedStorage storage;
  @Mock private DistributedStorageAdmin storageAdmin;
  @Mock private ScalarNamespaceResolver namespaceResolver;
  private ScalarLease lease;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    // The lease lives in the default (base) namespace; ScalarLease resolves it once at
    // construction.
    when(namespaceResolver.resolve(Namespaces.DEFAULT)).thenReturn(RESOLVED_NAMESPACE);
    lease = new ScalarLease(storage, storageAdmin, namespaceResolver);
  }

  private Result mockResult(String holder, long expiry) {
    Result result = mock(Result.class);
    when(result.getText(ScalarLease.HOLDER)).thenReturn(holder);
    when(result.getBigInt(ScalarLease.EXPIRY)).thenReturn(expiry);
    return result;
  }

  @Test
  public void get_WhenRecordDoesNotExist_ShouldReturnEmpty() throws Exception {
    when(storage.get(any())).thenReturn(Optional.empty());

    assertThat(lease.get(ANY_LEASE_NAME)).isEmpty();
  }

  @Test
  public void get_WhenRecordExists_ShouldReturnLeaseEntry() throws Exception {
    // Build the Result mock before stubbing storage.get to avoid a nested when(...) call.
    Result result = mockResult(ANY_HOLDER, ANY_EXPIRY);
    when(storage.get(any())).thenReturn(Optional.of(result));

    Optional<LeaseEntry> entry = lease.get(ANY_LEASE_NAME);

    assertThat(entry).isPresent();
    assertThat(entry.get().getHolder()).isEqualTo(ANY_HOLDER);
    assertThat(entry.get().getExpiry()).isEqualTo(ANY_EXPIRY);
  }

  @Test
  public void get_WhenTableDoesNotExist_ShouldThrowLeaseTableNotFoundException() throws Exception {
    when(storage.get(any()))
        .thenThrow(
            new IllegalArgumentException(
                CoreError.TABLE_NOT_FOUND.buildCode() + " the table is missing"));

    assertThatThrownBy(() -> lease.get(ANY_LEASE_NAME))
        .isInstanceOf(LeaseTableNotFoundException.class);
  }

  @Test
  public void get_WhenExecutionExceptionThrown_ShouldThrowLeaseException() throws Exception {
    when(storage.get(any())).thenThrow(new ExecutionException("storage down"));

    assertThatThrownBy(() -> lease.get(ANY_LEASE_NAME)).isInstanceOf(LeaseException.class);
  }

  @Test
  public void tryAcquireOrRenew_WhenObservedIsNull_ShouldUsePutIfNotExistsAndReturnTrue()
      throws Exception {
    boolean result = lease.tryAcquireOrRenew(ANY_LEASE_NAME, null, ANY_HOLDER, ANY_EXPIRY);

    assertThat(result).isTrue();
    ArgumentCaptor<Put> captor = ArgumentCaptor.forClass(Put.class);
    verify(storage).put(captor.capture());
    assertThat(captor.getValue().getCondition()).get().isInstanceOf(PutIfNotExists.class);
  }

  @Test
  public void tryAcquireOrRenew_WhenObservedIsPresent_ShouldUsePutIfOnHolderAndExpiry()
      throws Exception {
    LeaseEntry observed = new LeaseEntry(ANY_HOLDER, ANY_EXPIRY);

    boolean result =
        lease.tryAcquireOrRenew(ANY_LEASE_NAME, observed, ANY_HOLDER, ANY_EXPIRY + 3000L);

    assertThat(result).isTrue();
    ArgumentCaptor<Put> captor = ArgumentCaptor.forClass(Put.class);
    verify(storage).put(captor.capture());
    assertThat(captor.getValue().getCondition()).get().isInstanceOf(PutIf.class);
    PutIf condition = (PutIf) captor.getValue().getCondition().get();
    // Conditioned on both the observed holder and the observed expiry (compare-and-swap on expiry
    // prevents resurrecting a lost lease).
    assertThat(condition.getExpressions()).hasSize(2);
  }

  @Test
  public void tryAcquireOrRenew_WhenNoMutationExceptionThrown_ShouldReturnFalse() throws Exception {
    doThrow(new NoMutationException("cas lost", Collections.emptyList()))
        .when(storage)
        .put(any(Put.class));

    boolean result = lease.tryAcquireOrRenew(ANY_LEASE_NAME, null, ANY_HOLDER, ANY_EXPIRY);

    assertThat(result).isFalse();
  }

  @Test
  public void tryAcquireOrRenew_WhenExecutionExceptionThrown_ShouldThrowLeaseException()
      throws Exception {
    doThrow(new ExecutionException("storage down")).when(storage).put(any(Put.class));

    assertThatThrownBy(() -> lease.tryAcquireOrRenew(ANY_LEASE_NAME, null, ANY_HOLDER, ANY_EXPIRY))
        .isInstanceOf(LeaseException.class);
  }

  @Test
  public void createTable_ShouldCreateLeaseTableWithIfNotExists() throws Exception {
    lease.createTable();

    verify(storageAdmin)
        .createTable(eq(RESOLVED_NAMESPACE), eq(ScalarLease.TABLE), any(), eq(true));
  }

  @Test
  public void createTable_WhenExecutionExceptionThrown_ShouldThrowLeaseException()
      throws Exception {
    doThrow(new ExecutionException("cannot create"))
        .when(storageAdmin)
        .createTable(any(), any(), any(), eq(true));

    assertThatThrownBy(() -> lease.createTable()).isInstanceOf(LeaseException.class);
  }

  @Test
  public void get_WhenNonTableNotFoundIllegalArgument_ShouldPropagate() throws Exception {
    when(storage.get(any())).thenThrow(new IllegalArgumentException("some other problem"));

    assertThatThrownBy(() -> lease.get(ANY_LEASE_NAME))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
