package com.scalar.dl.client.contract;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.dl.client.Constants;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GetObjectTest {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String SOME_OBJECT_ID = "foo";
  private static final String SOME_HASH_VALUE = "bar";
  private static final JsonNode SOME_PROPERTIES = mapper.createObjectNode().put("x", 1);
  private static final JsonNode SOME_DATA =
      mapper
          .createObjectNode()
          .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
          .put(Constants.HASH_VALUE, SOME_HASH_VALUE)
          .set(Constants.PROPERTIES, SOME_PROPERTIES);

  private final GetObject getObject = new GetObject();

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
    when(ledger.get(SOME_OBJECT_ID)).thenReturn(expected);
    when(asset.data()).thenReturn(SOME_DATA);

    // Act
    JsonNode actual = getObject.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isEqualTo(SOME_DATA);
    verify(ledger).get(SOME_OBJECT_ID);
  }

  @Test
  public void invoke_ArgumentsWithoutObjectIdGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = mapper.createObjectNode();

    // Act Assert
    assertThatThrownBy(() -> getObject.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.OBJECT_ID_IS_MISSING_OR_INVALID);
    verify(ledger, never()).get(SOME_OBJECT_ID);
  }

  @Test
  public void invoke_ArgumentsWithInvalidObjectIdTypeGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = mapper.createObjectNode().put(Constants.OBJECT_ID, 1);

    // Act Assert
    assertThatThrownBy(() -> getObject.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.OBJECT_ID_IS_MISSING_OR_INVALID);
    verify(ledger, never()).get(SOME_OBJECT_ID);
  }
}
