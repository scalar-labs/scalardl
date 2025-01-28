package com.scalar.dl.ledger.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.google.common.collect.ImmutableMap;
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.database.Database;
import com.scalar.dl.ledger.database.Transaction;
import com.scalar.dl.ledger.database.TransactionManager;
import com.scalar.dl.ledger.exception.ConflictException;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.function.FunctionMachine;
import com.scalar.dl.ledger.function.FunctionManager;
import com.scalar.dl.ledger.model.ContractExecutionRequest;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.proof.AssetProof;
import com.scalar.dl.ledger.statemachine.DeprecatedLedger;
import com.scalar.dl.ledger.statemachine.DeserializationType;
import com.scalar.dl.ledger.statemachine.JsonpBasedAssetLedger;
import com.scalar.dl.ledger.statemachine.Ledger;
import com.scalar.dl.ledger.statemachine.StringBasedAssetLedger;
import com.scalar.dl.ledger.util.Argument;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class ContractExecutorTest {
  private static final String ANY_ENTITY_ID = "entity_id";
  private static final int ANY_CERT_VERSION = 1;
  private static final String ANY_CONTRACT_ARGUMENT = "contract_argument";
  private static final String ANY_CONTRACT_ARGUMENT_V2 =
      Argument.format("contract_argument", "nonce", Collections.emptyList());
  private static final String ANY_CONTRACT_RESULT = "contract_result";
  private static final String ANY_FUNCTION_ID = "function_id";
  private static final String ANY_FUNCTION_ARGUMENT = "function_argument";
  private static final String ANY_FUNCTION_RESULT1 = "function_result1";
  private static final String ANY_FUNCTION_RESULT2 = "function_result2";
  private static final String ANY_ASSET_ID = "asset_id";
  private static final int ANY_ASSET_AGE = 1;
  @Mock private LedgerConfig config;
  @Mock private TransactionManager transactionManager;
  @Mock private ContractManager contractManager;
  @Mock private FunctionManager functionManager;
  @InjectMocks private ContractExecutor executor;
  @Mock private ContractMachine contract;
  @Mock private ContractExecutionRequest request;
  @Mock private FunctionMachine function;
  @Mock private DeprecatedLedger deprecatedLedger;
  @Mock private JsonpBasedAssetLedger jsonpBasedAssetLedger;
  @Mock private StringBasedAssetLedger stringBasedAssetLedger;
  @Mock private Database database;
  @Mock private Transaction transaction;
  private AutoCloseable closeable;

  @BeforeEach
  public void setUp() {
    closeable = openMocks(this);
  }

  @AfterEach
  public void tearDown() throws Exception {
    closeable.close();
  }

  private void configureBehaviors(int contractArgumentVersion, boolean useFunction) {
    when(request.getEntityId()).thenReturn(ANY_ENTITY_ID);
    when(request.getKeyVersion()).thenReturn(ANY_CERT_VERSION);
    if (contractArgumentVersion == 1) {
      when(request.getContractArgument()).thenReturn(ANY_CONTRACT_ARGUMENT);
    } else if (contractArgumentVersion == 2) {
      when(request.getContractArgument()).thenReturn(ANY_CONTRACT_ARGUMENT_V2);
    } else {
      throw new IllegalArgumentException("unsupported version");
    }
    if (useFunction) {
      when(request.getFunctionIds()).thenReturn(Arrays.asList(ANY_FUNCTION_ID, ANY_FUNCTION_ID));
      when(request.getFunctionArgument()).thenReturn(Optional.of(ANY_FUNCTION_ARGUMENT));
    }
    ContractEntry entry = mock(ContractEntry.class);
    when(contractManager.get(any())).thenReturn(entry);
    when(contractManager.getInstance(entry)).thenReturn(contract);
    when(entry.getProperties()).thenReturn(Optional.empty());
    when(functionManager.getInstance(ANY_FUNCTION_ID)).thenReturn(function);
    when(transactionManager.startWith(request)).thenReturn(transaction);
    doReturn(deprecatedLedger).when(transaction).getLedger(DeserializationType.DEPRECATED);
    doReturn(jsonpBasedAssetLedger).when(transaction).getLedger(DeserializationType.JSONP_JSON);
    doReturn(stringBasedAssetLedger).when(transaction).getLedger(DeserializationType.STRING);
    when(transaction.getDatabase()).thenReturn(database);
  }

  @Test
  public void execute_ContractWithV1ArgumentFormatGiven_ShouldInvokeContract() {
    // Arrange
    configureBehaviors(1, false);
    when(contract.invoke(
            ArgumentMatchers.<Ledger<JsonObject>>any(), anyString(), nullable(String.class)))
        .thenReturn(ANY_CONTRACT_RESULT);
    when(contract.getDeserializationType()).thenReturn(DeserializationType.DEPRECATED);
    when(config.isFunctionEnabled()).thenReturn(false);
    List<AssetProof> proofs = new ArrayList<>();
    when(transaction.commit()).thenReturn(proofs);

    // Act
    ContractExecutionResult result = executor.execute(request);

    // Assert
    verify(contract).invoke(deprecatedLedger, ANY_CONTRACT_ARGUMENT, null);
    verify(function, never())
        .invoke(any(Database.class), nullable(String.class), anyString(), nullable(String.class));
    verify(transaction).commit();
    verify(transaction, never()).abort();
    assertThat(result.getContractResult().orElse(null)).isEqualTo(ANY_CONTRACT_RESULT);
    assertThat(result.getFunctionResult()).isEmpty();
    assertThat(result.getLedgerProofs()).isEqualTo(proofs);
    assertThat(result.getAuditorProofs()).isEmpty();
  }

  @Test
  public void execute_ContractWithV2ArgumentFormatGiven_ShouldInvokeContract() {
    // Arrange
    configureBehaviors(2, false);
    when(contract.invoke(
            ArgumentMatchers.<Ledger<JsonObject>>any(), anyString(), nullable(String.class)))
        .thenReturn(ANY_CONTRACT_RESULT);
    when(contract.getDeserializationType()).thenReturn(DeserializationType.JSONP_JSON);
    when(config.isFunctionEnabled()).thenReturn(false);
    List<AssetProof> proofs = new ArrayList<>();
    when(transaction.commit()).thenReturn(proofs);

    // Act
    ContractExecutionResult result = executor.execute(request);

    // Assert
    verify(contract).invoke(jsonpBasedAssetLedger, ANY_CONTRACT_ARGUMENT, null);
    verify(function, never())
        .invoke(any(Database.class), nullable(String.class), anyString(), nullable(String.class));
    verify(transaction).commit();
    verify(transaction, never()).abort();
    assertThat(result.getContractResult().orElse(null)).isEqualTo(ANY_CONTRACT_RESULT);
    assertThat(result.getFunctionResult()).isEmpty();
    assertThat(result.getLedgerProofs()).isEqualTo(proofs);
    assertThat(result.getAuditorProofs()).isEmpty();
  }

  @Test
  public void
      execute_ContractWithV1ArgumentFormatAndFunctionGiven_ShouldInvokeContractAndFunction() {
    // Arrange
    configureBehaviors(1, true);
    when(contract.invoke(
            ArgumentMatchers.<Ledger<String>>any(), anyString(), nullable(String.class)))
        .thenReturn(ANY_CONTRACT_RESULT);
    when(contract.getDeserializationType()).thenReturn(DeserializationType.DEPRECATED);
    when(function.invoke(
            any(Database.class), nullable(String.class), anyString(), nullable(String.class)))
        .thenReturn(ANY_FUNCTION_RESULT1)
        .thenReturn(ANY_FUNCTION_RESULT2);
    when(config.isFunctionEnabled()).thenReturn(true);
    List<AssetProof> proofs = new ArrayList<>();
    when(transaction.commit()).thenReturn(proofs);

    // Act
    ContractExecutionResult result = executor.execute(request);

    // Assert
    verify(contract).invoke(deprecatedLedger, ANY_CONTRACT_ARGUMENT, null);
    verify(function, times(2)).setContractContext(any());
    verify(function, times(2)).invoke(database, ANY_FUNCTION_ARGUMENT, ANY_CONTRACT_ARGUMENT, null);
    verify(transaction).commit();
    verify(transaction, never()).abort();
    assertThat(result.getContractResult().orElse(null)).isEqualTo(ANY_CONTRACT_RESULT);
    assertThat(result.getFunctionResult().orElse(null)).isEqualTo(ANY_FUNCTION_RESULT2);
    assertThat(result.getLedgerProofs()).isEqualTo(proofs);
    assertThat(result.getAuditorProofs()).isEmpty();
  }

  @Test
  public void
      execute_ContractWithV2ArgumentFormatAndFunctionGiven_ShouldInvokeContractAndFunction() {
    // Arrange
    configureBehaviors(2, true);
    when(contract.invoke(
            ArgumentMatchers.<Ledger<JsonObject>>any(), anyString(), nullable(String.class)))
        .thenReturn(ANY_CONTRACT_RESULT);
    when(contract.getDeserializationType()).thenReturn(DeserializationType.JSONP_JSON);
    when(function.invoke(
            any(Database.class), nullable(String.class), anyString(), nullable(String.class)))
        .thenReturn(ANY_FUNCTION_RESULT1)
        .thenReturn(ANY_FUNCTION_RESULT2);
    when(config.isFunctionEnabled()).thenReturn(true);
    List<AssetProof> proofs = new ArrayList<>();
    when(transaction.commit()).thenReturn(proofs);

    // Act
    ContractExecutionResult result = executor.execute(request);

    // Assert
    verify(contract).invoke(jsonpBasedAssetLedger, ANY_CONTRACT_ARGUMENT, null);
    verify(function, times(2)).setContractContext(any());
    verify(function, times(2)).invoke(database, ANY_FUNCTION_ARGUMENT, ANY_CONTRACT_ARGUMENT, null);
    verify(transaction).commit();
    verify(transaction, never()).abort();
    assertThat(result.getContractResult().orElse(null)).isEqualTo(ANY_CONTRACT_RESULT);
    assertThat(result.getFunctionResult().orElse(null)).isEqualTo(ANY_FUNCTION_RESULT2);
    assertThat(result.getLedgerProofs()).isEqualTo(proofs);
    assertThat(result.getAuditorProofs()).isEmpty();
  }

  @Test
  public void execute_ContractContextExceptionThrown_ShouldThrowExceptionWithoutRecovery() {
    // Arrange
    configureBehaviors(1, false);
    ContractContextException toThrow = mock(ContractContextException.class);
    doThrow(toThrow)
        .when(contract)
        .invoke(ArgumentMatchers.<Ledger<JsonObject>>any(), anyString(), nullable(String.class));
    when(contract.getDeserializationType()).thenReturn(DeserializationType.DEPRECATED);
    when(config.isFunctionEnabled()).thenReturn(false);
    doNothing().when(transaction).abort();

    // Act
    Throwable thrown = catchThrowable(() -> executor.execute(request));

    // Assert
    verify(contract).invoke(deprecatedLedger, ANY_CONTRACT_ARGUMENT, null);
    verify(transaction, never()).commit();
    verify(transaction).abort();
    verify(function, never())
        .invoke(any(Database.class), nullable(String.class), anyString(), nullable(String.class));
    assertThat(thrown).isEqualTo(toThrow);
  }

  @Test
  public void execute_ConflictExceptionThrown_ShouldThrowExceptionWithRecovery() {
    // Arrange
    configureBehaviors(1, false);
    when(contract.invoke(
            ArgumentMatchers.<Ledger<JsonObject>>any(), anyString(), nullable(String.class)))
        .thenReturn(ANY_CONTRACT_RESULT);
    when(contract.getDeserializationType()).thenReturn(DeserializationType.DEPRECATED);
    when(config.isFunctionEnabled()).thenReturn(false);
    Map<String, Integer> expectedMap = ImmutableMap.of(ANY_ASSET_ID, ANY_ASSET_AGE);
    ConflictException toThrow = mock(ConflictException.class);
    when(toThrow.getIds()).thenReturn(expectedMap);
    when(transaction.commit()).thenThrow(toThrow);
    doNothing().when(transaction).abort();

    // Act
    Throwable thrown = catchThrowable(() -> executor.execute(request));

    // Assert
    verify(contract).invoke(deprecatedLedger, ANY_CONTRACT_ARGUMENT, null);
    verify(transaction).commit();
    verify(transaction).abort();
    verify(transactionManager).recover(expectedMap);
    verify(function, never())
        .invoke(any(Database.class), nullable(String.class), anyString(), nullable(String.class));
    assertThat(thrown).isEqualTo(toThrow);
  }
}
