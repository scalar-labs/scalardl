package com.scalar.dl.genericcontracts.object;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Ledger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PutTest {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String SOME_OBJECT_ID = "foo";
  private static final String SOME_OBJECT_ID_WITH_PREFIX =
      Constants.OBJECT_ID_PREFIX + SOME_OBJECT_ID;
  private static final String SOME_HASH_VALUE = "bar";
  private static final JsonNode SOME_METADATA = mapper.createObjectNode().put("x", 1);

  private final Put put = new Put();

  @Mock private Ledger<JsonNode> ledger;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this).close();
  }

  @Test
  public void invoke_CorrectArgumentsGiven_ShouldPutObject() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .put(Constants.HASH_VALUE, SOME_HASH_VALUE)
            .set(Constants.METADATA, SOME_METADATA);

    // Act
    JsonNode actual = put.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNull();
    verify(ledger).get(SOME_OBJECT_ID_WITH_PREFIX);
    verify(ledger)
        .put(
            SOME_OBJECT_ID_WITH_PREFIX,
            mapper
                .createObjectNode()
                .put(Constants.OBJECT_ID, argument.get(Constants.OBJECT_ID).asText())
                .put(Constants.HASH_VALUE, argument.get(Constants.HASH_VALUE).asText())
                .set(Constants.METADATA, argument.get(Constants.METADATA)));
  }

  @Test
  public void invoke_ArgumentsWithoutObjectIdGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.HASH_VALUE, SOME_HASH_VALUE)
            .set(Constants.METADATA, SOME_METADATA);

    // Act Assert
    assertThatThrownBy(() -> put.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.OBJECT_ID_IS_MISSING_OR_INVALID);
    verify(ledger, never()).get(SOME_OBJECT_ID_WITH_PREFIX);
    verify(ledger, never()).put(eq(SOME_OBJECT_ID_WITH_PREFIX), any(JsonNode.class));
  }

  @Test
  public void invoke_ArgumentsWithoutHashValueGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .set(Constants.METADATA, SOME_METADATA);

    // Act Assert
    assertThatThrownBy(() -> put.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.HASH_VALUE_IS_MISSING_OR_INVALID);
    verify(ledger, never()).get(SOME_OBJECT_ID_WITH_PREFIX);
    verify(ledger, never()).put(eq(SOME_OBJECT_ID_WITH_PREFIX), any(JsonNode.class));
  }

  @Test
  public void invoke_ArgumentsWithInvalidMetadataGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .put(Constants.HASH_VALUE, SOME_HASH_VALUE)
            .put(Constants.METADATA, "bar");

    // Act Assert
    assertThatThrownBy(() -> put.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_METADATA_FORMAT);
    verify(ledger, never()).get(SOME_OBJECT_ID_WITH_PREFIX);
    verify(ledger, never()).put(eq(SOME_OBJECT_ID_WITH_PREFIX), any(JsonNode.class));
  }
}
