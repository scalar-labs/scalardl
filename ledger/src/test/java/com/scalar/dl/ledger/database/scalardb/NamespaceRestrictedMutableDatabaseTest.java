package com.scalar.dl.ledger.database.scalardb;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.scalar.db.api.Delete;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.io.Key;
import com.scalar.dl.ledger.database.MutableDatabase;
import com.scalar.dl.ledger.error.LedgerError;
import com.scalar.dl.ledger.exception.InvalidFunctionException;
import com.scalar.dl.ledger.namespace.Namespaces;
import com.scalar.dl.ledger.service.StatusCode;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class NamespaceRestrictedMutableDatabaseTest {
  private static final String CONTEXT_NAMESPACE = "context_ns";
  private static final String DIFFERENT_NAMESPACE = "different_ns";
  private static final String TABLE = "table";
  private static final Key PARTITION_KEY = Key.ofText("col", "val");

  private enum Op {
    GET,
    SCAN,
    PUT,
    DELETE
  }

  @Mock private MutableDatabase<Get, Scan, Put, Delete, Result> delegate;

  // contextNamespace == DEFAULT: only the disallowed (system) namespaces are blocked.
  private NamespaceRestrictedMutableDatabase defaultDatabase;
  // contextNamespace != DEFAULT: only the same-named namespace is accessible.
  private NamespaceRestrictedMutableDatabase restrictedDatabase;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    defaultDatabase = new NamespaceRestrictedMutableDatabase(delegate, Namespaces.DEFAULT);
    restrictedDatabase = new NamespaceRestrictedMutableDatabase(delegate, CONTEXT_NAMESPACE);
  }

  static Stream<String> disallowedNamespaces() {
    return Stream.of(
        "system",
        "system_schema",
        "system_auth",
        "system_distributed",
        "system_traces",
        "coordinator",
        "scalar",
        "scalar_my_namespace",
        "auditor",
        "auditor_my_namespace",
        // Case insensitive
        "SYSTEM",
        "Coordinator",
        "SCALAR",
        "Scalar_Namespace",
        "AUDITOR",
        "Auditor_Namespace");
  }

  static Stream<Arguments> operationsAndDisallowedNamespaces() {
    return disallowedNamespaces()
        .flatMap(ns -> Arrays.stream(Op.values()).map(op -> Arguments.of(op, ns)));
  }

  static Stream<Arguments> operationsAndReservedContextNamespaces() {
    return Stream.of("coordinator", "system", "scalar_secret", "auditor_log")
        .flatMap(ns -> Arrays.stream(Op.values()).map(op -> Arguments.of(op, ns)));
  }

  // ---- DEFAULT context: backward-compatible behavior (only system namespaces blocked) ----

  @ParameterizedTest
  @MethodSource("operationsAndDisallowedNamespaces")
  public void operation_DefaultContextWithDisallowedNamespace_ShouldThrowInvalidFunctionException(
      Op op, String namespace) {
    assertThatThrownBy(() -> run(defaultDatabase, op, namespace))
        .isInstanceOf(InvalidFunctionException.class);
  }

  @ParameterizedTest
  @EnumSource(Op.class)
  public void operation_DefaultContextWithAllowedNamespace_ShouldDelegate(Op op) {
    assertThatCode(() -> run(defaultDatabase, op, "my_namespace")).doesNotThrowAnyException();
    verifyDelegated(op, "my_namespace");
  }

  @ParameterizedTest
  @EnumSource(Op.class)
  public void operation_DefaultContextWithoutNamespace_ShouldDelegate(Op op) {
    assertThatCode(() -> run(defaultDatabase, op, null)).doesNotThrowAnyException();
    verifyDelegated(op, null);
  }

  // ---- Non-default context: only the same-named namespace is accessible ----

  @ParameterizedTest
  @EnumSource(Op.class)
  public void operation_NonDefaultContextWithSameNamespace_ShouldDelegate(Op op) {
    assertThatCode(() -> run(restrictedDatabase, op, CONTEXT_NAMESPACE)).doesNotThrowAnyException();
    verifyDelegated(op, CONTEXT_NAMESPACE);
  }

  @ParameterizedTest
  @EnumSource(Op.class)
  public void operation_NonDefaultContextWithDifferentNamespace_ShouldThrow(Op op) {
    assertThatThrownBy(() -> run(restrictedDatabase, op, DIFFERENT_NAMESPACE))
        .isInstanceOf(InvalidFunctionException.class)
        .hasMessage(
            LedgerError.FUNCTION_IS_NOT_ALLOWED_TO_ACCESS_DIFFERENT_NAMESPACE.buildMessage(
                DIFFERENT_NAMESPACE, CONTEXT_NAMESPACE))
        .extracting("code")
        .isEqualTo(StatusCode.INVALID_FUNCTION);
  }

  @ParameterizedTest
  @EnumSource(Op.class)
  public void operation_NonDefaultContextWithoutNamespace_ShouldThrow(Op op) {
    assertThatThrownBy(() -> run(restrictedDatabase, op, null))
        .isInstanceOf(InvalidFunctionException.class)
        .hasMessage(
            LedgerError.FUNCTION_IS_NOT_ALLOWED_TO_ACCESS_DIFFERENT_NAMESPACE.buildMessage(
                (Object) null, CONTEXT_NAMESPACE))
        .extracting("code")
        .isEqualTo(StatusCode.INVALID_FUNCTION);
  }

  @ParameterizedTest
  @EnumSource(Op.class)
  public void operation_NonDefaultContextWithSystemNamespace_ShouldThrow(Op op) {
    // A system namespace is always rejected by the deny list, which is applied regardless of the
    // context namespace.
    assertThatThrownBy(() -> run(restrictedDatabase, op, "coordinator"))
        .isInstanceOf(InvalidFunctionException.class)
        .hasMessage(
            LedgerError.FUNCTION_IS_NOT_ALLOWED_TO_ACCESS_SPECIFIED_NAMESPACE.buildMessage());
  }

  @ParameterizedTest
  @MethodSource("operationsAndReservedContextNamespaces")
  public void operation_ReservedContextNamespaceTargetingItself_ShouldThrow(
      Op op, String reservedNamespace) {
    // Even when the context namespace itself is a reserved name, accessing that reserved ScalarDB
    // namespace must be rejected; the deny list is always applied.
    NamespaceRestrictedMutableDatabase database =
        new NamespaceRestrictedMutableDatabase(delegate, reservedNamespace);
    assertThatThrownBy(() -> run(database, op, reservedNamespace))
        .isInstanceOf(InvalidFunctionException.class)
        .hasMessage(
            LedgerError.FUNCTION_IS_NOT_ALLOWED_TO_ACCESS_SPECIFIED_NAMESPACE.buildMessage());
  }

  @ParameterizedTest
  @EnumSource(Op.class)
  public void operation_NonDefaultContextWithDifferentCase_ShouldThrow(Op op) {
    // The same-name check is case-sensitive, mirroring NamespaceRestrictedAssetLedger.
    String differentCase = CONTEXT_NAMESPACE.toUpperCase();
    assertThatThrownBy(() -> run(restrictedDatabase, op, differentCase))
        .isInstanceOf(InvalidFunctionException.class)
        .hasMessage(
            LedgerError.FUNCTION_IS_NOT_ALLOWED_TO_ACCESS_DIFFERENT_NAMESPACE.buildMessage(
                differentCase, CONTEXT_NAMESPACE));
  }

  // ---- helpers ----

  private void run(NamespaceRestrictedMutableDatabase database, Op op, String namespace) {
    switch (op) {
      case GET:
        database.get(get(namespace));
        return;
      case SCAN:
        database.scan(scan(namespace));
        return;
      case PUT:
        database.put(put(namespace));
        return;
      case DELETE:
        database.delete(delete(namespace));
        return;
      default:
        throw new AssertionError("unexpected op: " + op);
    }
  }

  private void verifyDelegated(Op op, String namespace) {
    switch (op) {
      case GET:
        verify(delegate).get(get(namespace));
        return;
      case SCAN:
        verify(delegate).scan(scan(namespace));
        return;
      case PUT:
        verify(delegate).put(put(namespace));
        return;
      case DELETE:
        verify(delegate).delete(delete(namespace));
        return;
      default:
        throw new AssertionError("unexpected op: " + op);
    }
  }

  private Get get(String namespace) {
    return namespace == null
        ? Get.newBuilder().table(TABLE).partitionKey(PARTITION_KEY).build()
        : Get.newBuilder().namespace(namespace).table(TABLE).partitionKey(PARTITION_KEY).build();
  }

  private Scan scan(String namespace) {
    return namespace == null
        ? Scan.newBuilder().table(TABLE).partitionKey(PARTITION_KEY).build()
        : Scan.newBuilder().namespace(namespace).table(TABLE).partitionKey(PARTITION_KEY).build();
  }

  private Put put(String namespace) {
    return namespace == null
        ? Put.newBuilder().table(TABLE).partitionKey(PARTITION_KEY).build()
        : Put.newBuilder().namespace(namespace).table(TABLE).partitionKey(PARTITION_KEY).build();
  }

  private Delete delete(String namespace) {
    return namespace == null
        ? Delete.newBuilder().table(TABLE).partitionKey(PARTITION_KEY).build()
        : Delete.newBuilder().namespace(namespace).table(TABLE).partitionKey(PARTITION_KEY).build();
  }
}
