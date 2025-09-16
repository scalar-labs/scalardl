package com.scalar.dl.genericcontracts.collection.v1_0_0;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.genericcontracts.collection.Constants;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class CreateTest {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String SOME_COLLECTION_ID = "set";
  private static final String SOME_COLLECTION_ID_WITH_PREFIX =
      Constants.COLLECTION_ID_PREFIX + SOME_COLLECTION_ID;
  private static final ArrayNode SOME_OBJECT_IDS = mapper.createArrayNode().add("foo").add("bar");
  private static final JsonNode SOME_CHECKPOINT_INTERVAL =
      mapper
          .createObjectNode()
          .put(
              Constants.COLLECTION_CHECKPOINT_INTERVAL,
              Constants.DEFAULT_COLLECTION_CHECKPOINT_INTERVAL);

  @Spy private Create create = new Create();
  @Mock private Ledger<JsonNode> ledger;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this).close();
  }

  @Test
  public void invoke_CorrectArgumentsGiven_ShouldCreateCollection() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.COLLECTION_ID, SOME_COLLECTION_ID)
            .set(Constants.OBJECT_IDS, SOME_OBJECT_IDS);
    ObjectNode expectedToBePut = mapper.createObjectNode();
    expectedToBePut.put(Constants.COLLECTION_ID, SOME_COLLECTION_ID);
    expectedToBePut.put(Constants.OPERATION_TYPE, Constants.OPERATION_CREATE);
    expectedToBePut.set(Constants.OBJECT_IDS, SOME_OBJECT_IDS);
    expectedToBePut.set(Constants.COLLECTION_SNAPSHOT, mapper.createArrayNode());
    expectedToBePut.put(
        Constants.COLLECTION_CHECKPOINT_INTERVAL, Constants.DEFAULT_COLLECTION_CHECKPOINT_INTERVAL);
    when(ledger.get(SOME_COLLECTION_ID_WITH_PREFIX)).thenReturn(Optional.empty());
    doReturn(SOME_CHECKPOINT_INTERVAL)
        .when(create)
        .invokeSubContract(Constants.CONTRACT_GET_CHECKPOINT_INTERVAL, ledger, argument);

    // Act
    JsonNode actual = create.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNull();
    verify(ledger).get(SOME_COLLECTION_ID_WITH_PREFIX);
    verify(ledger).put(SOME_COLLECTION_ID_WITH_PREFIX, expectedToBePut);
  }

  @Test
  public void invoke_ExistingCollectionGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.COLLECTION_ID, SOME_COLLECTION_ID)
            .set(Constants.OBJECT_IDS, SOME_OBJECT_IDS);
    Asset<JsonNode> asset = (Asset<JsonNode>) mock(Asset.class);
    when(ledger.get(SOME_COLLECTION_ID_WITH_PREFIX)).thenReturn(Optional.of(asset));

    // Act Assert
    assertThatThrownBy(() -> create.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.COLLECTION_ALREADY_EXISTS);
    verify(ledger).get(SOME_COLLECTION_ID_WITH_PREFIX);
    verify(ledger, never()).put(eq(SOME_COLLECTION_ID_WITH_PREFIX), any(JsonNode.class));
  }

  @Test
  public void invoke_ArgumentsWithoutCollectionIdGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = mapper.createObjectNode().set(Constants.OBJECT_IDS, SOME_OBJECT_IDS);

    // Act Assert
    assertThatThrownBy(() -> create.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.COLLECTION_ID_IS_MISSING_OR_INVALID);
    verify(ledger, never()).get(SOME_COLLECTION_ID_WITH_PREFIX);
    verify(ledger, never()).put(eq(SOME_COLLECTION_ID_WITH_PREFIX), any(JsonNode.class));
  }

  @Test
  public void invoke_ArgumentsWithoutObjectIdsGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = mapper.createObjectNode().put(Constants.COLLECTION_ID, SOME_COLLECTION_ID);

    // Act Assert
    assertThatThrownBy(() -> create.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.OBJECT_IDS_ARE_MISSING);
    verify(ledger, never()).get(SOME_COLLECTION_ID_WITH_PREFIX);
    verify(ledger, never()).put(eq(SOME_COLLECTION_ID_WITH_PREFIX), any(JsonNode.class));
  }

  @Test
  public void invoke_ArgumentsWithInvalidObjectIdsGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument1 =
        mapper
            .createObjectNode()
            .put(Constants.COLLECTION_ID, SOME_COLLECTION_ID)
            .put(Constants.OBJECT_IDS, "");
    JsonNode argument2 =
        mapper
            .createObjectNode()
            .put(Constants.COLLECTION_ID, SOME_COLLECTION_ID)
            .set(Constants.OBJECT_IDS, mapper.createArrayNode().add("bar5").add(4).add(3));

    // Act Assert
    validateObjectIdsAndAssert(ledger, argument1);
    validateObjectIdsAndAssert(ledger, argument2);
    verify(ledger, never()).scan(any());
  }

  private void validateObjectIdsAndAssert(Ledger<JsonNode> ledger, JsonNode argument) {
    Assertions.assertThatThrownBy(() -> create.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_OBJECT_IDS_FORMAT);
  }
}
