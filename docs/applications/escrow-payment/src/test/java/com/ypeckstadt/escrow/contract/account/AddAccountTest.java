package com.ypeckstadt.escrow.contract.account;

import static com.ypeckstadt.escrow.common.Constants.ACCOUNT_ASSET_TYPE;
import static com.ypeckstadt.escrow.common.Constants.ACCOUNT_ID;
import static com.ypeckstadt.escrow.common.Constants.ACCOUNT_NAME;
import static com.ypeckstadt.escrow.common.Constants.ACCOUNT_TIMESTAMP;
import static com.ypeckstadt.escrow.common.Constants.CONTRACT_ADD_ACCOUNT_DUPLICATE_ERROR;
import static com.ypeckstadt.escrow.common.Constants.CONTRACT_ADD_ACCOUNT_MISSING_ARGUMENTS_ERROR;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.crypto.CertificateEntry.Key;
import com.scalar.dl.ledger.database.Ledger;
import com.scalar.dl.ledger.exception.ContractContextException;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class AddAccountTest {

  private static final String MOCKED_HOLDER_ID = "mockedHolderId";

  @Mock private Ledger ledger;
  @Mock private Key mockedKey;
  @Spy private AddAccount addAccount;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(addAccount.getCertificateKey()).thenReturn(mockedKey);
    when(mockedKey.getHolderId()).thenReturn(MOCKED_HOLDER_ID);
  }

  @Test
  public void invoke_InsertNonexistentAccount_ShouldInsertAccount() {
    // Arrange
    JsonObject argument = prepareArgument();
    JsonObject properties = prepareProperties();
    mockLedgerGetCall(argument, null);

    // Act
    JsonObject invokeResult = addAccount.invoke(ledger, argument, Optional.of(properties));
    assertEquals(invokeResult, null);
  }

  @Test
  public void invoke_ArgumentsMissing_ShouldThrowContractContextException() {
    // Arrange
    JsonObject argument = Json.createObjectBuilder().build();
    JsonObject properties = prepareProperties();

    // Act
    // Assert
    assertThatThrownBy(
            () -> {
              addAccount.invoke(ledger, argument, Optional.of(properties));
            })
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(CONTRACT_ADD_ACCOUNT_MISSING_ARGUMENTS_ERROR);
  }

  @Test
  public void invoke_InsertExistentAccount_ShouldThrowContractContextException() {
    // Arrange
    JsonObject argument = prepareArgument();
    JsonObject properties = prepareProperties();
    mockLedgerGetCall(argument, mock(Asset.class));

    // Act
    // Assert
    assertThatThrownBy(
            () -> {
              addAccount.invoke(ledger, argument, Optional.of(properties));
            })
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(CONTRACT_ADD_ACCOUNT_DUPLICATE_ERROR);
  }

  private JsonObject prepareArgument() {
    return Json.createObjectBuilder()
        .add(ACCOUNT_ID, "1")
        .add(ACCOUNT_NAME, "name")
        .add(ACCOUNT_TIMESTAMP, System.currentTimeMillis())
        .build();
  }

  private JsonObject prepareProperties() {
    return Json.createObjectBuilder().build();
  }

  private void mockLedgerGetCall(JsonObject argument, Asset result) {
    when(ledger.get(getAssetId(argument))).thenReturn(Optional.ofNullable(result));
  }

  private String getAssetId(JsonObject argument) {
    return ACCOUNT_ASSET_TYPE + "_" + argument.getString(ACCOUNT_ID);
  }
}
