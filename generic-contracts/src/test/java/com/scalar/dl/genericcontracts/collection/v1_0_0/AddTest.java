package com.scalar.dl.genericcontracts.collection.v1_0_0;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

public class AddTest {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String SOME_COLLECTION_ID = "set";
  private static final String SOME_COLLECTION_ID_WITH_PREFIX =
      Constants.COLLECTION_ID_PREFIX + SOME_COLLECTION_ID;
  private static final int SOME_CHECKPOINT_AGE = 10;
  private static final int SOME_AGE_RIGHT_BEFORE_CHECKPOINT = 9;
  private static final ArrayNode SOME_CURRENT_OBJECT_IDS_ARRAY =
      mapper.createArrayNode().add("foo").add("bar");
  private static final ArrayNode SOME_NEW_OBJECT_IDS_ARRAY =
      mapper.createArrayNode().add("baz").add("qux");
  private static final JsonNode SOME_FORCED_OPTION_TRUE =
      mapper.createObjectNode().put(Constants.OPTION_FORCE, true);
  private static final JsonNode SOME_CURRENT_OBJECT_IDS =
      mapper.createObjectNode().set(Constants.OBJECT_IDS, SOME_CURRENT_OBJECT_IDS_ARRAY);
  private static final JsonNode SOME_CHECKPOINT_INTERVAL =
      mapper
          .createObjectNode()
          .put(
              Constants.COLLECTION_CHECKPOINT_INTERVAL,
              Constants.DEFAULT_COLLECTION_CHECKPOINT_INTERVAL);

  @Spy private Add add = new Add();
  @Mock private Ledger<JsonNode> ledger;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this).close();
  }

  @Test
  public void invoke_CorrectArgumentsGivenAndCheckpointNotNeeded_ShouldAddObjectsWithoutSnapshot() {
    // Arrange
    ObjectNode argument =
        mapper
            .createObjectNode()
            .put(Constants.COLLECTION_ID, SOME_COLLECTION_ID)
            .set(Constants.OBJECT_IDS, SOME_NEW_OBJECT_IDS_ARRAY);
    ObjectNode expectedToBePut =
        mapper
            .createObjectNode()
            .put(Constants.COLLECTION_ID, SOME_COLLECTION_ID)
            .put(Constants.OPERATION_TYPE, Constants.OPERATION_ADD)
            .set(Constants.OBJECT_IDS, SOME_NEW_OBJECT_IDS_ARRAY);
    Asset<JsonNode> asset = (Asset<JsonNode>) mock(Asset.class);
    when(ledger.get(SOME_COLLECTION_ID_WITH_PREFIX)).thenReturn(Optional.of(asset));
    when(asset.age()).thenReturn(SOME_CHECKPOINT_AGE);
    doReturn(SOME_CHECKPOINT_INTERVAL)
        .when(add)
        .invokeSubContract(Constants.CONTRACT_GET_CHECKPOINT_INTERVAL, ledger, argument);
    doReturn(SOME_CURRENT_OBJECT_IDS)
        .when(add)
        .invokeSubContract(Constants.CONTRACT_GET, ledger, argument);

    // Act
    JsonNode actual = add.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNull();
    verify(ledger).get(SOME_COLLECTION_ID_WITH_PREFIX);
    verify(ledger).put(SOME_COLLECTION_ID_WITH_PREFIX, expectedToBePut);
  }

  @Test
  public void
      invoke_CorrectArgumentsWithForcedOptionGivenAndCheckpointNeeded_ShouldAddObjectsWithSnapshot() {
    // Arrange
    ObjectNode argument = mapper.createObjectNode();
    argument.put(Constants.COLLECTION_ID, SOME_COLLECTION_ID);
    argument.set(Constants.OBJECT_IDS, SOME_NEW_OBJECT_IDS_ARRAY);
    argument.set(Constants.OPTIONS, SOME_FORCED_OPTION_TRUE);
    ObjectNode expectedToBePut = mapper.createObjectNode();
    expectedToBePut.put(Constants.COLLECTION_ID, SOME_COLLECTION_ID);
    expectedToBePut.put(Constants.OPERATION_TYPE, Constants.OPERATION_ADD);
    expectedToBePut.set(Constants.OBJECT_IDS, SOME_NEW_OBJECT_IDS_ARRAY);
    expectedToBePut.set(Constants.COLLECTION_SNAPSHOT, SOME_CURRENT_OBJECT_IDS_ARRAY);
    expectedToBePut.put(
        Constants.COLLECTION_CHECKPOINT_INTERVAL, Constants.DEFAULT_COLLECTION_CHECKPOINT_INTERVAL);
    Asset<JsonNode> asset = (Asset<JsonNode>) mock(Asset.class);
    when(ledger.get(SOME_COLLECTION_ID_WITH_PREFIX)).thenReturn(Optional.of(asset));
    when(asset.age()).thenReturn(SOME_AGE_RIGHT_BEFORE_CHECKPOINT);
    doReturn(SOME_CHECKPOINT_INTERVAL)
        .when(add)
        .invokeSubContract(Constants.CONTRACT_GET_CHECKPOINT_INTERVAL, ledger, argument);
    doReturn(SOME_CURRENT_OBJECT_IDS)
        .when(add)
        .invokeSubContract(Constants.CONTRACT_GET, ledger, argument);

    // Act
    JsonNode actual = add.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNull();
    verify(ledger).get(SOME_COLLECTION_ID_WITH_PREFIX);
    verify(ledger).put(SOME_COLLECTION_ID_WITH_PREFIX, expectedToBePut);
  }

  @Test
  public void invoke_ArgumentsWithoutCollectionIdGiven_ShouldThrowContractContextException() {
    // Arrange
    ObjectNode argument =
        mapper.createObjectNode().set(Constants.OBJECT_IDS, SOME_NEW_OBJECT_IDS_ARRAY);

    // Act Assert
    assertThatThrownBy(() -> add.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.COLLECTION_ID_IS_MISSING_OR_INVALID);
    verify(ledger, never()).get(any());
    verify(ledger, never()).put(any(), any());
  }

  @Test
  public void invoke_ArgumentsWithoutObjectIdsGiven_ShouldThrowContractContextException() {
    // Arrange
    ObjectNode argument =
        mapper.createObjectNode().put(Constants.COLLECTION_ID, SOME_COLLECTION_ID);

    // Act Assert
    assertThatThrownBy(() -> add.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.OBJECT_IDS_ARE_MISSING);
    verify(ledger, never()).get(any());
    verify(ledger, never()).put(any(), any());
  }

  @Test
  public void invoke_ArgumentsWithInvalidObjectIdsGiven_ShouldThrowContractContextException() {
    // Arrange
    ObjectNode argument1 =
        mapper
            .createObjectNode()
            .put(Constants.COLLECTION_ID, SOME_COLLECTION_ID)
            .put(Constants.OBJECT_IDS, 1);
    ObjectNode argument2 =
        mapper
            .createObjectNode()
            .put(Constants.COLLECTION_ID, SOME_COLLECTION_ID)
            .set(Constants.OBJECT_IDS, mapper.createArrayNode().add("foo").add(1));

    // Act Assert
    validateObjectIdsAndAssert(ledger, argument1);
    validateObjectIdsAndAssert(ledger, argument2);
    verify(ledger, never()).get(any());
    verify(ledger, never()).put(any(), any());
  }

  private void validateObjectIdsAndAssert(Ledger<JsonNode> ledger, JsonNode argument) {
    Assertions.assertThatThrownBy(() -> add.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_OBJECT_IDS_FORMAT);
  }

  @Test
  public void
      invoke_CorrectArgumentsGivenAndCollectionNotFound_ShouldThrowContractContextException() {
    // Arrange
    ObjectNode argument =
        mapper
            .createObjectNode()
            .put(Constants.COLLECTION_ID, SOME_COLLECTION_ID)
            .set(Constants.OBJECT_IDS, SOME_NEW_OBJECT_IDS_ARRAY);
    when(ledger.get(SOME_COLLECTION_ID_WITH_PREFIX)).thenReturn(Optional.empty());

    // Act Assert
    assertThatThrownBy(() -> add.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.COLLECTION_NOT_FOUND);
    verify(ledger, never()).put(any(), any());
  }

  @Test
  public void invoke_ArgumentsWithDuplicateObjectIdGiven_ShouldThrowContractContextException() {
    // Arrange
    ObjectNode argument =
        mapper
            .createObjectNode()
            .put(Constants.COLLECTION_ID, SOME_COLLECTION_ID)
            .set(Constants.OBJECT_IDS, mapper.createArrayNode().add("foo"));
    Asset<JsonNode> asset = (Asset<JsonNode>) mock(Asset.class);
    when(ledger.get(SOME_COLLECTION_ID_WITH_PREFIX)).thenReturn(Optional.of(asset));
    doReturn(SOME_CURRENT_OBJECT_IDS)
        .when(add)
        .invokeSubContract(Constants.CONTRACT_GET, ledger, argument);

    // Act Assert
    assertThatThrownBy(() -> add.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.OBJECT_ALREADY_EXISTS_IN_COLLECTION);
    verify(ledger, never()).put(any(), any());
  }
}
