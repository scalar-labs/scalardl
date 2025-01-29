package com.scalar.dl.ledger.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class AssetHasherTest {
  private static final String ANY_ID = "id";
  private static final int ANY_AGE = 1;
  private static final String ANY_INPUT1 =
      "{\"id1\":{\"balance\":1000},\"id2\":{\"balance\":1000}}";
  private static final String ANY_INPUT2 = "{\"id1\":{\"balance\":100},\"id2\":{\"balance\":1000}}";
  private static final String ANY_OUTPUT = "{\"balance\":1100}";
  private static final String ANY_CONTRACT_ID = "com.any.contract.AnyContract";
  private static final String ANY_ARGUMENT = "{\"asset_ids\":[\"id1\",\"id2\"],\"amount\":100}";
  private static final byte[] ANY_SIGNATURE = "signature".getBytes(StandardCharsets.UTF_8);
  private static final byte[] ANY_PREV_HASH = "prev_hash".getBytes(StandardCharsets.UTF_8);

  @Test
  public void get_SameInputsGiven_ShouldReturnSame() {
    // Arrange
    byte[] hash1 =
        new AssetHasher.Builder()
            .id(ANY_ID)
            .age(ANY_AGE)
            .input(ANY_INPUT1)
            .output(ANY_OUTPUT)
            .contractId(ANY_CONTRACT_ID)
            .argument(ANY_ARGUMENT)
            .signature(ANY_SIGNATURE)
            .prevHash(ANY_PREV_HASH)
            .build()
            .get();

    // Act
    byte[] hash2 =
        new AssetHasher.Builder()
            .id(ANY_ID)
            .age(ANY_AGE)
            .input(ANY_INPUT1)
            .output(ANY_OUTPUT)
            .contractId(ANY_CONTRACT_ID)
            .argument(ANY_ARGUMENT)
            .signature(ANY_SIGNATURE)
            .prevHash(ANY_PREV_HASH)
            .build()
            .get();

    // Assert
    assertThat(hash2).isEqualTo(hash1);
  }

  @Test
  public void get_SameInputsInDifferentOrderGiven_ShouldReturnSame() {
    // Arrange
    byte[] hash1 =
        new AssetHasher.Builder()
            .id(ANY_ID)
            .age(ANY_AGE)
            .input(ANY_INPUT1)
            .output(ANY_OUTPUT)
            .contractId(ANY_CONTRACT_ID)
            .argument(ANY_ARGUMENT)
            .signature(ANY_SIGNATURE)
            .prevHash(ANY_PREV_HASH)
            .build()
            .get();

    // Act
    byte[] hash2 =
        new AssetHasher.Builder()
            .prevHash(ANY_PREV_HASH)
            .signature(ANY_SIGNATURE)
            .argument(ANY_ARGUMENT)
            .contractId(ANY_CONTRACT_ID)
            .output(ANY_OUTPUT)
            .input(ANY_INPUT1)
            .age(ANY_AGE)
            .id(ANY_ID)
            .build()
            .get();

    // Assert
    assertThat(hash2).isEqualTo(hash1);
  }

  @Test
  public void get_DifferentInputsGiven_ShouldReturnDifferent() {
    // Arrange
    byte[] hash1 =
        new AssetHasher.Builder()
            .id(ANY_ID)
            .age(ANY_AGE)
            .input(ANY_INPUT1)
            .output(ANY_OUTPUT)
            .contractId(ANY_CONTRACT_ID)
            .argument(ANY_ARGUMENT)
            .signature(ANY_SIGNATURE)
            .prevHash(ANY_PREV_HASH)
            .build()
            .get();

    // Act
    byte[] hash2 =
        new AssetHasher.Builder()
            .id(ANY_ID)
            .age(ANY_AGE)
            .input(ANY_INPUT2)
            .output(ANY_OUTPUT)
            .contractId(ANY_CONTRACT_ID)
            .argument(ANY_ARGUMENT)
            .signature(ANY_SIGNATURE)
            .prevHash(ANY_PREV_HASH)
            .build()
            .get();

    // Assert
    assertThat(hash2).isNotEqualTo(hash1);
  }

  @Test
  public void get_InputWithoutPrevHashGiven_ShouldThrowIllegalArgumentException() {
    // Act Assert
    assertThatThrownBy(
            () ->
                new AssetHasher.Builder()
                    .id(ANY_ID)
                    .age(ANY_AGE)
                    .input(ANY_INPUT1)
                    .output(ANY_OUTPUT)
                    .contractId(ANY_CONTRACT_ID)
                    .argument(ANY_ARGUMENT)
                    .signature(ANY_SIGNATURE)
                    .build()
                    .get())
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void get_InputWithInitialAgeWithoutPrevHashGiven_ShouldCreateHashWithoutPrevHash() {
    // Act
    assertThatCode(
            () ->
                new AssetHasher.Builder()
                    .id(ANY_ID)
                    .age(0)
                    .input(ANY_INPUT1)
                    .output(ANY_OUTPUT)
                    .contractId(ANY_CONTRACT_ID)
                    .argument(ANY_ARGUMENT)
                    .signature(ANY_SIGNATURE)
                    .build()
                    .get())
        .doesNotThrowAnyException();
  }
}
