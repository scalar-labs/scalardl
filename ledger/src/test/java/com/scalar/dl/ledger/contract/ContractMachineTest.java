package com.scalar.dl.ledger.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.dl.ledger.crypto.CertificateEntry;
import com.scalar.dl.ledger.database.Ledger;
import com.scalar.dl.ledger.statemachine.DeprecatedLedger;
import com.scalar.dl.ledger.statemachine.JsonpBasedAssetLedger;
import com.scalar.dl.ledger.statemachine.StringBasedAssetLedger;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import javax.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ContractMachineTest {
  @Mock private Ledger ledger;
  @Mock private ContractManager contractManager;
  @Mock private CertificateEntry.Key certKey;
  private ContractMachine machine;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void constructor_ContractGiven_ShouldCreateDeprecatedContract() {
    // Arrange

    // Act
    machine = new ContractMachine(new TestContract());

    // Assert
    assertThat(machine.getContractBase()).isInstanceOf(DeprecatedContract.class);
  }

  @Test
  public void constructor_JsonpBasedContractGiven_ShouldKeepGivenContract() {
    // Arrange

    // Act
    machine = new ContractMachine(new TestJsonpBasedContract());

    // Assert
    assertThat(machine.getContractBase()).isInstanceOf(JsonpBasedContract.class);
  }

  @Test
  public void constructor_StringBasedContractGiven_ShouldKeepGivenContract() {
    // Arrange

    // Act
    machine = new ContractMachine(new TestStringBasedContract());

    // Assert
    assertThat(machine.getContractBase()).isInstanceOf(StringBasedContract.class);
  }

  @Test
  public void initialize_ContractBaseGiven_ShouldCallContractBaseInitialize() {
    // Arrange
    TestStringBasedContract contract = mock(TestStringBasedContract.class);
    machine = new ContractMachine(contract);

    // Act
    machine.initialize(contractManager, certKey);

    // Assert
    verify(contract).initialize(contractManager, certKey);
  }

  @Test
  public void setRoot_ContractBaseGiven_ShouldCallContractBaseSetRoot() {
    // Arrange
    TestStringBasedContract contract = mock(TestStringBasedContract.class);
    machine = new ContractMachine(contract);

    // Act
    machine.setRoot(true);

    // Assert
    verify(contract).setRoot(true);
  }

  @Test
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  public void isRoot_ContractBaseGiven_ShouldCallContractBaseIsRoot() {
    // Arrange
    TestStringBasedContract contract = mock(TestStringBasedContract.class);
    machine = new ContractMachine(contract);

    // Act
    machine.isRoot();

    // Assert
    verify(contract).isRoot();
  }

  @Test
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  public void getCertificateKey_ContractBaseGiven_ShouldCallContractBaseGetCertificateKey() {
    // Arrange
    TestStringBasedContract contract = mock(TestStringBasedContract.class);
    machine = new ContractMachine(contract);

    // Act
    machine.getClientIdentityKey();

    // Assert
    verify(contract).getClientIdentityKey();
  }

  @Test
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  public void invoke_ContractGiven_ShouldInvokeProperly() {
    // Arrange
    TestContract contract = spy(new TestContract());
    when(contract.invoke(any(Ledger.class), any(JsonObject.class), any()))
        .thenReturn(JsonObject.EMPTY_JSON_OBJECT);
    machine = new ContractMachine(contract);
    String argument = JsonObject.EMPTY_JSON_OBJECT.toString();
    DeprecatedLedger newLedger = mock(DeprecatedLedger.class);
    when(newLedger.getDeprecatedLedger()).thenReturn(ledger);

    // Act
    String result = machine.invoke(newLedger, argument, null);

    // Assert
    verify(contract).invoke(ledger, JsonObject.EMPTY_JSON_OBJECT, Optional.empty());
    assertThat(result).isEqualTo(JsonObject.EMPTY_JSON_OBJECT.toString());
  }

  @Test
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  public void invoke_JsonpBasedContractGiven_ShouldInvokeProperly() {
    // Arrange
    TestJsonpBasedContract contract = spy(new TestJsonpBasedContract());
    when(contract.invoke(any(), any(JsonObject.class), nullable(JsonObject.class)))
        .thenReturn(JsonObject.EMPTY_JSON_OBJECT);
    machine = new ContractMachine(contract);
    String argument = JsonObject.EMPTY_JSON_OBJECT.toString();
    JsonpBasedAssetLedger newLedger = mock(JsonpBasedAssetLedger.class);

    // Act
    String result = machine.invoke(newLedger, argument, null);

    // Assert
    verify(contract).invoke(newLedger, JsonObject.EMPTY_JSON_OBJECT, null);
    assertThat(result).isEqualTo(JsonObject.EMPTY_JSON_OBJECT.toString());
  }

  @Test
  public void invoke_StringBasedContractGiven_ShouldInvokeProperly() {
    // Arrange
    TestStringBasedContract contract = spy(new TestStringBasedContract());
    when(contract.invoke(any(), anyString(), nullable(String.class))).thenReturn("result");
    machine = new ContractMachine(contract);
    String argument = "x,y";
    StringBasedAssetLedger newLedger = mock(StringBasedAssetLedger.class);

    // Act
    String result = machine.invoke(newLedger, argument, null);

    // Assert
    verify(contract).invoke(newLedger, "x,y", null);
    assertThat(result).isEqualTo("result");
  }
}
