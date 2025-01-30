package com.scalar.dl.ledger.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.scalar.dl.ledger.contract.ContractMachine;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PrevHashValidatorTest {
  @Mock private Ledger<?> ledger;
  @Mock private ContractMachine contract;
  @InjectMocks private PrevHashValidator validator;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void validate_AssetWithCorrectPrevHashGiven_ShouldReturnOK() {
    // Arrange
    byte[] prevHash = "prevHash".getBytes(StandardCharsets.UTF_8);
    validator.initialize(prevHash);
    InternalAsset asset = mock(InternalAsset.class);
    when(asset.prevHash()).thenReturn(prevHash);

    // Act
    StatusCode result = validator.validate(ledger, contract, asset);

    // Assert
    assertThat(result).isEqualTo(StatusCode.OK);
  }

  @Test
  public void validate_AssetWithTamperedPrevHashGiven_ShouldReturnInvalid() {
    // Arrange
    byte[] prevHash = "prevHash".getBytes(StandardCharsets.UTF_8);
    validator.initialize(prevHash);
    InternalAsset asset = mock(InternalAsset.class);
    when(asset.prevHash()).thenReturn("prevHashx".getBytes(StandardCharsets.UTF_8));

    // Act
    StatusCode result = validator.validate(ledger, contract, asset);

    // Assert
    assertThat(result).isEqualTo(StatusCode.INVALID_PREV_HASH);
  }
}
