package com.scalar.dl.ledger.proof;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.dl.ledger.util.Argument;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

public class RequestProofTest {
  private static final String SOME_NONCE = "some_nonce";
  private static final String SOME_CONTRACT_ID = "some_contract_id";
  private static final String SOME_ENTITY_ID = "some_entity_id";
  private static final int SOME_KEY_VERSION = 1;
  private static final byte[] SOME_SIGNATURE = "some_signature".getBytes(StandardCharsets.UTF_8);

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void build_CorrectEntriesWithV1ArgumentGiven_ShouldBuildProperly() {
    // Arrange
    String v1Argument = JsonObject.EMPTY_JSON_OBJECT.toString();

    // Act
    RequestProof proof =
        RequestProof.newBuilder()
            .nonce(SOME_NONCE)
            .contractId(SOME_CONTRACT_ID)
            .argument(v1Argument)
            .entityId(SOME_ENTITY_ID)
            .keyVersion(SOME_KEY_VERSION)
            .signature(SOME_SIGNATURE)
            .build();

    // Assert
    assertThat(proof.getNonce()).isEqualTo(SOME_NONCE);
    assertThat(proof.getContractId()).isEqualTo(SOME_CONTRACT_ID);
    assertThat(proof.getArgument()).isEqualTo(v1Argument);
    assertThat(proof.getContractArgument()).isEqualTo(v1Argument);
    assertThat(proof.getEntityId()).isEqualTo(SOME_ENTITY_ID);
    assertThat(proof.getKeyVersion()).isEqualTo(SOME_KEY_VERSION);
    assertThat(proof.getSignature()).isEqualTo(SOME_SIGNATURE);
  }

  @Test
  public void build_CorrectEntriesWithV2ArgumentGiven_ShouldBuildProperly() {
    // Arrange
    JsonObject contractArgument = JsonObject.EMPTY_JSON_OBJECT;
    String v2Argument = Argument.format(contractArgument, SOME_NONCE, Collections.emptyList());

    // Act
    RequestProof proof =
        RequestProof.newBuilder()
            .nonce(SOME_NONCE)
            .contractId(SOME_CONTRACT_ID)
            .argument(v2Argument)
            .entityId(SOME_ENTITY_ID)
            .keyVersion(SOME_KEY_VERSION)
            .signature(SOME_SIGNATURE)
            .build();

    // Assert
    assertThat(proof.getNonce()).isEqualTo(SOME_NONCE);
    assertThat(proof.getContractId()).isEqualTo(SOME_CONTRACT_ID);
    assertThat(proof.getArgument()).isEqualTo(v2Argument);
    assertThat(proof.getContractArgument()).isEqualTo(contractArgument.toString());
    assertThat(proof.getEntityId()).isEqualTo(SOME_ENTITY_ID);
    assertThat(proof.getKeyVersion()).isEqualTo(SOME_KEY_VERSION);
    assertThat(proof.getSignature()).isEqualTo(SOME_SIGNATURE);
  }

  @Test
  public void equals_RequestProofWithSameEntriesWithV1ArgumentGiven_ShouldReturnTrue() {
    // Arrange
    String v1Argument1 = JsonObject.EMPTY_JSON_OBJECT.toString();
    RequestProof proof1 =
        RequestProof.newBuilder()
            .nonce(SOME_NONCE)
            .contractId(SOME_CONTRACT_ID)
            .argument(v1Argument1)
            .entityId(SOME_ENTITY_ID)
            .keyVersion(SOME_KEY_VERSION)
            .signature(SOME_SIGNATURE)
            .build();

    String v1Argument2 = JsonObject.EMPTY_JSON_OBJECT.toString();
    RequestProof proof2 =
        RequestProof.newBuilder()
            .nonce(SOME_NONCE)
            .contractId(SOME_CONTRACT_ID)
            .argument(v1Argument2)
            .entityId(SOME_ENTITY_ID)
            .keyVersion(SOME_KEY_VERSION)
            .signature(SOME_SIGNATURE)
            .build();

    // Act
    boolean actual = proof1.equals(proof2);

    // Assert
    assertThat(actual).isTrue();
    assertThat(proof1.hashCode()).isEqualTo(proof2.hashCode());
  }

  @Test
  public void equals_RequestProofWithDifferentEntriesWithV1ArgumentGiven_ShouldReturnFalse() {
    // Arrange
    String v1Argument1 = JsonObject.EMPTY_JSON_OBJECT.toString();
    RequestProof proof1 =
        RequestProof.newBuilder()
            .nonce(SOME_NONCE)
            .contractId(SOME_CONTRACT_ID)
            .argument(v1Argument1)
            .entityId(SOME_ENTITY_ID)
            .keyVersion(SOME_KEY_VERSION)
            .signature(SOME_SIGNATURE)
            .build();

    String v1Argument2 = Json.createObjectBuilder().add("key", "value").build().toString();
    RequestProof proof2 =
        RequestProof.newBuilder()
            .nonce(SOME_NONCE)
            .contractId(SOME_CONTRACT_ID)
            .argument(v1Argument2)
            .entityId(SOME_ENTITY_ID)
            .keyVersion(SOME_KEY_VERSION)
            .signature(SOME_SIGNATURE)
            .build();

    // Act
    boolean actual = proof1.equals(proof2);

    // Assert
    assertThat(actual).isFalse();
    assertThat(proof1.hashCode()).isNotEqualTo(proof2.hashCode());
  }

  @Test
  public void equals_RequestProofWithSameEntriesWithV2ArgumentGiven_ShouldReturnTrue() {
    // Arrange
    JsonObject contractArgument1 = JsonObject.EMPTY_JSON_OBJECT;
    String v2Argument1 = Argument.format(contractArgument1, SOME_NONCE, Collections.emptyList());
    RequestProof proof1 =
        RequestProof.newBuilder()
            .nonce(SOME_NONCE)
            .contractId(SOME_CONTRACT_ID)
            .argument(v2Argument1)
            .entityId(SOME_ENTITY_ID)
            .keyVersion(SOME_KEY_VERSION)
            .signature(SOME_SIGNATURE)
            .build();

    JsonObject contractArgument2 = JsonObject.EMPTY_JSON_OBJECT;
    String v2Argument2 = Argument.format(contractArgument2, SOME_NONCE, Collections.emptyList());
    RequestProof proof2 =
        RequestProof.newBuilder()
            .nonce(SOME_NONCE)
            .contractId(SOME_CONTRACT_ID)
            .argument(v2Argument2)
            .entityId(SOME_ENTITY_ID)
            .keyVersion(SOME_KEY_VERSION)
            .signature(SOME_SIGNATURE)
            .build();

    // Act
    boolean actual = proof1.equals(proof2);

    // Assert
    assertThat(actual).isTrue();
    assertThat(proof1.hashCode()).isEqualTo(proof2.hashCode());
  }

  @Test
  public void equals_RequestProofWithDifferentEntriesWithV2ArgumentGiven_ShouldReturnFalse() {
    // Arrange
    JsonObject contractArgument1 = JsonObject.EMPTY_JSON_OBJECT;
    String v2Argument1 =
        Argument.format(contractArgument1, SOME_NONCE + "x", Collections.emptyList());
    RequestProof proof1 =
        RequestProof.newBuilder()
            .nonce(SOME_NONCE)
            .contractId(SOME_CONTRACT_ID)
            .argument(v2Argument1)
            .entityId(SOME_ENTITY_ID)
            .keyVersion(SOME_KEY_VERSION)
            .signature(SOME_SIGNATURE)
            .build();

    JsonObject contractArgument2 = JsonObject.EMPTY_JSON_OBJECT;
    String v2Argument2 = Argument.format(contractArgument2, SOME_NONCE, Collections.emptyList());
    RequestProof proof2 =
        RequestProof.newBuilder()
            .nonce(SOME_NONCE)
            .contractId(SOME_CONTRACT_ID)
            .argument(v2Argument2)
            .entityId(SOME_ENTITY_ID)
            .keyVersion(SOME_KEY_VERSION)
            .signature(SOME_SIGNATURE)
            .build();

    // Act
    boolean actual = proof1.equals(proof2);

    // Assert
    assertThat(actual).isFalse();
    assertThat(proof1.hashCode()).isNotEqualTo(proof2.hashCode());
  }
}
