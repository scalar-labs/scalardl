package com.scalar.dl.ledger.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.scalar.dl.ledger.contract.ContractMachine;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class OutputValidatorTest {
  private static final String ID = "id";
  @Mock private LedgerTracerBase<?> ledger;
  @Mock private ContractMachine contract;
  @InjectMocks private OutputValidator validator;
  private static final String ANY_RECOMPUTED = "recomputed";

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void validate_AssetWithCorrectDataGiven_ShouldReturnOk() {
    // Arrange
    when(ledger.getOutput(ID)).thenReturn(ANY_RECOMPUTED);
    InternalAsset asset = mock(InternalAsset.class);
    when(asset.id()).thenReturn(ID);
    when(asset.data()).thenReturn(ANY_RECOMPUTED);

    // Act
    StatusCode result = validator.validate(ledger, contract, asset);

    // Assert
    assertThat(result).isEqualTo(StatusCode.OK);
  }

  @Test
  public void validate_AssetWithTamperedDataGiven_ShouldReturnInvalid() {
    // Arrange
    when(ledger.getOutput(ID)).thenReturn(ANY_RECOMPUTED);
    InternalAsset asset = mock(InternalAsset.class);
    when(asset.id()).thenReturn(ID);
    JsonObject tampered = Json.createObjectBuilder().add("x", 1).build();
    when(asset.data()).thenReturn(tampered.toString());

    // Act
    StatusCode result = validator.validate(ledger, contract, asset);

    // Assert
    assertThat(result).isEqualTo(StatusCode.INVALID_OUTPUT);
  }
}
