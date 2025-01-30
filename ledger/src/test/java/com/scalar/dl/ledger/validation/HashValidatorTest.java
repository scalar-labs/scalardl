package com.scalar.dl.ledger.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.scalar.dl.ledger.asset.AssetHasher;
import com.scalar.dl.ledger.contract.ContractMachine;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class HashValidatorTest {
  private static final String ID = "id";
  private static final int AGE = 1;
  private static final String INPUT = "input";
  private static final String DATA = "data";
  private static final String CONTRACT_ID = "contract_id";
  private static final String CONTRACT_ARGUMENT = "contract_argument";
  private static final byte[] SIGNATURE = "signature".getBytes(StandardCharsets.UTF_8);
  private static final byte[] PREV_HASH = "prevHash".getBytes(StandardCharsets.UTF_8);
  @Mock private Ledger<?> ledger;
  @Mock private ContractMachine contract;
  @InjectMocks private HashValidator validator;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  private InternalAsset createAssetMock(
      String id,
      int age,
      String input,
      String data,
      String contractId,
      String contractArgument,
      byte[] signature,
      byte[] prevHash,
      byte[] hash) {
    InternalAsset asset = mock(InternalAsset.class);
    when(asset.id()).thenReturn(id);
    when(asset.age()).thenReturn(age);
    when(asset.input()).thenReturn(input);
    when(asset.data()).thenReturn(data);
    when(asset.contractId()).thenReturn(contractId);
    when(asset.argument()).thenReturn(contractArgument);
    when(asset.signature()).thenReturn(signature);
    when(asset.prevHash()).thenReturn(prevHash);
    when(asset.hash()).thenReturn(hash);
    return asset;
  }

  private byte[] createHash(
      String id,
      int age,
      String input,
      String data,
      String contractId,
      String contractArgument,
      byte[] signature,
      byte[] prevHash) {
    return new AssetHasher.Builder()
        .id(id)
        .age(age)
        .input(input)
        .output(data)
        .contractId(contractId)
        .argument(contractArgument)
        .signature(signature)
        .prevHash(prevHash)
        .build()
        .get();
  }

  @Test
  public void validate_CorrectAssetGiven_ShouldReturnOK() {
    // Arrange
    byte[] hash =
        createHash(ID, AGE, INPUT, DATA, CONTRACT_ID, CONTRACT_ARGUMENT, SIGNATURE, PREV_HASH);
    InternalAsset asset =
        createAssetMock(
            ID, AGE, INPUT, DATA, CONTRACT_ID, CONTRACT_ARGUMENT, SIGNATURE, PREV_HASH, hash);

    // Act
    StatusCode result = validator.validate(ledger, contract, asset);

    // Assert
    assertThat(result).isEqualTo(StatusCode.OK);
  }

  @Test
  public void validate_AssetWithTamperedIdGiven_ShouldReturnInvalid() {
    // Arrange
    byte[] hash =
        createHash(ID, AGE, INPUT, DATA, CONTRACT_ID, CONTRACT_ARGUMENT, SIGNATURE, PREV_HASH);
    String tampered = ID + "x";
    InternalAsset asset =
        createAssetMock(
            tampered, AGE, INPUT, DATA, CONTRACT_ID, CONTRACT_ARGUMENT, SIGNATURE, PREV_HASH, hash);

    // Act
    StatusCode result = validator.validate(ledger, contract, asset);

    // Assert
    assertThat(result).isEqualTo(StatusCode.INVALID_HASH);
  }

  @Test
  public void validate_AssetWithTamperedAgeGiven_ShouldReturnInvalid() {
    // Arrange
    byte[] hash =
        createHash(ID, AGE, INPUT, DATA, CONTRACT_ID, CONTRACT_ARGUMENT, SIGNATURE, PREV_HASH);
    int tampered = AGE + 1;
    InternalAsset asset =
        createAssetMock(
            ID, tampered, INPUT, DATA, CONTRACT_ID, CONTRACT_ARGUMENT, SIGNATURE, PREV_HASH, hash);

    // Act
    StatusCode result = validator.validate(ledger, contract, asset);

    // Assert
    assertThat(result).isEqualTo(StatusCode.INVALID_HASH);
  }

  @Test
  public void validate_AssetWithTamperedInputGiven_ShouldReturnInvalid() {
    // Arrange
    byte[] hash =
        createHash(ID, AGE, INPUT, DATA, CONTRACT_ID, CONTRACT_ARGUMENT, SIGNATURE, PREV_HASH);
    JsonObject tampered = Json.createObjectBuilder().add("x", 1).build();
    InternalAsset asset =
        createAssetMock(
            ID,
            AGE,
            tampered.toString(),
            DATA,
            CONTRACT_ID,
            CONTRACT_ARGUMENT,
            SIGNATURE,
            PREV_HASH,
            hash);

    // Act
    StatusCode result = validator.validate(ledger, contract, asset);

    // Assert
    assertThat(result).isEqualTo(StatusCode.INVALID_HASH);
  }

  @Test
  public void validate_AssetWithTamperedDataGiven_ShouldReturnInvalid() {
    // Arrange
    byte[] hash =
        createHash(ID, AGE, INPUT, DATA, CONTRACT_ID, CONTRACT_ARGUMENT, SIGNATURE, PREV_HASH);
    JsonObject tampered = Json.createObjectBuilder().add("x", 1).build();
    InternalAsset asset =
        createAssetMock(
            ID,
            AGE,
            INPUT,
            tampered.toString(),
            CONTRACT_ID,
            CONTRACT_ARGUMENT,
            SIGNATURE,
            PREV_HASH,
            hash);

    // Act
    StatusCode result = validator.validate(ledger, contract, asset);

    // Assert
    assertThat(result).isEqualTo(StatusCode.INVALID_HASH);
  }

  @Test
  public void validate_AssetWithTamperedContractIdGiven_ShouldReturnInvalid() {
    // Arrange
    byte[] hash =
        createHash(ID, AGE, INPUT, DATA, CONTRACT_ID, CONTRACT_ARGUMENT, SIGNATURE, PREV_HASH);
    String tampered = CONTRACT_ID + "x";
    InternalAsset asset =
        createAssetMock(
            ID, AGE, INPUT, DATA, tampered, CONTRACT_ARGUMENT, SIGNATURE, PREV_HASH, hash);

    // Act
    StatusCode result = validator.validate(ledger, contract, asset);

    // Assert
    assertThat(result).isEqualTo(StatusCode.INVALID_HASH);
  }

  @Test
  public void validate_AssetWithTamperedArgumentGiven_ShouldReturnInvalid() {
    // Arrange
    byte[] hash =
        createHash(ID, AGE, INPUT, DATA, CONTRACT_ID, CONTRACT_ARGUMENT, SIGNATURE, PREV_HASH);
    String tampered = CONTRACT_ARGUMENT + "x";
    InternalAsset asset =
        createAssetMock(ID, AGE, INPUT, DATA, CONTRACT_ID, tampered, SIGNATURE, PREV_HASH, hash);

    // Act
    StatusCode result = validator.validate(ledger, contract, asset);

    // Assert
    assertThat(result).isEqualTo(StatusCode.INVALID_HASH);
  }

  @Test
  public void validate_AssetWithTamperedSignatureGiven_ShouldReturnInvalid() {
    // Arrange
    byte[] hash =
        createHash(ID, AGE, INPUT, DATA, CONTRACT_ID, CONTRACT_ARGUMENT, SIGNATURE, PREV_HASH);
    byte[] tampered = SIGNATURE.clone();
    tampered[0] = 'a';
    InternalAsset asset =
        createAssetMock(
            ID, AGE, INPUT, DATA, CONTRACT_ID, CONTRACT_ARGUMENT, tampered, PREV_HASH, hash);

    // Act
    StatusCode result = validator.validate(ledger, contract, asset);

    // Assert
    assertThat(result).isEqualTo(StatusCode.INVALID_HASH);
  }

  @Test
  public void validate_AssetWithTamperedPrevHashGiven_ShouldReturnInvalid() {
    // Arrange
    byte[] hash =
        createHash(ID, AGE, INPUT, DATA, CONTRACT_ID, CONTRACT_ARGUMENT, SIGNATURE, PREV_HASH);
    byte[] tampered = PREV_HASH.clone();
    tampered[0] = 'a';
    InternalAsset asset =
        createAssetMock(
            ID, AGE, INPUT, DATA, CONTRACT_ID, CONTRACT_ARGUMENT, SIGNATURE, tampered, hash);

    // Act
    StatusCode result = validator.validate(ledger, contract, asset);

    // Assert
    assertThat(result).isEqualTo(StatusCode.INVALID_HASH);
  }

  @Test
  public void validate_AssetWithTamperedHashGiven_ShouldReturnInvalid() {
    // Arrange
    byte[] hash =
        createHash(ID, AGE, INPUT, DATA, CONTRACT_ID, CONTRACT_ARGUMENT, SIGNATURE, PREV_HASH);
    hash[0] = 'a';
    byte[] tampered = hash;
    InternalAsset asset =
        createAssetMock(
            ID, AGE, INPUT, DATA, CONTRACT_ID, CONTRACT_ARGUMENT, SIGNATURE, PREV_HASH, tampered);

    // Act
    StatusCode result = validator.validate(ledger, contract, asset);

    // Assert
    assertThat(result).isEqualTo(StatusCode.INVALID_HASH);
  }
}
