package com.scalar.dl.tablestore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.amazon.ion.IonStruct;
import com.amazon.ion.IonSystem;
import com.amazon.ion.system.IonSystemBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.genericcontracts.table.v1_0_0.Constants;
import com.scalar.dl.ledger.LedgerEndToEndTestBase;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.util.JacksonSerDe;
import com.scalar.dl.tablestore.client.error.TableStoreClientError;
import com.scalar.dl.tablestore.client.model.StatementExecutionResult;
import com.scalar.dl.tablestore.client.service.ClientService;
import com.scalar.dl.tablestore.client.service.ClientServiceFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import javax.json.Json;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TableStoreEndToEndTest extends LedgerEndToEndTestBase {
  private static final String SOME_ENTITY = "entity";
  private static final String ACCOUNT_TABLE = "account";
  private static final String ACCOUNT_ID = "account_id";
  private static final String ACCOUNT_TYPE = "account_type";
  private static final String ACCOUNT_NAME = "account_name";
  private static final String ACTIVE = "active";
  private static final String BALANCE = "balance";
  private static final String PAYMENT_TABLE = "payment";
  private static final String PAYMENT_ID = "payment_id";
  private static final String SENDER = "sender";
  private static final String RECEIVER = "receiver";
  private static final String AMOUNT = "amount";
  private static final String DATE = "payment_date";
  private static final String INFORMATION_SCHEMA_TABLES = "information_schema.tables";
  private static final String TABLE_NAME = "table_name";
  private static final int INITIAL_BALANCE = 1000;
  private static final int NUM_ACCOUNTS = 8;
  private static final int NUM_TYPES = 4;

  private static final Pattern FIELD_KEY = Pattern.compile("\"([^\"]+)\":");
  private static final Pattern STRING_VALUE = Pattern.compile("\"([^\"]*)\"");
  private static final Pattern SINGLE_QUOTE_ESCAPE = Pattern.compile("'([^']*)'");

  private static final IonSystem ionSystem = IonSystemBuilder.standard().build();
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final JacksonSerDe jacksonSerDe = new JacksonSerDe(mapper);
  private static final ClientServiceFactory clientServiceFactory = new ClientServiceFactory();
  private ClientService clientService;

  @BeforeAll
  public void setUpBeforeClass() throws Exception {
    super.setUpBeforeClass();
    clientService = clientServiceFactory.create(createClientConfig(SOME_ENTITY));
  }

  @BeforeEach
  public void setUp() {
    createTables();
  }

  private void createTables() {
    clientService.executeStatement(
        "CREATE TABLE "
            + ACCOUNT_TABLE
            + "("
            + ACCOUNT_ID
            + " INT PRIMARY KEY,"
            + ACCOUNT_TYPE
            + " INT,"
            + ACCOUNT_NAME
            + " STRING,"
            + ACTIVE
            + " BOOLEAN)");
    clientService.executeStatement(
        "CREATE TABLE "
            + PAYMENT_TABLE
            + "("
            + PAYMENT_ID
            + " INT PRIMARY KEY,"
            + SENDER
            + " INT,"
            + RECEIVER
            + " INT,"
            + DATE
            + " STRING)");
  }

  protected void populateAccountRecords() throws IOException {
    for (int i = 0; i < NUM_ACCOUNTS; i++) {
      insertAccountRecord(i, INITIAL_BALANCE);
    }
  }

  private void populatePaymentRecords() {}

  private IonStruct createAccountIon(int accountId, int accountType, int balance) {
    IonStruct struct = ionSystem.newEmptyStruct();
    struct.put(ACCOUNT_ID).newInt(accountId);
    struct.put(ACCOUNT_TYPE).newInt(accountType);
    struct.put(ACCOUNT_NAME).newString(accountId + "_" + accountType);
    struct.put(ACTIVE).newBool(true);
    struct.put(BALANCE).newInt(balance);
    return struct;
  }

  private int getAccountType(int accountId) {
    return accountId % NUM_TYPES;
  }

  private String getAccountName(int accountId) {
    int accountType = getAccountType(accountId);
    return accountId + "_" + accountType;
  }

  private boolean isActive(int accountId) {
    return accountId % 2 == 0;
  }

  private JsonNode createAccountJson(int accountId, int balance) {
    return jacksonSerDe
        .getObjectMapper()
        .createObjectNode()
        .put(ACCOUNT_ID, accountId)
        .put(ACCOUNT_TYPE, getAccountType(accountId))
        .put(ACCOUNT_NAME, getAccountName(accountId))
        .put(ACTIVE, isActive(accountId))
        .put(BALANCE, balance);
  }

  private void insertAccountRecord(int accountId, int balance) {
    clientService.executeStatement(
        "INSERT INTO "
            + ACCOUNT_TABLE
            + " VALUES "
            + toPartiQLJsonString(createAccountJson(accountId, balance)));
  }

  private void updateAccountRecord(int accountId, int balance) {
    clientService.executeStatement(
        "UPDATE "
            + ACCOUNT_TABLE
            + " SET "
            + BALANCE
            + " = "
            + balance
            + " WHERE "
            + ACCOUNT_ID
            + " = "
            + accountId);
  }

  private ObjectNode createPaymentJson(int paymentId) {
    return jacksonSerDe.getObjectMapper().createObjectNode().put(PAYMENT_ID, paymentId);
  }

  private JsonNode createPaymentJson(
      int paymentId, int sender, int receiver, int amount, String date) {
    return createPaymentJson(paymentId)
        .put(SENDER, sender)
        .put(RECEIVER, receiver)
        .put(AMOUNT, amount)
        .put(DATE, date);
  }

  private void insertPaymentRecord(
      int paymentId, int sender, int receiver, int amount, String date) {
    clientService.executeStatement(
        "INSERT INTO "
            + PAYMENT_TABLE
            + " VALUES "
            + toPartiQLJsonString(createPaymentJson(paymentId, sender, receiver, amount, date)));
  }

  private void insertPaymentRecordWithNullValues(int paymentId) {
    clientService.executeStatement(
        "INSERT INTO "
            + PAYMENT_TABLE
            + " VALUES "
            + toPartiQLJsonString(createPaymentJson(paymentId)));
  }

  private String toPartiQLJsonString(JsonNode node) {
    String result = jacksonSerDe.serialize(node);

    // Remove double quotation in the key
    result = FIELD_KEY.matcher(result).replaceAll("$1:");

    // Convert string value to '...'
    result = STRING_VALUE.matcher(result).replaceAll("'$1'");

    // Escape single quotation
    Matcher m = SINGLE_QUOTE_ESCAPE.matcher(result);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      String inner = m.group(1).replace("'", "''");
      m.appendReplacement(sb, "'" + inner + "'");
    }
    m.appendTail(sb);

    return sb.toString();
  }

  private void assertRecords(JsonNode actual, List<JsonNode> expected) {
    List<JsonNode> records = new ArrayList<>();
    assertThat(actual.isArray()).isTrue();
    actual.forEach(records::add);
    assertThat(records).containsExactlyInAnyOrderElementsOf(expected);
  }

  @Test
  public void executeStatement_SelectSqlWithPrimaryKeyGiven_ShouldReturnRecord()
      throws IOException {
    // Arrange
    populateAccountRecords();
    int accountId = 1;
    ImmutableList<JsonNode> expectedRecords =
        ImmutableList.of(createAccountJson(accountId, INITIAL_BALANCE));

    // Act
    StatementExecutionResult result =
        clientService.executeStatement(
            "SELECT * FROM " + ACCOUNT_TABLE + " WHERE " + ACCOUNT_ID + " = " + accountId);

    // Assert
    assertThat(result.getResult().isPresent()).isTrue();
    assertRecords(jacksonSerDe.deserialize(result.getResult().get()), expectedRecords);
  }

  @Test
  public void executeStatement_SelectSqlWithNonExistingPrimaryKeyGiven_ShouldReturnEmpty()
      throws IOException {
    // Arrange
    populateAccountRecords();
    int accountId = NUM_ACCOUNTS;
    JsonNode expected = mapper.createArrayNode();

    // Act
    StatementExecutionResult result =
        clientService.executeStatement(
            "SELECT * FROM " + ACCOUNT_TABLE + " WHERE " + ACCOUNT_ID + " = " + accountId);

    // Assert
    assertThat(result.getResult().isPresent()).isTrue();
    assertThat(jacksonSerDe.deserialize(result.getResult().get())).isEqualTo(expected);
  }

  @Test
  public void executeStatement_SelectSqlWithProjectionGiven_ShouldReturnRecord()
      throws IOException {
    // Arrange
    populateAccountRecords();
    int accountId = 1;
    JsonNode expectedJson =
        jacksonSerDe
            .getObjectMapper()
            .createObjectNode()
            .put(ACCOUNT_ID, accountId)
            .put(BALANCE, INITIAL_BALANCE);
    ImmutableList<JsonNode> expectedRecords = ImmutableList.of(expectedJson);

    // Act
    StatementExecutionResult result =
        clientService.executeStatement(
            "SELECT "
                + ACCOUNT_ID
                + ","
                + BALANCE
                + " FROM "
                + ACCOUNT_TABLE
                + " WHERE "
                + ACCOUNT_ID
                + " = "
                + accountId);

    // Assert
    assertThat(result.getResult().isPresent()).isTrue();
    assertRecords(jacksonSerDe.deserialize(result.getResult().get()), expectedRecords);
  }

  @Test
  public void executeStatement_SelectSqlWithNumberIndexKeyGiven_ShouldReturnRecords()
      throws IOException {
    // Arrange
    populateAccountRecords();
    int accountType = 1;
    ImmutableList<JsonNode> expectedRecords =
        IntStream.range(0, NUM_ACCOUNTS)
            .filter(i -> i % NUM_TYPES == accountType)
            .mapToObj(i -> createAccountJson(i, INITIAL_BALANCE))
            .collect(ImmutableList.toImmutableList());

    // Act
    StatementExecutionResult result =
        clientService.executeStatement(
            "SELECT * FROM " + ACCOUNT_TABLE + " WHERE " + ACCOUNT_TYPE + " = " + accountType);

    // Assert
    assertThat(result.getResult().isPresent()).isTrue();
    assertRecords(jacksonSerDe.deserialize(result.getResult().get()), expectedRecords);
  }

  @Test
  public void executeStatement_SelectSqlWithStringIndexKeyGiven_ShouldReturnRecords()
      throws IOException {
    // Arrange
    populateAccountRecords();
    int accountId = 5;
    String accountName = getAccountName(accountId);
    ImmutableList<JsonNode> expectedRecords =
        ImmutableList.of(createAccountJson(accountId, INITIAL_BALANCE));
    // Act
    StatementExecutionResult result =
        clientService.executeStatement(
            "SELECT * FROM "
                + ACCOUNT_TABLE
                + " WHERE "
                + ACCOUNT_NAME
                + " = '"
                + accountName
                + "'");

    // Assert
    assertThat(result.getResult().isPresent()).isTrue();
    assertRecords(jacksonSerDe.deserialize(result.getResult().get()), expectedRecords);
  }

  @Test
  public void executeStatement_SelectSqlWithBooleanIndexKeyGiven_ShouldReturnRecords()
      throws IOException {
    // Arrange
    populateAccountRecords();
    boolean isActive = false;
    ImmutableList<JsonNode> expectedRecords =
        IntStream.range(0, NUM_ACCOUNTS)
            .filter(i -> i % 2 != 0)
            .mapToObj(i -> createAccountJson(i, INITIAL_BALANCE))
            .collect(ImmutableList.toImmutableList());

    // Act
    StatementExecutionResult result =
        clientService.executeStatement(
            "SELECT * FROM " + ACCOUNT_TABLE + " WHERE " + ACTIVE + " = " + isActive);

    // Assert
    assertThat(result.getResult().isPresent()).isTrue();
    assertRecords(jacksonSerDe.deserialize(result.getResult().get()), expectedRecords);
  }

  @Test
  public void executeStatement_SelectSqlWithRangePredicatesGiven_ShouldReturnRecords()
      throws IOException {
    // Arrange
    populateAccountRecords();
    int accountTypeStart = 3;
    int accountTypeEnd = 4;
    boolean isActive = true;
    ImmutableList<JsonNode> expectedRecords =
        IntStream.range(0, NUM_ACCOUNTS)
            .filter(
                i ->
                    i % 2 == 0
                        && getAccountType(i) >= accountTypeStart
                        && getAccountType(i) <= accountTypeEnd)
            .mapToObj(i -> createAccountJson(i, INITIAL_BALANCE))
            .collect(ImmutableList.toImmutableList());

    // Act
    StatementExecutionResult result =
        clientService.executeStatement(
            "SELECT * FROM "
                + ACCOUNT_TABLE
                + " WHERE "
                + ACTIVE
                + " = "
                + isActive
                + " AND "
                + ACCOUNT_TYPE
                + " >= "
                + accountTypeStart
                + " AND "
                + ACCOUNT_TYPE
                + " <= "
                + accountTypeEnd);

    // Assert
    assertThat(result.getResult().isPresent()).isTrue();
    assertRecords(jacksonSerDe.deserialize(result.getResult().get()), expectedRecords);
  }

  @Test
  public void executeStatement_SelectSqlWithOutOfRangePredicatesGiven_ShouldReturnEmpty()
      throws IOException {
    // Arrange
    populateAccountRecords();
    int accountTypeStart = NUM_TYPES;
    int accountTypeEnd = NUM_TYPES + 1;
    boolean isActive = true;
    JsonNode expected = mapper.createArrayNode();

    // Act
    StatementExecutionResult result =
        clientService.executeStatement(
            "SELECT * FROM "
                + ACCOUNT_TABLE
                + " WHERE "
                + ACTIVE
                + " = "
                + isActive
                + " AND "
                + ACCOUNT_TYPE
                + " >= "
                + accountTypeStart
                + " AND "
                + ACCOUNT_TYPE
                + " <= "
                + accountTypeEnd);

    // Assert
    assertThat(result.getResult().isPresent()).isTrue();
    assertThat(jacksonSerDe.deserialize(result.getResult().get())).isEqualTo(expected);
  }

  @Test
  public void executeStatement_SelectSqlWithNullConditionForIndexColumnGiven_ShouldReturnRecords()
      throws IOException {
    // Arrange
    insertPaymentRecord(1, 1, 2, 100, "");
    insertPaymentRecordWithNullValues(2);

    // Act
    StatementExecutionResult result =
        clientService.executeStatement(
            "SELECT * FROM "
                + PAYMENT_TABLE
                + " WHERE "
                + PAYMENT_ID
                + " IS NOT NULL AND "
                + DATE
                + " IS NULL AND "
                + SENDER
                + " IS NULL AND "
                + RECEIVER
                + " IS NULL");

    // Assert
    assertThat(result.getResult().isPresent()).isTrue();
    JsonNode tables = jacksonSerDe.deserialize(result.getResult().get());
    assertThat(tables.isArray()).isTrue();
    assertThat(tables.size()).isEqualTo(1);
    assertThat(tables.get(0).get(PAYMENT_ID).asInt()).isEqualTo(2);
  }

  @Test
  public void executeStatement_SelectSqlWithJoinGiven_ShouldReturnJoinedRecords()
      throws IOException {
    // Arrange
    String date1 = "2024-01-01";
    String date2 = "2025-01-01";
    int amount = 100;
    populateAccountRecords();
    insertPaymentRecord(1, 1, 2, amount, date1);
    insertPaymentRecord(2, 1, 3, amount, date1);
    insertPaymentRecord(3, 1, 4, amount, date2);

    // Act
    StatementExecutionResult result =
        clientService.executeStatement(
            String.format(
                "SELECT * FROM %s JOIN %s AS %s ON %s = %s JOIN %s AS %s ON %s = %s WHERE %s = '%s'",
                PAYMENT_TABLE,
                ACCOUNT_TABLE,
                SENDER,
                PAYMENT_TABLE + "." + SENDER,
                SENDER + "." + ACCOUNT_ID,
                ACCOUNT_TABLE,
                RECEIVER,
                PAYMENT_TABLE + "." + RECEIVER,
                RECEIVER + "." + ACCOUNT_ID,
                PAYMENT_TABLE + "." + DATE,
                date1));

    // Assert
    assertThat(result.getResult().isPresent()).isTrue();
    JsonNode tables = jacksonSerDe.deserialize(result.getResult().get());
    assertThat(tables.isArray()).isTrue();
    assertThat(tables.size()).isEqualTo(2);
    assertThat(tables.get(0).get(SENDER + "." + ACCOUNT_ID).asInt()).isEqualTo(1);
    assertThat(tables.get(0).get(RECEIVER + "." + ACCOUNT_ID).asInt()).isEqualTo(2);
    assertThat(tables.get(0).get(PAYMENT_TABLE + "." + AMOUNT).asInt()).isEqualTo(amount);
    assertThat(tables.get(1).get(SENDER + "." + ACCOUNT_ID).asInt()).isEqualTo(1);
    assertThat(tables.get(1).get(RECEIVER + "." + ACCOUNT_ID).asInt()).isEqualTo(3);
    assertThat(tables.get(1).get(PAYMENT_TABLE + "." + AMOUNT).asInt()).isEqualTo(amount);
  }

  @Test
  public void
      executeStatement_SelectSqlWithJoinAndProjectionGiven_ShouldReturnJoinedAndProjectedRecords()
          throws IOException {
    // Arrange
    String date = "2025-01-01";
    int amount = 100;
    populateAccountRecords();
    insertPaymentRecord(1, 2, 3, amount, date);

    // Act
    StatementExecutionResult result =
        clientService.executeStatement(
            String.format(
                "SELECT %s,%s FROM %s JOIN %s AS %s ON %s = %s JOIN %s AS %s ON %s = %s WHERE %s = '%s'",
                SENDER + "." + ACCOUNT_ID,
                PAYMENT_TABLE + "." + AMOUNT,
                PAYMENT_TABLE,
                ACCOUNT_TABLE,
                SENDER,
                PAYMENT_TABLE + "." + SENDER,
                SENDER + "." + ACCOUNT_ID,
                ACCOUNT_TABLE,
                RECEIVER,
                PAYMENT_TABLE + "." + RECEIVER,
                RECEIVER + "." + ACCOUNT_ID,
                PAYMENT_TABLE + "." + DATE,
                date));

    // Assert
    assertThat(result.getResult().isPresent()).isTrue();
    JsonNode tables = jacksonSerDe.deserialize(result.getResult().get());
    assertThat(tables.isArray()).isTrue();
    assertThat(tables.size()).isEqualTo(1);
    assertThat(tables.get(0).get(SENDER + "." + ACCOUNT_ID).asInt()).isEqualTo(2);
    assertThat(tables.get(0).get(PAYMENT_TABLE + "." + AMOUNT).asInt()).isEqualTo(amount);
    assertThat(tables.get(0).has(RECEIVER + "." + ACCOUNT_ID)).isFalse();
  }

  @Test
  public void executeStatement_SelectSqlWithJoinAndPredicatesGiven_ShouldReturnJoinedRecords()
      throws IOException {
    // Arrange
    String date = "2025-01-01";
    populateAccountRecords();
    insertPaymentRecord(1, 1, 2, 100, date);
    insertPaymentRecord(2, 1, 3, 200, date);
    insertPaymentRecord(3, 1, 4, 300, date);

    // Act
    StatementExecutionResult result =
        clientService.executeStatement(
            String.format(
                "SELECT * FROM %s JOIN %s AS %s ON %s = %s JOIN %s AS %s ON %s = %s WHERE %s = '%s' AND %s <= 200",
                PAYMENT_TABLE,
                ACCOUNT_TABLE,
                SENDER,
                PAYMENT_TABLE + "." + SENDER,
                SENDER + "." + ACCOUNT_ID,
                ACCOUNT_TABLE,
                RECEIVER,
                PAYMENT_TABLE + "." + RECEIVER,
                RECEIVER + "." + ACCOUNT_ID,
                PAYMENT_TABLE + "." + DATE,
                date,
                PAYMENT_TABLE + "." + AMOUNT));

    // Assert
    assertThat(result.getResult().isPresent()).isTrue();
    JsonNode tables = jacksonSerDe.deserialize(result.getResult().get());
    assertThat(tables.isArray()).isTrue();
    assertThat(tables.size()).isEqualTo(2);
    assertThat(tables.get(0).get(RECEIVER + "." + ACCOUNT_ID).asInt()).isEqualTo(2);
    assertThat(tables.get(0).get(PAYMENT_TABLE + "." + AMOUNT).asInt()).isEqualTo(100);
    assertThat(tables.get(1).get(RECEIVER + "." + ACCOUNT_ID).asInt()).isEqualTo(3);
    assertThat(tables.get(1).get(PAYMENT_TABLE + "." + AMOUNT).asInt()).isEqualTo(200);
  }

  @Test
  public void
      executeStatement_SelectSqlWithImplicitJoinGiven_ShouldThrowIllegalArgumentException() {
    // Arrange Act Assert
    assertThatThrownBy(
            () ->
                clientService.executeStatement(
                    String.format(
                        "SELECT * FROM %s, %s AS %s, %s AS %s WHERE %s=%s AND %s=%s AND %s='2025-01-01'",
                        PAYMENT_TABLE,
                        ACCOUNT_TABLE,
                        SENDER,
                        ACCOUNT_TABLE,
                        RECEIVER,
                        PAYMENT_TABLE + "." + SENDER,
                        SENDER + "." + ACCOUNT_ID,
                        PAYMENT_TABLE + "." + RECEIVER,
                        RECEIVER + "." + ACCOUNT_ID,
                        PAYMENT_TABLE + "." + DATE)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            TableStoreClientError.SYNTAX_ERROR_CROSS_AND_IMPLICIT_JOIN_NOT_SUPPORTED.buildCode());
  }

  @Test
  public void executeStatement_SelectSqlWithoutConditionsGiven_ShouldThrowClientException() {
    // Arrange Act Assert
    assertThatThrownBy(() -> clientService.executeStatement("SELECT * FROM " + ACCOUNT_TABLE))
        .isInstanceOf(ClientException.class)
        .hasMessageContaining(Constants.INVALID_KEY_SPECIFICATION);
  }

  @Test
  public void executeStatement_SelectSqlWithNonKeyConditionsOnlyGiven_ShouldThrowClientException() {
    // Arrange Act Assert
    assertThatThrownBy(
            () ->
                clientService.executeStatement(
                    "SELECT * FROM "
                        + PAYMENT_TABLE
                        + " WHERE "
                        + PAYMENT_ID
                        + " IS NOT NULL AND "
                        + DATE
                        + " IS NOT NULL"))
        .isInstanceOf(ClientException.class)
        .hasMessageContaining(Constants.INVALID_KEY_SPECIFICATION);
  }

  @Test
  public void executeStatement_SelectSqlWithNonExistingTableGiven_ShouldThrowClientException() {
    // Arrange Act Assert
    assertThatThrownBy(() -> clientService.executeStatement("SELECT * FROM foo"))
        .isInstanceOf(ClientException.class)
        .hasMessageContaining(Constants.TABLE_NOT_EXIST);
  }

  @Test
  public void executeStatement_InsertSqlForNonExistingRecordGiven_ShouldReturnRecord()
      throws IOException {
    // Arrange
    populateAccountRecords();
    int accountId = NUM_ACCOUNTS;
    JsonNode expectedRecord = createAccountJson(accountId, INITIAL_BALANCE);

    // Act
    clientService.executeStatement(
        "INSERT INTO " + ACCOUNT_TABLE + " VALUES " + toPartiQLJsonString(expectedRecord));

    // Assert
    StatementExecutionResult result =
        clientService.executeStatement(
            "SELECT * FROM " + ACCOUNT_TABLE + " WHERE " + ACCOUNT_ID + " = " + accountId);
    assertThat(result.getResult().isPresent()).isTrue();
    assertRecords(
        jacksonSerDe.deserialize(result.getResult().get()), ImmutableList.of(expectedRecord));
  }

  @Test
  public void executeStatement_InsertSqlForExistingRecordGiven_ShouldThrowClientException()
      throws IOException {
    // Arrange
    populateAccountRecords();
    int accountId = 0;
    JsonNode existingRecord = createAccountJson(accountId, INITIAL_BALANCE);

    // Act Assert
    assertThatThrownBy(
            () ->
                clientService.executeStatement(
                    "INSERT INTO "
                        + ACCOUNT_TABLE
                        + " VALUES "
                        + toPartiQLJsonString(existingRecord)))
        .isInstanceOf(ClientException.class)
        .hasMessageContaining(Constants.RECORD_ALREADY_EXISTS);
  }

  @Test
  public void executeStatement_InsertSqlWithNonExistingTableGiven_ShouldThrowClientException() {
    // Arrange Act Assert
    assertThatThrownBy(() -> clientService.executeStatement("INSERT INTO foo VALUES {}"))
        .isInstanceOf(ClientException.class)
        .hasMessageContaining(Constants.TABLE_NOT_EXIST);
  }

  @Test
  public void executeStatement_UpdateSqlWithPrimaryKeyForExistingRecordGiven_ShouldUpdateRecord()
      throws IOException {
    // Arrange
    populateAccountRecords();
    int accountId = 1;
    int expected = 100;
    ImmutableList<JsonNode> expectedRecords =
        ImmutableList.of(createAccountJson(accountId, expected));

    // Act
    clientService.executeStatement(
        "UPDATE "
            + ACCOUNT_TABLE
            + " SET "
            + BALANCE
            + " = "
            + expected
            + " WHERE "
            + ACCOUNT_ID
            + " = "
            + accountId);

    // Assert
    StatementExecutionResult result =
        clientService.executeStatement(
            "SELECT * FROM " + ACCOUNT_TABLE + " WHERE " + ACCOUNT_ID + " = " + accountId);
    assertThat(result.getResult().isPresent()).isTrue();
    assertRecords(jacksonSerDe.deserialize(result.getResult().get()), expectedRecords);
  }

  @Test
  public void executeStatement_UpdateSqlForNonExistingRecordGiven_ShouldDoNothing()
      throws IOException {
    // Arrange
    populateAccountRecords();
    int accountId = NUM_ACCOUNTS;
    int newBalance = 100;
    JsonNode expected = mapper.createArrayNode();

    // Act
    clientService.executeStatement(
        "UPDATE "
            + ACCOUNT_TABLE
            + " SET "
            + BALANCE
            + " = "
            + newBalance
            + " WHERE "
            + ACCOUNT_ID
            + " = "
            + accountId);

    // Assert
    StatementExecutionResult result =
        clientService.executeStatement(
            "SELECT * FROM " + ACCOUNT_TABLE + " WHERE " + ACCOUNT_ID + " = " + accountId);
    assertThat(result.getResult().isPresent()).isTrue();
    assertThat(jacksonSerDe.deserialize(result.getResult().get())).isEqualTo(expected);
  }

  @Test
  public void
      executeStatement_UpdateSqlWithIndexKeyForExistingMultipleRecordsGiven_ShouldUpdateRecords()
          throws IOException {
    // Arrange
    populateAccountRecords();
    int accountType = 1;
    int expected = 100;
    ImmutableList<JsonNode> expectedRecords =
        IntStream.range(0, NUM_ACCOUNTS)
            .filter(i -> i % NUM_TYPES == accountType)
            .mapToObj(i -> createAccountJson(i, expected))
            .collect(ImmutableList.toImmutableList());

    // Act
    clientService.executeStatement(
        "UPDATE "
            + ACCOUNT_TABLE
            + " SET "
            + BALANCE
            + " = "
            + expected
            + " WHERE "
            + ACCOUNT_TYPE
            + " = "
            + accountType);

    // Assert
    StatementExecutionResult result =
        clientService.executeStatement(
            "SELECT * FROM " + ACCOUNT_TABLE + " WHERE " + ACCOUNT_TYPE + " = " + accountType);
    assertThat(result.getResult().isPresent()).isTrue();
    assertRecords(jacksonSerDe.deserialize(result.getResult().get()), expectedRecords);
  }

  @Test
  public void executeStatement_UpdateSqlWithNonExistingTableGiven_ShouldThrowClientException() {
    // Arrange Act Assert
    assertThatThrownBy(() -> clientService.executeStatement("UPDATE foo SET a = 10"))
        .isInstanceOf(ClientException.class)
        .hasMessageContaining(Constants.TABLE_NOT_EXIST);
  }

  @Test
  public void executeStatement_MetadataQuerySqlWithTableNameGiven_ShouldReturnSingleTable() {
    // Arrange Act
    StatementExecutionResult result =
        clientService.executeStatement(
            "SELECT * FROM "
                + INFORMATION_SCHEMA_TABLES
                + " WHERE "
                + TABLE_NAME
                + " = '"
                + ACCOUNT_TABLE
                + "'");

    // Assert
    assertThat(result.getResult().isPresent()).isTrue();
    JsonNode tables = jacksonSerDe.deserialize(result.getResult().get());
    assertThat(tables.isArray()).isTrue();
    assertThat(tables.size()).isEqualTo(1);
    assertThat(tables.get(0).get(Constants.TABLE_NAME).asText()).isEqualTo(ACCOUNT_TABLE);
  }

  @Test
  public void executeStatement_MetadataQuerySqlWithoutTableNameGiven_ShouldReturnAllTables() {
    // Arrange Act
    StatementExecutionResult result =
        clientService.executeStatement("SELECT * FROM " + INFORMATION_SCHEMA_TABLES);

    // Assert
    assertThat(result.getResult().isPresent()).isTrue();
    JsonNode tables = jacksonSerDe.deserialize(result.getResult().get());
    assertThat(tables.isArray()).isTrue();
    assertThat(tables.size()).isEqualTo(2);
    assertThat(tables.get(0).get(Constants.TABLE_NAME).asText()).isEqualTo(ACCOUNT_TABLE);
    assertThat(tables.get(1).get(Constants.TABLE_NAME).asText()).isEqualTo(PAYMENT_TABLE);
  }

  @Test
  public void
      executeStatement_MetadataQuerySqlWithNonExistingTableGiven_ShouldThrowClientException() {
    // Arrange Act Assert
    assertThatThrownBy(
            () ->
                clientService.executeStatement(
                    "SELECT * FROM "
                        + INFORMATION_SCHEMA_TABLES
                        + " WHERE "
                        + TABLE_NAME
                        + " = 'foo'"))
        .isInstanceOf(ClientException.class)
        .hasMessageContaining(Constants.TABLE_NOT_EXIST);
  }

  @Test
  public void
      executeStatement_MetadataQuerySqlWithProjectionGiven_ShouldThrowIllegalArgumentException() {
    // Arrange Act Assert
    assertThatThrownBy(
            () -> clientService.executeStatement("SELECT name FROM " + INFORMATION_SCHEMA_TABLES))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            TableStoreClientError.PROJECTION_NOT_SUPPORTED_FOR_INFORMATION_SCHEMA_QUERY
                .buildCode());
  }

  @Test
  public void executeStatement_SelectHistorySqlWithoutLimitGiven_ShouldReturnAllHistories()
      throws IOException {
    // Arrange
    populateAccountRecords();
    int accountId = 1;
    int updatedValue1 = 100;
    int updatedValue2 = 200;
    updateAccountRecord(accountId, updatedValue1);
    updateAccountRecord(accountId, updatedValue2);

    // Act
    StatementExecutionResult result =
        clientService.executeStatement(
            "SELECT history() FROM " + ACCOUNT_TABLE + " WHERE " + ACCOUNT_ID + " = " + accountId);

    // Assert
    assertThat(result.getResult().isPresent()).isTrue();
    JsonNode tables = jacksonSerDe.deserialize(result.getResult().get());
    assertThat(tables.isArray()).isTrue();
    assertThat(tables.size()).isEqualTo(3);
    assertThat(tables.get(0).get(Constants.HISTORY_ASSET_AGE).asInt()).isEqualTo(2);
    assertThat(tables.get(0).get(Constants.RECORD_VALUES).get(BALANCE).asInt())
        .isEqualTo(updatedValue2);
    assertThat(tables.get(1).get(Constants.HISTORY_ASSET_AGE).asInt()).isEqualTo(1);
    assertThat(tables.get(1).get(Constants.RECORD_VALUES).get(BALANCE).asInt())
        .isEqualTo(updatedValue1);
    assertThat(tables.get(2).get(Constants.HISTORY_ASSET_AGE).asInt()).isEqualTo(0);
    assertThat(tables.get(2).get(Constants.RECORD_VALUES).get(BALANCE).asInt())
        .isEqualTo(INITIAL_BALANCE);
  }

  @Test
  public void executeStatement_SelectHistorySqlWithLimitGiven_ShouldReturnLimitedHistories()
      throws IOException {
    // Arrange
    populateAccountRecords();
    int accountId = 1;
    int updatedValue1 = 100;
    int updatedValue2 = 200;
    updateAccountRecord(accountId, updatedValue1);
    updateAccountRecord(accountId, updatedValue2);

    // Act
    StatementExecutionResult result =
        clientService.executeStatement(
            "SELECT history() FROM "
                + ACCOUNT_TABLE
                + " WHERE "
                + ACCOUNT_ID
                + " = "
                + accountId
                + " LIMIT 2");

    // Assert
    assertThat(result.getResult().isPresent()).isTrue();
    JsonNode tables = jacksonSerDe.deserialize(result.getResult().get());
    assertThat(tables.isArray()).isTrue();
    assertThat(tables.size()).isEqualTo(2);
    assertThat(tables.get(0).get(Constants.HISTORY_ASSET_AGE).asInt()).isEqualTo(2);
    assertThat(tables.get(0).get(Constants.RECORD_VALUES).get(BALANCE).asInt())
        .isEqualTo(updatedValue2);
    assertThat(tables.get(1).get(Constants.HISTORY_ASSET_AGE).asInt()).isEqualTo(1);
    assertThat(tables.get(1).get(Constants.RECORD_VALUES).get(BALANCE).asInt())
        .isEqualTo(updatedValue1);
  }

  @Test
  public void executeStatement_SelectHistorySqlWithNonExistingRecordGiven_ShouldReturnEmpty()
      throws IOException {
    // Arrange
    populateAccountRecords();
    int accountId = NUM_ACCOUNTS;
    JsonNode expected = mapper.createArrayNode();

    // Act
    StatementExecutionResult result =
        clientService.executeStatement(
            "SELECT history() FROM " + ACCOUNT_TABLE + " WHERE " + ACCOUNT_ID + " = " + accountId);

    // Assert
    assertThat(result.getResult().isPresent()).isTrue();
    assertThat(jacksonSerDe.deserialize(result.getResult().get())).isEqualTo(expected);
  }

  @Test
  public void
      executeStatement_SelectHistorySqlWithNonExistingTableGiven_ShouldThrowClientException() {
    // Arrange Act Assert
    assertThatThrownBy(
            () ->
                clientService.executeStatement(
                    "SELECT history() FROM foo WHERE " + ACCOUNT_ID + " = 0"))
        .isInstanceOf(ClientException.class)
        .hasMessageContaining(Constants.TABLE_NOT_EXIST);
  }

  @Test
  public void validateTableSchema_TableNameGiven_ShouldReturnCorrectResult() {
    // Arrange Act
    LedgerValidationResult actual = clientService.validateTableSchema(ACCOUNT_TABLE);

    // Assert
    assertThat(actual.getCode()).isEqualTo(StatusCode.OK);
    assertThat(actual.getLedgerProof().isPresent()).isTrue();
    assertThat(actual.getLedgerProof().get().getId())
        .isEqualTo(Constants.PREFIX_TABLE + ACCOUNT_TABLE);
    assertThat(actual.getLedgerProof().get().getAge()).isEqualTo(0);
    assertThat(actual.getAuditorProof().isPresent()).isFalse();
  }

  @Test
  public void validateRecord_JsonValueGiven_ShouldReturnCorrectResult() {
    // Arrange
    double value = 1.0;
    insertAccountRecord(1, INITIAL_BALANCE);
    String expected =
        Constants.PREFIX_RECORD
            + String.join(
                Constants.ASSET_ID_SEPARATOR, ImmutableList.of(ACCOUNT_TABLE, ACCOUNT_ID, "1"));

    // Act
    LedgerValidationResult actual =
        clientService.validateRecord(ACCOUNT_TABLE, ACCOUNT_ID, Json.createValue(value));

    // Assert
    assertThat(actual.getCode()).isEqualTo(StatusCode.OK);
    assertThat(actual.getLedgerProof().isPresent()).isTrue();
    assertThat(actual.getLedgerProof().get().getId()).isEqualTo(expected);
    assertThat(actual.getLedgerProof().get().getAge()).isEqualTo(0);
    assertThat(actual.getAuditorProof().isPresent()).isFalse();
  }

  @Test
  public void validateRecord_ValueNodeGiven_ShouldReturnCorrectResult() {
    // Arrange
    int value = 1;
    insertAccountRecord(1, INITIAL_BALANCE);
    String expected =
        Constants.PREFIX_RECORD
            + String.join(
                Constants.ASSET_ID_SEPARATOR, ImmutableList.of(ACCOUNT_TABLE, ACCOUNT_ID, "1"));

    // Act
    LedgerValidationResult actual =
        clientService.validateRecord(ACCOUNT_TABLE, ACCOUNT_ID, IntNode.valueOf(value));

    // Assert
    assertThat(actual.getCode()).isEqualTo(StatusCode.OK);
    assertThat(actual.getLedgerProof().isPresent()).isTrue();
    assertThat(actual.getLedgerProof().get().getId()).isEqualTo(expected);
    assertThat(actual.getLedgerProof().get().getAge()).isEqualTo(0);
    assertThat(actual.getAuditorProof().isPresent()).isFalse();
  }

  @Test
  public void validateRecord_StringGiven_ShouldReturnCorrectResult() {
    // Arrange
    String value = "1";
    insertAccountRecord(1, INITIAL_BALANCE);
    String expected =
        Constants.PREFIX_RECORD
            + String.join(
                Constants.ASSET_ID_SEPARATOR, ImmutableList.of(ACCOUNT_TABLE, ACCOUNT_ID, value));

    // Act
    LedgerValidationResult actual = clientService.validateRecord(ACCOUNT_TABLE, ACCOUNT_ID, value);

    // Assert
    assertThat(actual.getCode()).isEqualTo(StatusCode.OK);
    assertThat(actual.getLedgerProof().isPresent()).isTrue();
    assertThat(actual.getLedgerProof().get().getId()).isEqualTo(expected);
    assertThat(actual.getLedgerProof().get().getAge()).isEqualTo(0);
    assertThat(actual.getAuditorProof().isPresent()).isFalse();
  }

  @Test
  public void validateIndexRecord_JsonValueGiven_ShouldReturnCorrectResult() {
    // Arrange
    double accountType = 1.0;
    insertAccountRecord(1, INITIAL_BALANCE);
    insertAccountRecord(5, INITIAL_BALANCE); // Same account type (1 % 4 = 1, 5 % 4 = 1)
    String expected =
        Constants.PREFIX_INDEX
            + String.join(
                Constants.ASSET_ID_SEPARATOR, ImmutableList.of(ACCOUNT_TABLE, ACCOUNT_TYPE, "1"));

    // Act
    LedgerValidationResult actual =
        clientService.validateIndexRecord(
            ACCOUNT_TABLE, ACCOUNT_TYPE, Json.createValue(accountType));

    // Assert
    assertThat(actual.getCode()).isEqualTo(StatusCode.OK);
    assertThat(actual.getLedgerProof().isPresent()).isTrue();
    assertThat(actual.getLedgerProof().get().getId()).isEqualTo(expected);
    assertThat(actual.getLedgerProof().get().getAge()).isEqualTo(1);
    assertThat(actual.getAuditorProof().isPresent()).isFalse();
  }

  @Test
  public void validateIndexRecord_ValueNodeGiven_ShouldReturnCorrectResult() {
    // Arrange
    int accountType = 2;
    insertAccountRecord(2, INITIAL_BALANCE);
    insertAccountRecord(6, INITIAL_BALANCE); // Same account type (2 % 4 = 2, 6 % 4 = 2)
    String expected =
        Constants.PREFIX_INDEX
            + String.join(
                Constants.ASSET_ID_SEPARATOR, ImmutableList.of(ACCOUNT_TABLE, ACCOUNT_TYPE, "2"));

    // Act
    LedgerValidationResult actual =
        clientService.validateIndexRecord(
            ACCOUNT_TABLE, ACCOUNT_TYPE, IntNode.valueOf(accountType));

    // Assert
    assertThat(actual.getCode()).isEqualTo(StatusCode.OK);
    assertThat(actual.getLedgerProof().isPresent()).isTrue();
    assertThat(actual.getLedgerProof().get().getId()).isEqualTo(expected);
    assertThat(actual.getLedgerProof().get().getAge()).isEqualTo(1);
    assertThat(actual.getAuditorProof().isPresent()).isFalse();
  }

  @Test
  public void validateIndexRecord_StringGiven_ShouldReturnCorrectResult() {
    // Arrange
    String accountType = "3";
    insertAccountRecord(3, INITIAL_BALANCE);
    insertAccountRecord(7, INITIAL_BALANCE); // Same account type (3 % 4 = 3, 7 % 4 = 3)
    String expected =
        Constants.PREFIX_INDEX
            + String.join(
                Constants.ASSET_ID_SEPARATOR,
                ImmutableList.of(ACCOUNT_TABLE, ACCOUNT_TYPE, accountType));

    // Act
    LedgerValidationResult actual =
        clientService.validateIndexRecord(ACCOUNT_TABLE, ACCOUNT_TYPE, accountType);

    // Assert
    assertThat(actual.getCode()).isEqualTo(StatusCode.OK);
    assertThat(actual.getLedgerProof().isPresent()).isTrue();
    assertThat(actual.getLedgerProof().get().getId()).isEqualTo(expected);
    assertThat(actual.getLedgerProof().get().getAge()).isEqualTo(1);
    assertThat(actual.getAuditorProof().isPresent()).isFalse();
  }
}
