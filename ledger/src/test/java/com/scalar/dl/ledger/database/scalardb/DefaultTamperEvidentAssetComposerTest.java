package com.scalar.dl.ledger.database.scalardb;

import static com.scalar.dl.ledger.statemachine.AssetInput.INPUT_FORMAT_VERSION;
import static com.scalar.dl.ledger.statemachine.AssetInput.KEY_AGE;
import static com.scalar.dl.ledger.statemachine.AssetInput.KEY_VERSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.scalar.db.api.Put;
import com.scalar.db.io.BlobValue;
import com.scalar.db.io.IntValue;
import com.scalar.db.io.TextValue;
import com.scalar.dl.ledger.database.AssetRecord;
import com.scalar.dl.ledger.database.Snapshot;
import com.scalar.dl.ledger.model.ContractExecutionRequest;
import com.scalar.dl.ledger.statemachine.AssetKey;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.util.JsonpSerDe;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class DefaultTamperEvidentAssetComposerTest {
  private static final String DEFAULT_NAMESPACE = "default";
  private static final String BASE_NAMESPACE = "scalar";
  private static final String ANY_ID1 = "id1";
  private static final String ANY_ID2 = "id2";
  private static final AssetKey ASSET_KEY1 = AssetKey.of(DEFAULT_NAMESPACE, ANY_ID1);
  private static final AssetKey ASSET_KEY2 = AssetKey.of(DEFAULT_NAMESPACE, ANY_ID2);
  private static final int ANY_AGE = 1;
  private static final String ANY_INPUT1 = "input1";
  private static final String ANY_INPUT2 = "input2";
  private static final String ANY_OUTPUT1 = "output1";
  private static final String ANY_OUTPUT2 = "output2";
  private static final String ANY_CONTRACT_ID = "contract_id";
  private static final String ANY_CONTRACT_ARGUMENT = "contract_argument";
  private static final byte[] ANY_HASH = "hash".getBytes(StandardCharsets.UTF_8);
  private static final byte[] ANY_SIGNATURE = "signature".getBytes(StandardCharsets.UTF_8);
  private static final String ANY_ENTITY_ID = "entity_id";
  private static final int ANY_CERT_VERSION = 1;
  @Mock private Snapshot snapshot;
  @Mock private ContractExecutionRequest request;
  @Mock private ScalarNamespaceResolver namespaceResolver;
  @Spy @InjectMocks private DefaultTamperEvidentAssetComposer composer;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    when(namespaceResolver.resolve(DEFAULT_NAMESPACE)).thenReturn(BASE_NAMESPACE);
  }

  private JsonObject createSingleElementJsonWith(String property, String value) {
    return Json.createObjectBuilder().add(property, value).build();
  }

  private InternalAsset configureAssetMock(String data, int age, byte[] hash) {
    InternalAsset record = mock(InternalAsset.class);
    when(record.data()).thenReturn(data);
    when(record.age()).thenReturn(age);
    when(record.hash()).thenReturn(hash);
    return record;
  }

  private ContractExecutionRequest configureContractExecutionRequestMock() {
    when(request.getContractArgument()).thenReturn(ANY_CONTRACT_ARGUMENT);
    when(request.getContractId()).thenReturn(ANY_CONTRACT_ID);
    when(request.getSignature()).thenReturn(ANY_SIGNATURE);
    when(request.getEntityId()).thenReturn(ANY_ENTITY_ID);
    when(request.getKeyVersion()).thenReturn(ANY_CERT_VERSION);
    return request;
  }

  private InternalAsset createAsset(String assetId, int age, String data) {
    return AssetRecord.newBuilder().id(assetId).age(age).data(data).build();
  }

  @Test
  public void compose_OneReadSetAndOneWriteSetForSameAssetGiven_ShouldComposeOnePut() {
    // Arrange
    JsonObject input = createSingleElementJsonWith("input", ANY_INPUT1);
    InternalAsset record = configureAssetMock(input.toString(), ANY_AGE, ANY_HASH);
    when(snapshot.getReadSet()).thenReturn(ImmutableMap.of(ASSET_KEY1, record));
    when(snapshot.getWriteSet())
        .thenReturn(ImmutableMap.of(ASSET_KEY1, createAsset(ANY_ID1, ANY_AGE + 1, ANY_OUTPUT1)));
    request = configureContractExecutionRequestMock();
    doReturn(new byte[0])
        .when(composer)
        .hashWith(
            anyString(),
            anyInt(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            any(),
            any());

    // Act
    Map<AssetKey, Put> puts = composer.compose(snapshot, request);

    // Assert
    assertThat(puts).hasSize(1);
    Put put = puts.get(ASSET_KEY1);
    JsonObject expected =
        Json.createObjectBuilder()
            .add(
                DEFAULT_NAMESPACE,
                Json.createObjectBuilder()
                    .add(ANY_ID1, Json.createObjectBuilder().add(KEY_AGE, ANY_AGE)))
            .add(KEY_VERSION, INPUT_FORMAT_VERSION)
            .build();
    assertThat(put.getValues().get(AssetAttribute.INPUT))
        .isEqualTo(new TextValue(AssetAttribute.INPUT, new JsonpSerDe().serialize(expected)));
    assertThat(put.getValues().get(AssetAttribute.OUTPUT))
        .isEqualTo(new TextValue(AssetAttribute.OUTPUT, ANY_OUTPUT1));
    assertThat(put.getClusteringKey().get().get().get(0))
        .isEqualTo(new IntValue(AssetAttribute.AGE, ANY_AGE + 1));
    assertThat(put.getValues().get(AssetAttribute.PREV_HASH))
        .isEqualTo(new BlobValue(AssetAttribute.PREV_HASH, ANY_HASH));
  }

  @Test
  public void compose_TwoReadSetAndTwoWriteSetGiven_ShouldComposeTwoMutationsWithSameInput() {
    // Arrange
    List<JsonObject> inputs =
        Arrays.asList(
            createSingleElementJsonWith("input", ANY_INPUT1),
            createSingleElementJsonWith("input", ANY_INPUT2));
    List<String> outputs = Arrays.asList(ANY_OUTPUT1, ANY_OUTPUT2);
    InternalAsset record1 = configureAssetMock(inputs.get(0).toString(), ANY_AGE, ANY_HASH);
    InternalAsset record2 = configureAssetMock(inputs.get(1).toString(), ANY_AGE, ANY_HASH);
    when(snapshot.getReadSet())
        .thenReturn(ImmutableMap.of(ASSET_KEY1, record1, ASSET_KEY2, record2));
    when(snapshot.getWriteSet())
        .thenReturn(
            ImmutableMap.of(
                ASSET_KEY1,
                createAsset(ANY_ID1, ANY_AGE + 1, outputs.get(0)),
                ASSET_KEY2,
                createAsset(ANY_ID2, ANY_AGE + 1, outputs.get(1))));
    request = configureContractExecutionRequestMock();
    doReturn(new byte[0])
        .when(composer)
        .hashWith(
            anyString(),
            anyInt(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            any(),
            any());

    // Act
    Map<AssetKey, Put> puts = composer.compose(snapshot, request);

    // Assert
    assertThat(puts).hasSize(2);
    JsonObject expectedInput =
        Json.createObjectBuilder()
            .add(
                DEFAULT_NAMESPACE,
                Json.createObjectBuilder()
                    .add(ANY_ID1, Json.createObjectBuilder().add(KEY_AGE, ANY_AGE).build())
                    .add(ANY_ID2, Json.createObjectBuilder().add(KEY_AGE, ANY_AGE).build()))
            .add(KEY_VERSION, INPUT_FORMAT_VERSION)
            .build();

    Put put1 = puts.get(ASSET_KEY1);
    assertThat(put1.getValues().get(AssetAttribute.INPUT))
        .isEqualTo(new TextValue(AssetAttribute.INPUT, new JsonpSerDe().serialize(expectedInput)));
    assertThat(put1.getValues().get(AssetAttribute.OUTPUT))
        .isEqualTo(new TextValue(AssetAttribute.OUTPUT, ANY_OUTPUT1));
    assertThat(put1.getClusteringKey().get().get().get(0))
        .isEqualTo(new IntValue(AssetAttribute.AGE, ANY_AGE + 1));
    assertThat(put1.getValues().get(AssetAttribute.PREV_HASH))
        .isEqualTo(new BlobValue(AssetAttribute.PREV_HASH, ANY_HASH));

    Put put2 = puts.get(ASSET_KEY2);
    assertThat(put2.getValues().get(AssetAttribute.INPUT))
        .isEqualTo(new TextValue(AssetAttribute.INPUT, new JsonpSerDe().serialize(expectedInput)));
    assertThat(put2.getValues().get(AssetAttribute.OUTPUT))
        .isEqualTo(new TextValue(AssetAttribute.OUTPUT, ANY_OUTPUT2));
    assertThat(put2.getClusteringKey().get().get().get(0))
        .isEqualTo(new IntValue(AssetAttribute.AGE, ANY_AGE + 1));
    assertThat(put2.getValues().get(AssetAttribute.PREV_HASH))
        .isEqualTo(new BlobValue(AssetAttribute.PREV_HASH, ANY_HASH));
  }

  @Test
  public void compose_OneWriteSetGiven_ShouldComposePutWithAgeZeroWithoutPrevHash() {
    // Arrange
    when(snapshot.getWriteSet())
        .thenReturn(ImmutableMap.of(ASSET_KEY1, createAsset(ANY_ID1, 0, ANY_OUTPUT1)));
    request = configureContractExecutionRequestMock();
    doReturn(new byte[0])
        .when(composer)
        .hashWith(
            anyString(),
            anyInt(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            any(),
            any());

    // Act
    Map<AssetKey, Put> puts = composer.compose(snapshot, request);

    // Assert
    assertThat(puts).hasSize(1);
    Put put = puts.get(ASSET_KEY1);
    JsonObject expected = Json.createObjectBuilder().build();
    assertThat(put.getValues().get(AssetAttribute.INPUT))
        .isEqualTo(new TextValue(AssetAttribute.INPUT, new JsonpSerDe().serialize(expected)));
    assertThat(put.getValues().get(AssetAttribute.OUTPUT))
        .isEqualTo(new TextValue(AssetAttribute.OUTPUT, ANY_OUTPUT1));
    assertThat(put.getClusteringKey().get().get().get(0))
        .isEqualTo(new IntValue(AssetAttribute.AGE, 0));
    assertThat(put.getValues().get(AssetAttribute.PREV_HASH))
        .isEqualTo(new BlobValue(AssetAttribute.PREV_HASH, (byte[]) null));
  }
}
