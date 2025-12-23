package com.scalar.dl.tablestore.client.service;

import static com.scalar.dl.genericcontracts.table.Constants.ASSET_ID_SEPARATOR;
import static com.scalar.dl.genericcontracts.table.Constants.CONTRACT_CREATE;
import static com.scalar.dl.genericcontracts.table.Constants.CONTRACT_GET_ASSET_ID;
import static com.scalar.dl.genericcontracts.table.Constants.CONTRACT_GET_HISTORY;
import static com.scalar.dl.genericcontracts.table.Constants.CONTRACT_INSERT;
import static com.scalar.dl.genericcontracts.table.Constants.CONTRACT_SCAN;
import static com.scalar.dl.genericcontracts.table.Constants.CONTRACT_SELECT;
import static com.scalar.dl.genericcontracts.table.Constants.CONTRACT_SHOW_TABLES;
import static com.scalar.dl.genericcontracts.table.Constants.CONTRACT_UPDATE;
import static com.scalar.dl.genericcontracts.table.Constants.PREFIX_INDEX;
import static com.scalar.dl.genericcontracts.table.Constants.PREFIX_RECORD;
import static com.scalar.dl.genericcontracts.table.Constants.PREFIX_TABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.ExecutionResult;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.service.StatusCode;
import java.math.BigDecimal;
import javax.json.Json;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TableStoreClientServiceTest {
  private static final String ANY_TABLE = "tbl";
  private static final String ANY_COLUMN = "col";
  private static final int ANY_START_AGE = 0;
  private static final int ANY_END_AGE = 5;

  @Mock private ClientService clientService;
  @Mock private ContractExecutionResult contractExecutionResult;
  @Mock private LedgerValidationResult ledgerValidationResult;

  private TableStoreClientService service;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    service = new TableStoreClientService(clientService);
  }

  @Test
  public void bootstrap_Called_ShouldCallBootstrapAndRegisterContracts() {
    // Arrange Act
    service.bootstrap();

    // Assert
    verify(clientService).bootstrap();
    // Verify contracts are registered
    verify(clientService)
        .registerContract(eq(CONTRACT_CREATE), anyString(), any(byte[].class), eq((String) null));
    verify(clientService)
        .registerContract(eq(CONTRACT_INSERT), anyString(), any(byte[].class), eq((String) null));
    verify(clientService)
        .registerContract(eq(CONTRACT_SELECT), anyString(), any(byte[].class), eq((String) null));
    verify(clientService)
        .registerContract(eq(CONTRACT_UPDATE), anyString(), any(byte[].class), eq((String) null));
    verify(clientService)
        .registerContract(
            eq(CONTRACT_SHOW_TABLES), anyString(), any(byte[].class), eq((String) null));
    verify(clientService)
        .registerContract(
            eq(CONTRACT_GET_HISTORY), anyString(), any(byte[].class), eq((String) null));
    verify(clientService)
        .registerContract(
            eq(CONTRACT_GET_ASSET_ID), anyString(), any(byte[].class), eq((String) null));
    verify(clientService)
        .registerContract(eq(CONTRACT_SCAN), anyString(), any(byte[].class), eq((String) null));
  }

  @Test
  public void bootstrap_ContractAlreadyRegistered_ShouldContinueWithOtherContracts() {
    // Arrange
    ClientException exception =
        new ClientException("Already registered", StatusCode.CONTRACT_ALREADY_REGISTERED);
    doThrow(exception)
        .when(clientService)
        .registerContract(eq(CONTRACT_CREATE), anyString(), any(byte[].class), eq((String) null));

    // Act
    service.bootstrap();

    // Assert
    verify(clientService).bootstrap();
    // Should still register all contracts
    verify(clientService, times(8))
        .registerContract(anyString(), anyString(), any(byte[].class), eq((String) null));
  }

  @Test
  public void bootstrap_OtherExceptionThrown_ShouldThrowException() {
    // Arrange
    ClientException exception = new ClientException("Invalid request", StatusCode.INVALID_REQUEST);
    doNothing().when(clientService).registerCertificate();
    doThrow(exception)
        .when(clientService)
        .registerContract(eq(CONTRACT_CREATE), anyString(), any(byte[].class), eq((String) null));

    // Act
    Throwable thrown = catchThrowable(() -> service.bootstrap());

    // Assert
    assertThat(thrown).isExactlyInstanceOf(ClientException.class);
    assertThat(((ClientException) thrown).getStatusCode()).isEqualTo(StatusCode.INVALID_REQUEST);
  }

  @Test
  public void executeStatement_ValidStatement_ShouldReturnStatementExecutionResult() {
    // Arrange
    String statement = "INSERT INTO test VALUES {}";
    when(clientService.executeContract(eq(CONTRACT_INSERT), any(String.class)))
        .thenReturn(contractExecutionResult);

    // Act
    ExecutionResult result = service.executeStatement(statement);

    // Assert
    verify(clientService).executeContract(eq(CONTRACT_INSERT), any(String.class));
    assertThat(result).isNotNull();
  }

  @Test
  public void executeStatement_MultipleStatements_ShouldThrowIllegalArgumentException() {
    // Arrange
    String statement = "INSERT INTO test VALUES {}; INSERT INTO test VALUES {};";

    // Act
    Throwable thrown = catchThrowable(() -> service.executeStatement(statement));

    // Assert
    assertThat(thrown).isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void validateTableSchema_ShouldCallClientServiceValidateLedgerWithTablePrefix() {
    // Arrange
    when(clientService.validateLedger(PREFIX_TABLE + ANY_TABLE)).thenReturn(ledgerValidationResult);

    // Act
    LedgerValidationResult result = service.validateTableSchema(ANY_TABLE);

    // Assert
    verify(clientService).validateLedger(PREFIX_TABLE + ANY_TABLE);
    assertThat(result).isEqualTo(ledgerValidationResult);
  }

  @Test
  public void validateTableSchema_WithAge_ShouldCallClientServiceValidateLedgerWithTablePrefix() {
    // Arrange
    when(clientService.validateLedger(PREFIX_TABLE + ANY_TABLE, ANY_START_AGE, ANY_END_AGE))
        .thenReturn(ledgerValidationResult);

    // Act
    LedgerValidationResult result =
        service.validateTableSchema(ANY_TABLE, ANY_START_AGE, ANY_END_AGE);

    // Assert
    verify(clientService).validateLedger(PREFIX_TABLE + ANY_TABLE, ANY_START_AGE, ANY_END_AGE);
    assertThat(result).isEqualTo(ledgerValidationResult);
  }

  @Test
  public void
      validateRecord_JsonValueGiven_ShouldCallClientServiceValidateLedgerWithRecordPrefix() {
    // Arrange
    String stringValue = "val";
    String expectedForStringValue =
        PREFIX_RECORD
            + ANY_TABLE
            + ASSET_ID_SEPARATOR
            + ANY_COLUMN
            + ASSET_ID_SEPARATOR
            + stringValue;
    int intValue = 1;
    String expectedForIntValue =
        PREFIX_RECORD + ANY_TABLE + ASSET_ID_SEPARATOR + ANY_COLUMN + ASSET_ID_SEPARATOR + intValue;
    double doubleValue = 1.23;
    String expectedForDoubleValue =
        PREFIX_RECORD
            + ANY_TABLE
            + ASSET_ID_SEPARATOR
            + ANY_COLUMN
            + ASSET_ID_SEPARATOR
            + doubleValue;
    double doubleIntegerValue = 2.0;
    String expectedForDoubleIntegerValue =
        PREFIX_RECORD + ANY_TABLE + ASSET_ID_SEPARATOR + ANY_COLUMN + ASSET_ID_SEPARATOR + "2";
    BigDecimal bigDecimalValue = new BigDecimal("1.2345678901234567890123456789");
    String expectedForBigDecimalValue =
        PREFIX_RECORD
            + ANY_TABLE
            + ASSET_ID_SEPARATOR
            + ANY_COLUMN
            + ASSET_ID_SEPARATOR
            + bigDecimalValue.doubleValue();

    // Act
    service.validateRecord(ANY_TABLE, ANY_COLUMN, Json.createValue(stringValue));
    service.validateRecord(
        ANY_TABLE, ANY_COLUMN, Json.createValue(stringValue), ANY_START_AGE, ANY_END_AGE);
    service.validateRecord(ANY_TABLE, ANY_COLUMN, Json.createValue(intValue));
    service.validateRecord(ANY_TABLE, ANY_COLUMN, Json.createValue(doubleValue));
    service.validateRecord(ANY_TABLE, ANY_COLUMN, Json.createValue(doubleIntegerValue));
    service.validateRecord(ANY_TABLE, ANY_COLUMN, Json.createValue(bigDecimalValue));

    // Assert
    verify(clientService).validateLedger(expectedForStringValue);
    verify(clientService).validateLedger(expectedForStringValue, ANY_START_AGE, ANY_END_AGE);
    verify(clientService).validateLedger(expectedForIntValue);
    verify(clientService).validateLedger(expectedForDoubleValue);
    verify(clientService).validateLedger(expectedForDoubleIntegerValue);
    verify(clientService).validateLedger(expectedForBigDecimalValue);
  }

  @Test
  public void
      validateRecord_ValueNodeGiven_ShouldCallClientServiceValidateLedgerWithRecordPrefix() {
    // Arrange
    String stringValue = "val";
    String expectedForStringValue =
        PREFIX_RECORD
            + ANY_TABLE
            + ASSET_ID_SEPARATOR
            + ANY_COLUMN
            + ASSET_ID_SEPARATOR
            + stringValue;
    int intValue = 1;
    String expectedForIntValue =
        PREFIX_RECORD + ANY_TABLE + ASSET_ID_SEPARATOR + ANY_COLUMN + ASSET_ID_SEPARATOR + intValue;
    double doubleValue = 1.23;
    String expectedForDoubleValue =
        PREFIX_RECORD
            + ANY_TABLE
            + ASSET_ID_SEPARATOR
            + ANY_COLUMN
            + ASSET_ID_SEPARATOR
            + doubleValue;
    double doubleIntegerValue = 2.0;
    String expectedForDoubleIntegerValue =
        PREFIX_RECORD + ANY_TABLE + ASSET_ID_SEPARATOR + ANY_COLUMN + ASSET_ID_SEPARATOR + "2";
    BigDecimal bigDecimalValue = new BigDecimal("1.2345678901234567890123456789");
    String expectedForBigDecimalValue =
        PREFIX_RECORD
            + ANY_TABLE
            + ASSET_ID_SEPARATOR
            + ANY_COLUMN
            + ASSET_ID_SEPARATOR
            + bigDecimalValue.doubleValue();

    // Act
    service.validateRecord(ANY_TABLE, ANY_COLUMN, TextNode.valueOf(stringValue));
    service.validateRecord(
        ANY_TABLE, ANY_COLUMN, TextNode.valueOf(stringValue), ANY_START_AGE, ANY_END_AGE);
    service.validateRecord(ANY_TABLE, ANY_COLUMN, IntNode.valueOf(intValue));
    service.validateRecord(ANY_TABLE, ANY_COLUMN, DoubleNode.valueOf(doubleValue));
    service.validateRecord(ANY_TABLE, ANY_COLUMN, DoubleNode.valueOf(doubleIntegerValue));
    service.validateRecord(ANY_TABLE, ANY_COLUMN, DecimalNode.valueOf(bigDecimalValue));

    // Assert
    verify(clientService).validateLedger(expectedForStringValue);
    verify(clientService).validateLedger(expectedForStringValue, ANY_START_AGE, ANY_END_AGE);
    verify(clientService).validateLedger(expectedForIntValue);
    verify(clientService).validateLedger(expectedForDoubleValue);
    verify(clientService).validateLedger(expectedForDoubleIntegerValue);
    verify(clientService).validateLedger(expectedForBigDecimalValue);
  }

  @Test
  public void
      validateRecord_StringValueGiven_ShouldCallClientServiceValidateLedgerWithRecordPrefix() {
    // Arrange
    String stringValue = "\"val\"";
    String expectedForStringValue =
        PREFIX_RECORD + ANY_TABLE + ASSET_ID_SEPARATOR + ANY_COLUMN + ASSET_ID_SEPARATOR + "val";
    String intValue = "1";
    String expectedForIntValue =
        PREFIX_RECORD + ANY_TABLE + ASSET_ID_SEPARATOR + ANY_COLUMN + ASSET_ID_SEPARATOR + intValue;
    String doubleValue = "1.23";
    String expectedForDoubleValue =
        PREFIX_RECORD
            + ANY_TABLE
            + ASSET_ID_SEPARATOR
            + ANY_COLUMN
            + ASSET_ID_SEPARATOR
            + doubleValue;
    String doubleIntegerValue = "2.0";
    String expectedForDoubleIntegerValue =
        PREFIX_RECORD + ANY_TABLE + ASSET_ID_SEPARATOR + ANY_COLUMN + ASSET_ID_SEPARATOR + "2";
    String bigDecimalValue = "1.2345678901234567890123456789";
    String expectedForBigDecimalValue =
        PREFIX_RECORD
            + ANY_TABLE
            + ASSET_ID_SEPARATOR
            + ANY_COLUMN
            + ASSET_ID_SEPARATOR
            + new BigDecimal(bigDecimalValue).doubleValue();

    // Act
    service.validateRecord(ANY_TABLE, ANY_COLUMN, stringValue);
    service.validateRecord(ANY_TABLE, ANY_COLUMN, stringValue, ANY_START_AGE, ANY_END_AGE);
    service.validateRecord(ANY_TABLE, ANY_COLUMN, intValue);
    service.validateRecord(ANY_TABLE, ANY_COLUMN, doubleValue);
    service.validateRecord(ANY_TABLE, ANY_COLUMN, doubleIntegerValue);
    service.validateRecord(ANY_TABLE, ANY_COLUMN, bigDecimalValue);

    // Assert
    verify(clientService).validateLedger(expectedForStringValue);
    verify(clientService).validateLedger(expectedForStringValue, ANY_START_AGE, ANY_END_AGE);
    verify(clientService).validateLedger(expectedForIntValue);
    verify(clientService).validateLedger(expectedForDoubleValue);
    verify(clientService).validateLedger(expectedForDoubleIntegerValue);
    verify(clientService).validateLedger(expectedForBigDecimalValue);
  }

  @Test
  public void
      validateIndexRecord_JsonValueGiven_ShouldCallClientServiceValidateLedgerWithIndexPrefix() {
    // Arrange
    String stringValue = "val";
    String expectedForStringValue =
        PREFIX_INDEX
            + ANY_TABLE
            + ASSET_ID_SEPARATOR
            + ANY_COLUMN
            + ASSET_ID_SEPARATOR
            + stringValue;
    int intValue = 1;
    String expectedForIntValue =
        PREFIX_INDEX + ANY_TABLE + ASSET_ID_SEPARATOR + ANY_COLUMN + ASSET_ID_SEPARATOR + intValue;
    double doubleValue = 1.23;
    String expectedForDoubleValue =
        PREFIX_INDEX
            + ANY_TABLE
            + ASSET_ID_SEPARATOR
            + ANY_COLUMN
            + ASSET_ID_SEPARATOR
            + doubleValue;
    double doubleIntegerValue = 2.0;
    String expectedForDoubleIntegerValue =
        PREFIX_INDEX + ANY_TABLE + ASSET_ID_SEPARATOR + ANY_COLUMN + ASSET_ID_SEPARATOR + "2";
    BigDecimal bigDecimalValue = new BigDecimal("1.2345678901234567890123456789");
    String expectedForBigDecimalValue =
        PREFIX_INDEX
            + ANY_TABLE
            + ASSET_ID_SEPARATOR
            + ANY_COLUMN
            + ASSET_ID_SEPARATOR
            + bigDecimalValue.doubleValue();

    // Act
    service.validateIndexRecord(ANY_TABLE, ANY_COLUMN, Json.createValue(stringValue));
    service.validateIndexRecord(
        ANY_TABLE, ANY_COLUMN, Json.createValue(stringValue), ANY_START_AGE, ANY_END_AGE);
    service.validateIndexRecord(ANY_TABLE, ANY_COLUMN, Json.createValue(intValue));
    service.validateIndexRecord(ANY_TABLE, ANY_COLUMN, Json.createValue(doubleValue));
    service.validateIndexRecord(ANY_TABLE, ANY_COLUMN, Json.createValue(doubleIntegerValue));
    service.validateIndexRecord(ANY_TABLE, ANY_COLUMN, Json.createValue(bigDecimalValue));

    // Assert
    verify(clientService).validateLedger(expectedForStringValue);
    verify(clientService).validateLedger(expectedForStringValue, ANY_START_AGE, ANY_END_AGE);
    verify(clientService).validateLedger(expectedForIntValue);
    verify(clientService).validateLedger(expectedForDoubleValue);
    verify(clientService).validateLedger(expectedForDoubleIntegerValue);
    verify(clientService).validateLedger(expectedForBigDecimalValue);
  }

  @Test
  public void
      validateIndexRecord_ValueNodeGiven_ShouldCallClientServiceValidateLedgerWithIndexPrefix() {
    // Arrange
    String stringValue = "val";
    String expectedForStringValue =
        PREFIX_INDEX
            + ANY_TABLE
            + ASSET_ID_SEPARATOR
            + ANY_COLUMN
            + ASSET_ID_SEPARATOR
            + stringValue;
    int intValue = 1;
    String expectedForIntValue =
        PREFIX_INDEX + ANY_TABLE + ASSET_ID_SEPARATOR + ANY_COLUMN + ASSET_ID_SEPARATOR + intValue;
    double doubleValue = 1.23;
    String expectedForDoubleValue =
        PREFIX_INDEX
            + ANY_TABLE
            + ASSET_ID_SEPARATOR
            + ANY_COLUMN
            + ASSET_ID_SEPARATOR
            + doubleValue;
    double doubleIntegerValue = 2.0;
    String expectedForDoubleIntegerValue =
        PREFIX_INDEX + ANY_TABLE + ASSET_ID_SEPARATOR + ANY_COLUMN + ASSET_ID_SEPARATOR + "2";
    BigDecimal bigDecimalValue = new BigDecimal("1.2345678901234567890123456789");
    String expectedForBigDecimalValue =
        PREFIX_INDEX
            + ANY_TABLE
            + ASSET_ID_SEPARATOR
            + ANY_COLUMN
            + ASSET_ID_SEPARATOR
            + bigDecimalValue.doubleValue();

    // Act
    service.validateIndexRecord(ANY_TABLE, ANY_COLUMN, TextNode.valueOf(stringValue));
    service.validateIndexRecord(
        ANY_TABLE, ANY_COLUMN, TextNode.valueOf(stringValue), ANY_START_AGE, ANY_END_AGE);
    service.validateIndexRecord(ANY_TABLE, ANY_COLUMN, IntNode.valueOf(intValue));
    service.validateIndexRecord(ANY_TABLE, ANY_COLUMN, DoubleNode.valueOf(doubleValue));
    service.validateIndexRecord(ANY_TABLE, ANY_COLUMN, DoubleNode.valueOf(doubleIntegerValue));
    service.validateIndexRecord(ANY_TABLE, ANY_COLUMN, DecimalNode.valueOf(bigDecimalValue));

    // Assert
    verify(clientService).validateLedger(expectedForStringValue);
    verify(clientService).validateLedger(expectedForStringValue, ANY_START_AGE, ANY_END_AGE);
    verify(clientService).validateLedger(expectedForIntValue);
    verify(clientService).validateLedger(expectedForDoubleValue);
    verify(clientService).validateLedger(expectedForDoubleIntegerValue);
    verify(clientService).validateLedger(expectedForBigDecimalValue);
  }

  @Test
  public void
      validateIndexRecord_StringValueGiven_ShouldCallClientServiceValidateLedgerWithIndexPrefix() {
    // Arrange
    String stringValue = "\"val\"";
    String expectedForStringValue =
        PREFIX_INDEX + ANY_TABLE + ASSET_ID_SEPARATOR + ANY_COLUMN + ASSET_ID_SEPARATOR + "val";
    String intValue = "1";
    String expectedForIntValue =
        PREFIX_INDEX + ANY_TABLE + ASSET_ID_SEPARATOR + ANY_COLUMN + ASSET_ID_SEPARATOR + intValue;
    String doubleValue = "1.23";
    String expectedForDoubleValue =
        PREFIX_INDEX
            + ANY_TABLE
            + ASSET_ID_SEPARATOR
            + ANY_COLUMN
            + ASSET_ID_SEPARATOR
            + doubleValue;
    String doubleIntegerValue = "2.0";
    String expectedForDoubleIntegerValue =
        PREFIX_INDEX + ANY_TABLE + ASSET_ID_SEPARATOR + ANY_COLUMN + ASSET_ID_SEPARATOR + "2";
    String bigDecimalValue = "1.2345678901234567890123456789";
    String expectedForBigDecimalValue =
        PREFIX_INDEX
            + ANY_TABLE
            + ASSET_ID_SEPARATOR
            + ANY_COLUMN
            + ASSET_ID_SEPARATOR
            + new BigDecimal(bigDecimalValue).doubleValue();

    // Act
    service.validateIndexRecord(ANY_TABLE, ANY_COLUMN, stringValue);
    service.validateIndexRecord(ANY_TABLE, ANY_COLUMN, stringValue, ANY_START_AGE, ANY_END_AGE);
    service.validateIndexRecord(ANY_TABLE, ANY_COLUMN, intValue);
    service.validateIndexRecord(ANY_TABLE, ANY_COLUMN, doubleValue);
    service.validateIndexRecord(ANY_TABLE, ANY_COLUMN, doubleIntegerValue);
    service.validateIndexRecord(ANY_TABLE, ANY_COLUMN, bigDecimalValue);

    // Assert
    verify(clientService).validateLedger(expectedForStringValue);
    verify(clientService).validateLedger(expectedForStringValue, ANY_START_AGE, ANY_END_AGE);
    verify(clientService).validateLedger(expectedForIntValue);
    verify(clientService).validateLedger(expectedForDoubleValue);
    verify(clientService).validateLedger(expectedForDoubleIntegerValue);
    verify(clientService).validateLedger(expectedForBigDecimalValue);
  }
}
