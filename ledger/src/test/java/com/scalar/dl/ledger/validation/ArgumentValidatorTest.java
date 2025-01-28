package com.scalar.dl.ledger.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.scalar.dl.ledger.contract.ContractMachine;
import com.scalar.dl.ledger.error.LedgerError;
import com.scalar.dl.ledger.exception.ValidationException;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.statemachine.Ledger;
import com.scalar.dl.ledger.util.Argument;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.json.JsonObject;
import javax.json.JsonValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ArgumentValidatorTest {
  private static final String ASSET_ID = "X";
  @Mock private Ledger<?> ledger;
  @Mock private ContractMachine contract;
  private NonceValidator validator;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    validator = new NonceValidator();
  }

  public List<InternalAsset> createMockAssetListWithV1Jsonp() {
    List<InternalAsset> result = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      JsonObject argument = Argument.format(JsonValue.EMPTY_JSON_OBJECT, Integer.toString(i));
      InternalAsset asset = mock(InternalAsset.class);
      when(asset.argument()).thenReturn(argument.toString());
      when(asset.id()).thenReturn(ASSET_ID);
      result.add(asset);
    }
    return result;
  }

  public List<InternalAsset> createMockAssetListWithV2String() {
    List<InternalAsset> result = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      String argument = Argument.format("", Integer.toString(i), Collections.emptyList());
      InternalAsset asset = mock(InternalAsset.class);
      when(asset.argument()).thenReturn(argument);
      when(asset.id()).thenReturn(ASSET_ID);
      result.add(asset);
    }
    return result;
  }

  @Test
  public void validate_NoDuplicatedNonceWithV1JsonpGiven_ShouldReturnAllTrue() {
    // Arrange
    List<InternalAsset> assets = createMockAssetListWithV1Jsonp();
    List<StatusCode> results = new ArrayList<>();

    // Act
    assets.forEach(asset -> results.add(validator.validate(ledger, contract, asset)));

    // Assert
    assertThat(results).containsOnly(StatusCode.OK);
  }

  @Test
  public void validate_DuplicateNonceWithV1JsonpGiven_ShouldReturnAFalse() {
    // Arrange
    List<InternalAsset> assets = createMockAssetListWithV1Jsonp();
    assets.add(assets.get(1)); // duplicate a nonce
    List<StatusCode> results = new ArrayList<>();

    // Act Asset
    assertThatThrownBy(
            () -> assets.forEach(asset -> results.add(validator.validate(ledger, contract, asset))))
        .isInstanceOf(ValidationException.class)
        .hasMessage(LedgerError.VALIDATION_FAILED_FOR_NONCE.buildMessage(assets.get(1).id(), "1"))
        .extracting("code")
        .isEqualTo(StatusCode.INVALID_NONCE);
  }

  @Test
  public void validate_NoDuplicatedNonceWithV2StringGiven_ShouldReturnAllTrue() {
    // Arrange
    List<InternalAsset> assets = createMockAssetListWithV2String();
    List<StatusCode> results = new ArrayList<>();

    // Act
    assets.forEach(asset -> results.add(validator.validate(ledger, contract, asset)));

    // Assert
    assertThat(results).containsOnly(StatusCode.OK);
  }

  @Test
  public void validate_DuplicateNonceWithV2StringGiven_ShouldReturnAFalse() {
    // Arrange
    List<InternalAsset> assets = createMockAssetListWithV2String();
    assets.add(assets.get(1)); // duplicate a nonce
    List<StatusCode> results = new ArrayList<>();

    // Act Asset
    assertThatThrownBy(
            () -> assets.forEach(asset -> results.add(validator.validate(ledger, contract, asset))))
        .isInstanceOf(ValidationException.class)
        .hasMessage(LedgerError.VALIDATION_FAILED_FOR_NONCE.buildMessage(assets.get(1).id(), "1"))
        .extracting("code")
        .isEqualTo(StatusCode.INVALID_NONCE);
  }
}
