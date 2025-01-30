package com.scalar.dl.ledger.database.scalardb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.api.Consistency;
import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.PutIfNotExists;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.api.Scanner;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.exception.storage.NoMutationException;
import com.scalar.db.io.BigIntValue;
import com.scalar.db.io.BlobValue;
import com.scalar.db.io.IntValue;
import com.scalar.db.io.Key;
import com.scalar.db.io.TextValue;
import com.scalar.dl.ledger.contract.ContractEntry;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.MissingContractException;
import com.scalar.dl.ledger.service.StatusCode;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ScalarContractRegistryTest {
  private static final String ANY_ID = "id";
  private static final String ANY_ID2 = "id2";
  private static final String ANY_NAME = "name";
  private static final String ANY_NAME2 = "name2";
  private static final String ANY_ENTITY_ID = "entity_id";
  private static final int ANY_CERT_VERSION = 1;
  private static final byte[] ANY_CONTRACT = "contract".getBytes(StandardCharsets.UTF_8);
  private static final byte[] ANOTHER_CONTRACT = "another".getBytes(StandardCharsets.UTF_8);
  private static final String ANY_PROPERTIES = "properties";
  private static final long ANY_REGISTERED_AT = 1L;
  private static final byte[] ANY_SIGNATURE = "signature".getBytes(StandardCharsets.UTF_8);
  @Mock private DistributedStorage storage;
  private ScalarContractRegistry registry;
  private ContractEntry entry = null;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    registry = new ScalarContractRegistry(storage);
    entry = prepareContractEntry(ANY_ID, ANY_NAME);
  }

  private Result prepareResultForContract(ContractEntry entry) {
    Result result = mock(Result.class);
    when(result.getValue(ContractEntry.ID))
        .thenReturn(Optional.of(new TextValue(ContractEntry.ID, entry.getId())));
    when(result.getValue(ContractEntry.BINARY_NAME))
        .thenReturn(Optional.of(new TextValue(ContractEntry.BINARY_NAME, entry.getBinaryName())));
    when(result.getValue(ContractEntry.ENTITY_ID))
        .thenReturn(Optional.of(new TextValue(ContractEntry.ENTITY_ID, entry.getEntityId())));
    when(result.getValue(ContractEntry.KEY_VERSION))
        .thenReturn(Optional.of(new IntValue(ContractEntry.KEY_VERSION, entry.getKeyVersion())));
    when(result.getValue(ContractEntry.PROPERTIES))
        .thenReturn(
            Optional.of(
                new TextValue(ContractEntry.PROPERTIES, entry.getProperties().orElse(null))));
    when(result.getValue(ContractEntry.REGISTERED_AT))
        .thenReturn(
            Optional.of(new BigIntValue(ContractEntry.REGISTERED_AT, entry.getRegisteredAt())));
    when(result.getValue(ContractEntry.SIGNATURE))
        .thenReturn(Optional.of(new BlobValue(ContractEntry.SIGNATURE, entry.getSignature())));
    return result;
  }

  private Result prepareResultForClass(ContractEntry entry) {
    Result result = mock(Result.class);
    when(result.getValue(ContractEntry.BINARY_NAME))
        .thenReturn(Optional.of(new TextValue(ContractEntry.BINARY_NAME, entry.getBinaryName())));
    when(result.getValue(ContractEntry.BYTE_CODE))
        .thenReturn(Optional.of(new BlobValue(ContractEntry.BYTE_CODE, entry.getByteCode())));
    return result;
  }

  private ContractEntry prepareContractEntry(String contractId, String contractBinaryName) {
    return new ContractEntry(
        contractId,
        contractBinaryName,
        ANY_ENTITY_ID,
        ANY_CERT_VERSION,
        ANY_CONTRACT,
        ANY_PROPERTIES,
        ANY_REGISTERED_AT,
        ANY_SIGNATURE);
  }

  private void prepareScanner(Result result) throws ExecutionException {
    Scanner scanner = mock(Scanner.class);
    when(storage.scan(any(Scan.class))).thenReturn(scanner);
    when(scanner.spliterator()).thenReturn(Collections.singletonList(result).spliterator());

    ContractEntry entry1 = mock(ContractEntry.class);
    when(entry1.getId()).thenReturn(entry.getId());
  }

  private Get prepareGetForContract(ContractEntry entry) {
    return new Get(
            new Key(ContractEntry.ENTITY_ID, entry.getEntityId()),
            new Key(
                new IntValue(ContractEntry.KEY_VERSION, entry.getKeyVersion()),
                new TextValue(ContractEntry.ID, entry.getId())))
        .withConsistency(Consistency.SEQUENTIAL)
        .forTable(ScalarContractRegistry.CONTRACT_TABLE);
  }

  private Get prepareGetForContractClass(ContractEntry entry) {
    return new Get(new Key(ContractEntry.BINARY_NAME, entry.getBinaryName()))
        .withConsistency(Consistency.LINEARIZABLE)
        .forTable(ScalarContractRegistry.CONTRACT_CLASS_TABLE);
  }

  @Test
  public void bind_ContractEntryGiven_ShouldBindProperly() throws ExecutionException {
    // Arrange
    doReturn(Optional.empty()).when(storage).get(any(Get.class));
    doNothing().when(storage).put(any(Put.class));

    // Act Assert
    assertThatCode(() -> registry.bind(entry)).doesNotThrowAnyException();

    // Assert
    Put expected1 =
        new Put(new Key(ContractEntry.BINARY_NAME, entry.getBinaryName()))
            .withValue(ContractEntry.BYTE_CODE, entry.getByteCode())
            .withCondition(new PutIfNotExists())
            .withConsistency(Consistency.LINEARIZABLE)
            .forTable(ScalarContractRegistry.CONTRACT_CLASS_TABLE);

    Put expected2 =
        new Put(
                new Key(ContractEntry.ENTITY_ID, entry.getEntityId()),
                new Key(
                    new IntValue(ContractEntry.KEY_VERSION, entry.getKeyVersion()),
                    new TextValue(ContractEntry.ID, entry.getId())))
            .withValue(ContractEntry.BINARY_NAME, entry.getBinaryName())
            .withValue(ContractEntry.PROPERTIES, entry.getProperties().get().toString())
            .withValue(ContractEntry.REGISTERED_AT, entry.getRegisteredAt())
            .withValue(ContractEntry.SIGNATURE, ANY_SIGNATURE)
            .withConsistency(Consistency.SEQUENTIAL)
            .forTable(ScalarContractRegistry.CONTRACT_TABLE);

    verify(storage).put(expected1);
    verify(storage).put(expected2);
  }

  @Test
  public void bind_ContractEntryGivenAndFirstStorageOperationFailed_ShouldThrowDatabaseException()
      throws ExecutionException {
    // Arrange
    doReturn(Optional.empty()).when(storage).get(any(Get.class));
    ExecutionException toThrow = mock(ExecutionException.class);
    doThrow(toThrow).when(storage).put(any(Put.class));

    // Act Assert
    assertThatThrownBy(() -> registry.bind(entry))
        .isInstanceOf(DatabaseException.class)
        .hasCause(toThrow);
  }

  @Test
  public void bind_ContractEntryGivenAndSecondStorageOperationFailed_ShouldThrowDatabaseException()
      throws ExecutionException {
    // Arrange
    doReturn(Optional.empty()).when(storage).get(any(Get.class));
    ExecutionException toThrow = mock(ExecutionException.class);
    doNothing().doThrow(toThrow).when(storage).put(any(Put.class));

    // Act Assert
    assertThatThrownBy(() -> registry.bind(entry))
        .isInstanceOf(DatabaseException.class)
        .hasCause(toThrow);
  }

  @Test
  public void bind_ContractEntryGivenAndClassAlreadyRegistered_ShouldBindProperly()
      throws ExecutionException {
    // Arrange
    Result result = mock(Result.class);
    when(result.getValue(ContractEntry.BYTE_CODE))
        .thenReturn(Optional.of(new BlobValue(ANY_CONTRACT)));
    doReturn(Optional.of(result)).when(storage).get(any(Get.class));
    NoMutationException toThrow = mock(NoMutationException.class);
    doThrow(toThrow).doNothing().when(storage).put(any(Put.class));

    // Act
    registry.bind(entry);

    // Assert
    verify(storage, times(2)).put(any(Put.class));
  }

  @Test
  public void
      bind_ContractEntryGivenAndDifferentByteCodeAlreadyRegistered_ShouldThrowDatabaseException()
          throws ExecutionException {
    // Arrange
    Result result = mock(Result.class);
    when(result.getValue(ContractEntry.BYTE_CODE))
        .thenReturn(Optional.of(new BlobValue(ANOTHER_CONTRACT)));
    doReturn(Optional.of(result)).when(storage).get(any(Get.class));

    // Act
    Throwable thrown = catchThrowable(() -> registry.bind(entry));

    // Assert
    verify(storage, never()).put(any(Put.class));
    assertThat(thrown).isInstanceOf(DatabaseException.class);
    assertThat(((DatabaseException) thrown).getCode())
        .isEqualTo(StatusCode.CONTRACT_ALREADY_REGISTERED);
  }

  @Test
  public void lookup_ContractKeyGiven_ShouldLookupProperly() throws ExecutionException {
    // Arrange
    Result resultForContract = prepareResultForContract(entry);
    Result resultForClass = prepareResultForClass(entry);
    when(storage.get(any(Get.class)))
        .thenReturn(Optional.of(resultForContract))
        .thenReturn(Optional.of(resultForClass));

    // Act Assert
    ContractEntry actual = registry.lookup(entry.getKey());

    // Assert
    Get expected1 = prepareGetForContract(entry);
    Get expected2 = prepareGetForContractClass(entry);
    verify(storage).get(expected1);
    verify(storage).get(expected2);
    verify(storage, times(2)).get(any(Get.class));
    assertThat(actual).isEqualTo(entry);
  }

  @Test
  public void lookup_SameContractKeyGivenTwice_ShouldLookupOnceForEach() throws ExecutionException {
    // Arrange
    Result resultForContract = prepareResultForContract(entry);
    Result resultForClass = prepareResultForClass(entry);
    when(storage.get(any(Get.class)))
        .thenReturn(Optional.of(resultForContract))
        .thenReturn(Optional.of(resultForClass));

    // Act Assert
    ContractEntry actual1 = registry.lookup(entry.getKey());
    ContractEntry actual2 = registry.lookup(entry.getKey());

    // Assert
    Get expected1 = prepareGetForContract(entry);
    Get expected2 = prepareGetForContractClass(entry);
    verify(storage).get(expected1);
    verify(storage).get(expected2);
    verify(storage, times(2)).get(any(Get.class));
    assertThat(actual1).isEqualTo(entry);
    assertThat(actual2).isEqualTo(entry);
  }

  @Test
  public void lookup_DifferentContractKeysGiven_ShouldLookupTwiceForEach()
      throws ExecutionException {
    // Arrange
    ContractEntry entry2 = prepareContractEntry(ANY_ID2, ANY_NAME2);
    Result resultForContract = prepareResultForContract(entry);
    Result resultForClass = prepareResultForClass(entry);
    Result resultForContract2 = prepareResultForContract(entry2);
    Result resultForClass2 = prepareResultForClass(entry2);
    when(storage.get(any(Get.class)))
        .thenReturn(Optional.of(resultForContract))
        .thenReturn(Optional.of(resultForClass))
        .thenReturn(Optional.of(resultForContract2))
        .thenReturn(Optional.of(resultForClass2));

    // Act Assert
    ContractEntry actual1 = registry.lookup(entry.getKey());
    ContractEntry actual2 = registry.lookup(entry2.getKey());

    // Assert
    Get expected1_1 = prepareGetForContract(entry);
    Get expected1_2 = prepareGetForContractClass(entry);
    Get expected2_1 = prepareGetForContract(entry2);
    Get expected2_2 = prepareGetForContractClass(entry2);
    verify(storage).get(expected1_1);
    verify(storage).get(expected1_2);
    verify(storage).get(expected2_1);
    verify(storage).get(expected2_2);
    verify(storage, times(4)).get(any(Get.class));
    assertThat(actual1).isEqualTo(entry);
    assertThat(actual2).isEqualTo(entry2);
  }

  @Test
  public void lookup_SameContractButDifferentIdsGiven_ShouldLookupOneTwiceAnotherOnce()
      throws ExecutionException {
    // Arrange
    ContractEntry entry2 = prepareContractEntry(ANY_ID2, ANY_NAME);
    Result resultForContract = prepareResultForContract(entry);
    Result resultForClass = prepareResultForClass(entry);
    Result resultForContract2 = prepareResultForContract(entry2);
    Result resultForClass2 = prepareResultForClass(entry2);
    when(storage.get(any(Get.class)))
        .thenReturn(Optional.of(resultForContract))
        .thenReturn(Optional.of(resultForClass))
        .thenReturn(Optional.of(resultForContract2))
        .thenReturn(Optional.of(resultForClass2));

    // Act Assert
    ContractEntry actual1 = registry.lookup(entry.getKey());
    ContractEntry actual2 = registry.lookup(entry2.getKey());

    // Assert
    Get expected1_1 = prepareGetForContract(entry);
    Get expected1_2 = prepareGetForContractClass(entry);
    Get expected2_1 = prepareGetForContract(entry2);
    verify(storage).get(expected1_1);
    verify(storage).get(expected1_2);
    verify(storage).get(expected2_1);
    verify(storage, times(3)).get(any(Get.class));
    assertThat(actual1).isEqualTo(entry);
    assertThat(actual2).isEqualTo(entry2);
  }

  @Test
  public void lookup_ContractEntryMissing_ShouldThrowMissingContractException()
      throws ExecutionException {
    // Arrange
    when(storage.get(any(Get.class))).thenReturn(Optional.empty());

    // Act Assert
    assertThatThrownBy(() -> registry.lookup(entry.getKey()))
        .isInstanceOf(MissingContractException.class);
  }

  @Test
  public void lookup_FirstStorageOperationFailed_ShouldThrowDatabaseException()
      throws ExecutionException {
    // Arrange
    ExecutionException toThrow = mock(ExecutionException.class);
    doThrow(toThrow).when(storage).get(any(Get.class));

    // Act Assert
    assertThatThrownBy(() -> registry.lookup(entry.getKey()))
        .isInstanceOf(DatabaseException.class)
        .hasCause(toThrow);
  }

  @Test
  public void lookup_SecondStorageOperationFailed_ShouldThrowDatabaseException()
      throws ExecutionException {
    // Arrange
    Result resultForContract = prepareResultForContract(entry);
    ExecutionException toThrow = mock(ExecutionException.class);
    doReturn(Optional.of(resultForContract)).doThrow(toThrow).when(storage).get(any(Get.class));

    // Act Assert
    assertThatThrownBy(() -> registry.lookup(entry.getKey()))
        .isInstanceOf(DatabaseException.class)
        .hasCause(toThrow);
  }

  @Test
  public void scan_OnlyCertIdGiven_ShouldReturnAllContractsWithCertId() throws ExecutionException {
    // Arrange
    Result resultForContract = prepareResultForContract(entry);
    Result resultForClass = prepareResultForClass(entry);
    prepareScanner(resultForContract);
    when(storage.get(any(Get.class))).thenReturn(Optional.of(resultForClass));

    // Act Assert
    List<ContractEntry> actual = registry.scan(entry.getEntityId());

    // Assert
    assertThat(actual).containsOnly(entry);
    Scan scan =
        new Scan(new Key(ContractEntry.ENTITY_ID, entry.getEntityId()))
            .withConsistency(Consistency.SEQUENTIAL)
            .forTable(ScalarContractRegistry.CONTRACT_TABLE);
    verify(storage).scan(scan);
  }

  @Test
  public void scan_CertIdAndVersionGiven_ShouldReturnContractsWithVersion()
      throws ExecutionException {
    // Arrange
    Result resultForContract = prepareResultForContract(entry);
    Result resultForClass = prepareResultForClass(entry);
    prepareScanner(resultForContract);
    when(storage.get(any(Get.class))).thenReturn(Optional.of(resultForClass));

    // Act Assert
    List<ContractEntry> actual = registry.scan(entry.getEntityId(), entry.getKeyVersion());

    // Assert
    assertThat(actual).containsOnly(entry);
    Scan scan =
        new Scan(new Key(ContractEntry.ENTITY_ID, entry.getEntityId()))
            .withStart(new Key(ContractEntry.KEY_VERSION, entry.getKeyVersion()))
            .withEnd(new Key(ContractEntry.KEY_VERSION, entry.getKeyVersion()))
            .withConsistency(Consistency.SEQUENTIAL)
            .forTable(ScalarContractRegistry.CONTRACT_TABLE);
    verify(storage).scan(scan);
  }
}
