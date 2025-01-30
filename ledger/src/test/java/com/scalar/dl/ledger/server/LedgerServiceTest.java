package com.scalar.dl.ledger.server;

import static com.scalar.dl.ledger.server.TypeConverter.convert;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.exception.LedgerException;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.proof.AssetProof;
import com.scalar.dl.ledger.service.LedgerValidationService;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.rpc.AssetProofRetrievalRequest;
import com.scalar.dl.rpc.AssetProofRetrievalResponse;
import com.scalar.dl.rpc.ContractExecutionRequest;
import com.scalar.dl.rpc.ContractExecutionResponse;
import com.scalar.dl.rpc.ContractRegistrationRequest;
import com.scalar.dl.rpc.LedgerValidationRequest;
import com.scalar.dl.rpc.LedgerValidationResponse;
import io.grpc.stub.StreamObserver;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class LedgerServiceTest {
  private static final String SOME_ENTITY_ID = "entity_id";
  private static final int SOME_KEY_VERSION = 1;
  private static final String SOME_CONTRACT_ID = "contract_id";
  private static final String SOME_CONTRACT_BINARY_NAME = "contract_name";
  private static final byte[] SOME_CONTRACT_BYTE_CODE = "contract".getBytes(StandardCharsets.UTF_8);
  private static final String SOME_CONTRACT_PROPERTIES = "{}";
  private static final String SOME_CONTRACT_RESULT = "{\"result\":\"contract_result\"}";
  private static final String SOME_FUNCTION_ID = "function_id";
  private static final String SOME_FUNCTION_ARGUMENT = "function_argument";
  private static final String SOME_FUNCTION_RESULT = "{\"result\":\"function_result\"}";
  private static final String SOME_ASSET_ID = "asset_id";
  private static final int SOME_ASSET_AGE = 1;
  private static final byte[] SOME_HASH = "hash".getBytes(StandardCharsets.UTF_8);
  private static final byte[] SOME_PREV_HASH = "prev_hash".getBytes(StandardCharsets.UTF_8);
  private static final String SOME_NONCE = "nonce";
  private static final String SOME_INPUT = "input";
  private static final byte[] SOME_SIGNATURE = "signature".getBytes(StandardCharsets.UTF_8);
  private static final String SOME_ARGUMENT = "{\"nonce\": \"xxx\"}";
  private static final String SOME_MESSAGE = "hello";
  private static final String SOME_LEDGER_NAME = "myledger";
  @Mock private com.scalar.dl.ledger.service.LedgerService ledger;
  @Mock private LedgerValidationService validation;
  @Mock private LedgerConfig config;
  private LedgerService grpc;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    CommonService commonService = new CommonService(null, null);
    grpc = new LedgerService(ledger, validation, commonService, config);
  }

  private AssetProof createAssetProof() {
    return AssetProof.newBuilder()
        .id(SOME_ASSET_ID)
        .age(SOME_ASSET_AGE)
        .input(SOME_INPUT)
        .hash(SOME_HASH)
        .prevHash(SOME_PREV_HASH)
        .nonce(SOME_NONCE)
        .signature(SOME_SIGNATURE)
        .build();
  }

  @Test
  public void registerContract_ContractRegistrationRequestGiven_CallRegister() {
    // Arrange
    ContractRegistrationRequest request =
        ContractRegistrationRequest.newBuilder()
            .setContractId(SOME_CONTRACT_ID)
            .setContractBinaryName(SOME_CONTRACT_BINARY_NAME)
            .setContractByteCode(ByteString.copyFrom(SOME_CONTRACT_BYTE_CODE))
            .setContractProperties(SOME_CONTRACT_PROPERTIES)
            .setEntityId(SOME_ENTITY_ID)
            .setKeyVersion(SOME_KEY_VERSION)
            .setSignature(ByteString.copyFrom(SOME_SIGNATURE))
            .build();
    StreamObserver<Empty> observer = mock(StreamObserver.class);

    // Act
    grpc.registerContract(request, observer);

    // Assert
    verify(ledger).register(convert(request));
  }

  @Test
  public void executeContract_ContractExecutionRequestGiven_ShouldCallExecuteAndOnCompleted() {
    // Arrange
    AssetProof proof = createAssetProof();
    ContractExecutionRequest request =
        ContractExecutionRequest.newBuilder()
            .setNonce(SOME_NONCE)
            .setContractId(SOME_CONTRACT_ID)
            .setContractArgument(SOME_ARGUMENT)
            .setEntityId(SOME_ENTITY_ID)
            .setKeyVersion(SOME_KEY_VERSION)
            .setSignature(ByteString.copyFrom(SOME_SIGNATURE))
            .build();
    ContractExecutionResult result =
        new ContractExecutionResult(
            SOME_CONTRACT_RESULT, SOME_FUNCTION_RESULT, Collections.singletonList(proof), null);
    when(ledger.execute(convert(request))).thenReturn(result);
    StreamObserver<ContractExecutionResponse> observer = mock(StreamObserver.class);

    // Act
    grpc.executeContract(request, observer);

    // Assert
    verify(ledger).execute(convert(request));
    ContractExecutionResponse.Builder builder = ContractExecutionResponse.newBuilder();
    builder.setContractResult(SOME_CONTRACT_RESULT);
    builder.setFunctionResult(SOME_FUNCTION_RESULT);
    builder.addProofs(CommonTypeConverter.convert(proof));
    verify(observer).onNext(builder.build());
    verify(observer).onCompleted();
  }

  @Test
  public void executeContract_LedgerExceptionThrown_ShouldCallOnError() {
    // Arrange
    ContractExecutionRequest request =
        ContractExecutionRequest.newBuilder()
            .setNonce(SOME_NONCE)
            .setContractId(SOME_CONTRACT_ID)
            .setContractArgument(SOME_ARGUMENT)
            .setEntityId(SOME_ENTITY_ID)
            .setKeyVersion(SOME_KEY_VERSION)
            .setSignature(ByteString.copyFrom(SOME_SIGNATURE))
            .build();
    LedgerException toThrow =
        new LedgerException(SOME_MESSAGE, StatusCode.CONTRACT_CONTEXTUAL_ERROR);
    when(ledger.execute(convert(request))).thenThrow(toThrow);
    StreamObserver<ContractExecutionResponse> observer = mock(StreamObserver.class);

    // Act
    grpc.executeContract(request, observer);

    // Assert
    verify(ledger).execute(convert(request));
    // Can't compare since StatusRuntimeException doesn't implement equals
    verify(observer).onError(any());
  }

  @Test
  public void executeContract_FunctionIdAndArgumentGiven_ShouldCallExecute() {
    // Arrange
    ContractExecutionRequest request =
        ContractExecutionRequest.newBuilder()
            .setNonce(SOME_NONCE)
            .setContractId(SOME_CONTRACT_ID)
            .setContractArgument(SOME_ARGUMENT)
            .setEntityId(SOME_ENTITY_ID)
            .setKeyVersion(SOME_KEY_VERSION)
            .addFunctionIds(SOME_FUNCTION_ID)
            .setFunctionArgument(SOME_FUNCTION_ARGUMENT)
            .setSignature(ByteString.copyFrom(SOME_SIGNATURE))
            .build();
    ContractExecutionResult result =
        new ContractExecutionResult(SOME_CONTRACT_RESULT, SOME_FUNCTION_RESULT, null, null);
    when(ledger.execute(convert(request))).thenReturn(result);
    StreamObserver<ContractExecutionResponse> observer = mock(StreamObserver.class);

    // Act
    grpc.executeContract(request, observer);

    // Assert
    verify(ledger).execute(convert(request));
    ContractExecutionResponse.Builder builder = ContractExecutionResponse.newBuilder();
    builder.setContractResult(SOME_CONTRACT_RESULT);
    builder.setFunctionResult(SOME_FUNCTION_RESULT);
    verify(observer).onNext(builder.build());
    verify(observer).onCompleted();
  }

  @Test
  public void validate_LedgerValidationRequestGiven_CallValidateAndOnCompleted() {
    // Arrange
    AssetProof proof = createAssetProof();
    LedgerValidationRequest request =
        LedgerValidationRequest.newBuilder()
            .setAssetId(SOME_ASSET_ID)
            .setEntityId(SOME_ENTITY_ID)
            .setKeyVersion(SOME_KEY_VERSION)
            .setSignature(ByteString.copyFrom(SOME_SIGNATURE))
            .build();
    LedgerValidationResult result = new LedgerValidationResult(StatusCode.OK, proof, null);
    when(validation.validate(convert(request))).thenReturn(result);
    StreamObserver<LedgerValidationResponse> observer = mock(StreamObserver.class);

    // Act
    grpc.validateLedger(request, observer);

    // Assert
    verify(validation).validate(convert(request));
    LedgerValidationResponse.Builder builder = LedgerValidationResponse.newBuilder();
    builder.setProof(CommonTypeConverter.convert(proof));
    builder.setStatusCode(StatusCode.OK.get());
    verify(observer).onNext(builder.build());
    verify(observer).onCompleted();
  }

  @Test
  public void validate_LedgerExceptionThrown_ShouldCallOnError() {
    // Arrange
    LedgerValidationRequest request =
        LedgerValidationRequest.newBuilder()
            .setAssetId(SOME_ASSET_ID)
            .setEntityId(SOME_ENTITY_ID)
            .setKeyVersion(SOME_KEY_VERSION)
            .setSignature(ByteString.copyFrom(SOME_SIGNATURE))
            .build();
    LedgerException toThrow = new LedgerException(SOME_MESSAGE, StatusCode.INVALID_HASH);
    when(validation.validate(convert(request))).thenThrow(toThrow);
    StreamObserver<LedgerValidationResponse> observer = mock(StreamObserver.class);

    // Act
    grpc.validateLedger(request, observer);

    // Assert
    verify(validation).validate(convert(request));
    // Can't compare since StatusRuntimeException doesn't implement equals
    verify(observer).onError(any());
  }

  @Test
  public void retrieveAssetProof_AssetProofRetrievalRequestGiven_CallRetrieveAndOnCompleted() {
    // Arrange
    when(config.getName()).thenReturn(SOME_LEDGER_NAME);
    AssetProof proof = createAssetProof();
    AssetProofRetrievalRequest request =
        AssetProofRetrievalRequest.newBuilder()
            .setAssetId(SOME_ASSET_ID)
            .setAge(SOME_ASSET_AGE)
            .setEntityId(SOME_ENTITY_ID)
            .setKeyVersion(SOME_KEY_VERSION)
            .setSignature(ByteString.copyFrom(SOME_SIGNATURE))
            .build();
    when(validation.retrieve(convert(request))).thenReturn(proof);
    StreamObserver<AssetProofRetrievalResponse> observer = mock(StreamObserver.class);

    // Act
    grpc.retrieveAssetProof(request, observer);

    // Assert
    verify(validation).retrieve(convert(request));
    AssetProofRetrievalResponse.Builder builder = AssetProofRetrievalResponse.newBuilder();
    builder.setProof(CommonTypeConverter.convert(proof));
    builder.setLedgerName(SOME_LEDGER_NAME);
    verify(observer).onNext(builder.build());
    verify(observer).onCompleted();
  }

  @Test
  public void retrieveAssetProof_LedgerExceptionThrown_ShouldCallOnError() {
    // Arrange
    AssetProofRetrievalRequest request =
        AssetProofRetrievalRequest.newBuilder()
            .setAssetId(SOME_ASSET_ID)
            .setAge(SOME_ASSET_AGE)
            .setEntityId(SOME_ENTITY_ID)
            .setKeyVersion(SOME_KEY_VERSION)
            .setSignature(ByteString.copyFrom(SOME_SIGNATURE))
            .build();
    LedgerException toThrow = new LedgerException(SOME_MESSAGE, StatusCode.RUNTIME_ERROR);
    when(validation.retrieve(convert(request))).thenThrow(toThrow);
    StreamObserver<AssetProofRetrievalResponse> observer = mock(StreamObserver.class);

    // Act
    grpc.retrieveAssetProof(request, observer);

    // Assert
    verify(validation).retrieve(convert(request));
    // Can't compare since StatusRuntimeException doesn't implement equals
    verify(observer).onError(any());
  }
}
