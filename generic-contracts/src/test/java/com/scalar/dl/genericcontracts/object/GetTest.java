package com.scalar.dl.genericcontracts.object;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GetTest {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String SOME_OBJECT_ID = "foo";
  private static final String SOME_OBJECT_ID_WITH_PREFIX =
      Constants.OBJECT_ID_PREFIX + SOME_OBJECT_ID;
  private static final String SOME_HASH_VALUE = "bar";
  private static final JsonNode SOME_METADATA = mapper.createObjectNode().put("x", 1);
  private static final JsonNode SOME_DATA =
      mapper
          .createObjectNode()
          .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
          .put(Constants.HASH_VALUE, SOME_HASH_VALUE)
          .set(Constants.METADATA, SOME_METADATA);

  private final Get get = new Get();

  @Mock private Ledger<JsonNode> ledger;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this).close();
  }

  @Test
  public void invoke_CorrectArgumentsGiven_ShouldGetObject() {
    // Arrange
    JsonNode argument = mapper.createObjectNode().put(Constants.OBJECT_ID, SOME_OBJECT_ID);
    Asset<JsonNode> asset = (Asset<JsonNode>) mock(Asset.class);
    Optional<Asset<JsonNode>> expected = Optional.of(asset);
    when(ledger.get(SOME_OBJECT_ID_WITH_PREFIX)).thenReturn(expected);
    when(asset.data()).thenReturn(SOME_DATA);

    // Act
    JsonNode actual = get.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isEqualTo(SOME_DATA);
    verify(ledger).get(SOME_OBJECT_ID_WITH_PREFIX);
  }

  @Test
  public void invoke_ArgumentsWithoutObjectIdGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = mapper.createObjectNode();

    // Act Assert
    assertThatThrownBy(() -> get.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.OBJECT_ID_IS_MISSING_OR_INVALID);
    verify(ledger, never()).get(SOME_OBJECT_ID_WITH_PREFIX);
  }

  @Test
  public void invoke_ArgumentsWithInvalidObjectIdTypeGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = mapper.createObjectNode().put(Constants.OBJECT_ID, 1);

    // Act Assert
    assertThatThrownBy(() -> get.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.OBJECT_ID_IS_MISSING_OR_INVALID);
    verify(ledger, never()).get(SOME_OBJECT_ID_WITH_PREFIX);
  }
}
