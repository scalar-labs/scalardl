package com.scalar.dl.ledger.server;

import static com.scalar.dl.ledger.server.ThreadTestUtils.awaitThreadState;
import static com.scalar.dl.ledger.server.ThreadTestUtils.joinPromptly;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.ErrorInfo;
import com.scalar.admin.rpc.CheckPausedResponse;
import com.scalar.admin.rpc.PauseRequest;
import com.scalar.dl.ledger.server.GateKeeper.PauseResult;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class AdminServiceTest {

  /** The fallback used when the request does not specify a maximum pause wait time. */
  private static final long DEFAULT_MAX_PAUSE_WAIT_TIME_MILLIS = 30000;

  private GateKeeper gateKeeper;
  private AdminService adminService;

  @BeforeEach
  public void setUp() {
    gateKeeper = mock(GateKeeper.class);
    adminService = new AdminService(gateKeeper);
  }

  @Test
  public void pause_WithWaitOutstandingAndPaused_ShouldPause() {
    // Arrange
    when(gateKeeper.pauseAndAwaitDrained(anyLong(), any())).thenReturn(PauseResult.PAUSED);
    StreamObserver<Empty> responseObserver = emptyResponseObserver();

    // Act
    adminService.pause(pauseRequest(true, 1000), responseObserver);

    // Assert
    verify(gateKeeper).pauseAndAwaitDrained(1000L, TimeUnit.MILLISECONDS);
    verify(responseObserver).onNext(Empty.getDefaultInstance());
    verify(responseObserver).onCompleted();
    verify(responseObserver, never()).onError(any());
  }

  @Test
  public void pause_WithWaitOutstandingAndTimedOut_ShouldReturnFailedPrecondition() {
    // Arrange
    when(gateKeeper.pauseAndAwaitDrained(anyLong(), any())).thenReturn(PauseResult.TIMED_OUT);
    StreamObserver<Empty> responseObserver = emptyResponseObserver();

    // Act
    adminService.pause(pauseRequest(true, 1000), responseObserver);

    // Assert
    verify(responseObserver, never()).onNext(any());
    verify(responseObserver, never()).onCompleted();

    StatusRuntimeException error = captureError(responseObserver);
    assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.FAILED_PRECONDITION);
    assertReason(error, PauseResult.TIMED_OUT);
    assertThat(error.getStatus().getDescription())
        .contains("outstanding requests were not drained within 1000 ms")
        .contains("the gate has been reopened");
  }

  @Test
  public void pause_WithWaitOutstandingAndTimedOutStillPaused_ShouldSayGateStaysClosed() {
    // Arrange
    when(gateKeeper.pauseAndAwaitDrained(anyLong(), any()))
        .thenReturn(PauseResult.TIMED_OUT_STILL_PAUSED);
    StreamObserver<Empty> responseObserver = emptyResponseObserver();

    // Act
    adminService.pause(pauseRequest(true, 1000), responseObserver);

    // Assert: the caller has to learn that the server is paused despite its own failure. Taking
    // this for an unpaused server and "recovering" with an unpause would revoke the earlier pause.
    StatusRuntimeException error = captureError(responseObserver);
    assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.FAILED_PRECONDITION);
    assertReason(error, PauseResult.TIMED_OUT_STILL_PAUSED);
    assertThat(error.getStatus().getDescription())
        .contains("outstanding requests were not drained within 1000 ms")
        .contains("the gate stays closed by an earlier pause")
        .doesNotContain("has been reopened");
  }

  @Test
  public void pause_WhenTimedOutWithAndWithoutAnEarlierPause_ShouldDifferOnlyByReason() {
    // Arrange: these two outcomes need opposite recovery -- only TIMED_OUT leaves the gate open and
    // may be followed by an unpause -- yet they deliberately share a gRPC status code.
    when(gateKeeper.pauseAndAwaitDrained(anyLong(), any()))
        .thenReturn(PauseResult.TIMED_OUT, PauseResult.TIMED_OUT_STILL_PAUSED);
    StreamObserver<Empty> reopenedResponseObserver = emptyResponseObserver();
    StreamObserver<Empty> stillPausedResponseObserver = emptyResponseObserver();

    // Act
    adminService.pause(pauseRequest(true, 1000), reopenedResponseObserver);
    adminService.pause(pauseRequest(true, 1000), stillPausedResponseObserver);

    // Assert: the status code alone cannot tell them apart, so a caller deciding whether unpausing
    // is safe has to be able to do it without parsing the description.
    StatusRuntimeException reopened = captureError(reopenedResponseObserver);
    StatusRuntimeException stillPaused = captureError(stillPausedResponseObserver);
    assertThat(reopened.getStatus().getCode()).isEqualTo(stillPaused.getStatus().getCode());
    assertReason(reopened, PauseResult.TIMED_OUT);
    assertReason(stillPaused, PauseResult.TIMED_OUT_STILL_PAUSED);
  }

  @Test
  public void pause_WithWaitOutstandingAndGateOpen_ShouldReturnAborted() {
    // Arrange
    when(gateKeeper.pauseAndAwaitDrained(anyLong(), any())).thenReturn(PauseResult.GATE_OPEN);
    StreamObserver<Empty> responseObserver = emptyResponseObserver();

    // Act
    adminService.pause(pauseRequest(true, 1000), responseObserver);

    // Assert: losing a race with an unpause is a transient conflict, reported the same way as
    // losing one with another pause so that a caller retrying on ABORTED covers both.
    verify(responseObserver, never()).onNext(any());
    verify(responseObserver, never()).onCompleted();

    StatusRuntimeException error = captureError(responseObserver);
    assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.ABORTED);
    assertReason(error, PauseResult.GATE_OPEN);
    assertThat(error.getStatus().getDescription())
        .contains("reopened by a concurrent unpause")
        .doesNotContain("were not drained within");
  }

  @Test
  public void pause_WithWaitOutstandingAndPauseInProgress_ShouldReturnAborted() {
    // Arrange
    when(gateKeeper.pauseAndAwaitDrained(anyLong(), any()))
        .thenReturn(PauseResult.PAUSE_IN_PROGRESS);
    StreamObserver<Empty> responseObserver = emptyResponseObserver();

    // Act
    adminService.pause(pauseRequest(true, 1000), responseObserver);

    // Assert: losing a race with another pause is a transient conflict, so it is reported as a
    // retryable ABORTED rather than as a failed precondition.
    verify(responseObserver, never()).onNext(any());
    verify(responseObserver, never()).onCompleted();

    StatusRuntimeException error = captureError(responseObserver);
    assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.ABORTED);
    assertReason(error, PauseResult.PAUSE_IN_PROGRESS);
    assertThat(error.getStatus().getDescription()).contains("another pause request is in progress");
  }

  @Test
  public void pause_WithoutWaitOutstanding_ShouldPauseWithoutAwaitingDrain() {
    // Arrange
    when(gateKeeper.pause()).thenReturn(PauseResult.PAUSED);
    StreamObserver<Empty> responseObserver = emptyResponseObserver();

    // Act
    adminService.pause(pauseRequest(false, 1000), responseObserver);

    // Assert
    verify(gateKeeper).pause();
    verify(gateKeeper, never()).pauseAndAwaitDrained(anyLong(), any());
    verify(responseObserver).onNext(Empty.getDefaultInstance());
    verify(responseObserver).onCompleted();
  }

  @Test
  public void pause_WithoutWaitOutstandingAndPauseInProgress_ShouldReturnAborted() {
    // Arrange
    when(gateKeeper.pause()).thenReturn(PauseResult.PAUSE_IN_PROGRESS);
    StreamObserver<Empty> responseObserver = emptyResponseObserver();

    // Act
    adminService.pause(pauseRequest(false, 1000), responseObserver);

    // Assert
    StatusRuntimeException error = captureError(responseObserver);
    assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.ABORTED);
    assertReason(error, PauseResult.PAUSE_IN_PROGRESS);
    assertThat(error.getStatus().getDescription()).contains("another pause request is in progress");
  }

  @Test
  public void pause_WithoutMaxPauseWaitTime_ShouldAwaitDrainWithDefaultMaxPauseWaitTime() {
    // Arrange: the field is left unset, which is how a caller with no time limit in mind asks for
    // the default. An explicitly specified value is never substituted, not even 0.
    when(gateKeeper.pauseAndAwaitDrained(anyLong(), any())).thenReturn(PauseResult.PAUSED);
    StreamObserver<Empty> responseObserver = emptyResponseObserver();

    // Act
    adminService.pause(pauseRequestWithoutMaxPauseWaitTime(true), responseObserver);

    // Assert
    verify(gateKeeper)
        .pauseAndAwaitDrained(DEFAULT_MAX_PAUSE_WAIT_TIME_MILLIS, TimeUnit.MILLISECONDS);
    verify(responseObserver).onCompleted();
  }

  @Test
  public void pause_WithZeroMaxPauseWaitTime_ShouldReturnInvalidArgument() {
    // Arrange: 0 is a specified time limit, distinguishable from an unset field, so it must not be
    // silently replaced by the default the way an unset field is.
    StreamObserver<Empty> responseObserver = emptyResponseObserver();

    // Act
    adminService.pause(pauseRequest(true, 0), responseObserver);

    // Assert
    verify(gateKeeper, never()).pauseAndAwaitDrained(anyLong(), any());
    verify(gateKeeper, never()).pause();
    verify(responseObserver, never()).onNext(any());
    verify(responseObserver, never()).onCompleted();

    StatusRuntimeException error = captureError(responseObserver);
    assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    assertThat(error.getStatus().getDescription())
        .contains("max_pause_wait_time must be greater than 0 ms, but was 0 ms");
  }

  @Test
  public void pause_WithNegativeMaxPauseWaitTime_ShouldReturnInvalidArgument() {
    // Arrange: a negative limit has already elapsed when the wait begins, so passing it on would
    // turn a request that asked to wait into one that does not, and report the failure as a drain
    // timeout that never waited.
    StreamObserver<Empty> responseObserver = emptyResponseObserver();

    // Act
    adminService.pause(pauseRequest(true, -1), responseObserver);

    // Assert
    verify(gateKeeper, never()).pauseAndAwaitDrained(anyLong(), any());
    verify(responseObserver, never()).onCompleted();

    StatusRuntimeException error = captureError(responseObserver);
    assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    assertThat(error.getStatus().getDescription())
        .contains("max_pause_wait_time must be greater than 0 ms, but was -1 ms");
  }

  @Test
  public void pause_WithoutWaitOutstandingAndNegativeMaxPauseWaitTime_ShouldPause() {
    // Arrange: a request that asked not to wait has always ignored the field, so validating it
    // there would fail calls that work today.
    when(gateKeeper.pause()).thenReturn(PauseResult.PAUSED);
    StreamObserver<Empty> responseObserver = emptyResponseObserver();

    // Act
    adminService.pause(pauseRequest(false, -1), responseObserver);

    // Assert
    verify(gateKeeper).pause();
    verify(responseObserver).onCompleted();
    verify(responseObserver, never()).onError(any());
  }

  @Test
  public void pause_WithMaxPauseWaitTime_ShouldAwaitDrainWithGivenMaxPauseWaitTime() {
    // Arrange
    when(gateKeeper.pauseAndAwaitDrained(anyLong(), any())).thenReturn(PauseResult.PAUSED);
    StreamObserver<Empty> responseObserver = emptyResponseObserver();

    // Act
    adminService.pause(pauseRequest(true, 1234), responseObserver);

    // Assert
    verify(gateKeeper).pauseAndAwaitDrained(1234L, TimeUnit.MILLISECONDS);
    verify(responseObserver).onCompleted();
  }

  @Test
  public void unpause_ShouldOpenGate() {
    // Arrange
    StreamObserver<Empty> responseObserver = emptyResponseObserver();

    // Act
    adminService.unpause(Empty.getDefaultInstance(), responseObserver);

    // Assert
    verify(gateKeeper).open();
    verify(responseObserver).onNext(Empty.getDefaultInstance());
    verify(responseObserver).onCompleted();
  }

  // The tests below run the service against a real GateKeeper rather than a mock, so that the two
  // classes are verified as they actually compose. The race they guard against lives in that
  // composition, not in either class alone.

  @Test
  public void pause_WhenLaterPauseTimesOutAfterEarlierPauseSucceeded_ShouldKeepGateClosed() {
    // Arrange: a request is in flight, then a pause that does not wait for outstanding requests
    // closes the gate and is told "Paused". Its client now relies on the gate staying closed.
    GateKeeper realGateKeeper = new GateKeeper();
    AdminService service = new AdminService(realGateKeeper);
    realGateKeeper.letIn();

    StreamObserver<Empty> firstResponseObserver = emptyResponseObserver();
    service.pause(pauseRequest(false, 0), firstResponseObserver);

    verify(firstResponseObserver).onCompleted();
    verify(firstResponseObserver, never()).onError(any());
    assertThat(realGateKeeper.isOpen()).isFalse();

    // Act: a second pause arrives, inherits the already-closed gate, and times out because the
    // request is still running.
    StreamObserver<Empty> secondResponseObserver = emptyResponseObserver();
    service.pause(pauseRequest(true, 50), secondResponseObserver);

    // Assert: the second pause fails, but it must not reopen the gate. Doing so would revoke the
    // first pause's already-reported success and let new requests through while a backup tool
    // believes the server is paused.
    StatusRuntimeException error = captureError(secondResponseObserver);
    assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.FAILED_PRECONDITION);
    assertReason(error, PauseResult.TIMED_OUT_STILL_PAUSED);
    assertThat(error.getStatus().getDescription())
        .contains("the gate stays closed by an earlier pause");
    assertThat(realGateKeeper.isOpen()).isFalse();
  }

  @Test
  public void pause_WhenItTimesOutWithoutAnEarlierPause_ShouldReopenGate() {
    // Arrange: the only pause in play is this one, so it owns the closure it makes.
    GateKeeper realGateKeeper = new GateKeeper();
    AdminService service = new AdminService(realGateKeeper);
    realGateKeeper.letIn();

    // Act
    StreamObserver<Empty> responseObserver = emptyResponseObserver();
    service.pause(pauseRequest(true, 50), responseObserver);

    // Assert: a pause that fails on its own must not strand the server paused.
    StatusRuntimeException error = captureError(responseObserver);
    assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.FAILED_PRECONDITION);
    assertReason(error, PauseResult.TIMED_OUT);
    assertThat(error.getStatus().getDescription()).contains("the gate has been reopened");
    assertThat(realGateKeeper.isOpen()).isTrue();
  }

  @Test
  public void pause_WhileAnotherPauseIsDraining_ShouldWaitForItAndPause()
      throws InterruptedException {
    // Arrange: hold a request open so the first pause blocks while draining.
    GateKeeper realGateKeeper = new GateKeeper();
    AdminService service = new AdminService(realGateKeeper);
    realGateKeeper.letIn();

    StreamObserver<Empty> firstResponseObserver = emptyResponseObserver();
    Thread firstPause =
        new Thread(() -> service.pause(pauseRequest(true, 10_000), firstResponseObserver));
    firstPause.start();
    awaitThreadState(firstPause, Thread.State.TIMED_WAITING);

    // Act: a second pause arrives while the first is still draining, and waits for its turn.
    StreamObserver<Empty> secondResponseObserver = emptyResponseObserver();
    Thread secondPause =
        new Thread(() -> service.pause(pauseRequest(true, 10_000), secondResponseObserver));
    secondPause.start();
    awaitThreadState(secondPause, Thread.State.TIMED_WAITING);

    realGateKeeper.letOut();
    joinPromptly(firstPause);
    joinPromptly(secondPause);

    // Assert: neither pause is failed, so a caller that unpauses to recover from a failure never
    // issues the unpause that would reopen the gate under the other pause.
    verify(firstResponseObserver).onCompleted();
    verify(firstResponseObserver, never()).onError(any());
    verify(secondResponseObserver).onCompleted();
    verify(secondResponseObserver, never()).onError(any());
    assertThat(realGateKeeper.isOpen()).isFalse();
  }

  @Test
  public void pause_WhenAnotherPauseDoesNotFinishInTime_ShouldReturnAborted()
      throws InterruptedException {
    // Arrange: hold a request open so the first pause is still draining when the second arrives.
    // Give it a time limit it cannot reach during the test rather than one the second call has to
    // beat: if it could time out on its own, it would release the gate before the second call runs
    // and this test would fail for a scheduling delay rather than for a regression.
    GateKeeper realGateKeeper = new GateKeeper();
    AdminService service = new AdminService(realGateKeeper);
    realGateKeeper.letIn();

    StreamObserver<Empty> blockedResponseObserver = emptyResponseObserver();
    Thread blockedPause =
        new Thread(() -> service.pause(pauseRequest(true, 10_000), blockedResponseObserver));
    blockedPause.start();
    awaitThreadState(blockedPause, Thread.State.TIMED_WAITING);

    // Act: a short time limit is enough, and is all the test spends in real time. The pause in
    // progress is already confirmed to hold the gate above, so this call gives up at its own
    // deadline no matter how small that deadline is.
    StreamObserver<Empty> rejectedResponseObserver = emptyResponseObserver();
    service.pause(pauseRequest(true, 50), rejectedResponseObserver);

    // Assert: waiting is bounded by the caller's own time limit, and giving up leaves the gate to
    // the first pause rather than closing it behind that pause's back.
    StatusRuntimeException error = captureError(rejectedResponseObserver);
    assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.ABORTED);
    assertReason(error, PauseResult.PAUSE_IN_PROGRESS);
    assertThat(error.getStatus().getDescription()).contains("another pause request is in progress");

    // The first pause still completes normally once its request drains.
    realGateKeeper.letOut();
    joinPromptly(blockedPause);
    verify(blockedResponseObserver).onCompleted();
    verify(blockedResponseObserver, never()).onError(any());
  }

  @Test
  public void checkPaused_WhenGateIsOpen_ShouldReturnNotPaused() {
    // Arrange
    when(gateKeeper.isOpen()).thenReturn(true);

    @SuppressWarnings("unchecked")
    StreamObserver<CheckPausedResponse> responseObserver =
        (StreamObserver<CheckPausedResponse>) mock(StreamObserver.class);

    // Act
    adminService.checkPaused(Empty.getDefaultInstance(), responseObserver);

    // Assert
    ArgumentCaptor<CheckPausedResponse> captor = ArgumentCaptor.forClass(CheckPausedResponse.class);
    verify(responseObserver).onNext(captor.capture());
    assertThat(captor.getValue().getPaused()).isFalse();
    verify(responseObserver).onCompleted();
  }

  @Test
  public void checkPaused_WhenGateIsClosed_ShouldReturnPaused() {
    // Arrange
    when(gateKeeper.isOpen()).thenReturn(false);

    @SuppressWarnings("unchecked")
    StreamObserver<CheckPausedResponse> responseObserver =
        (StreamObserver<CheckPausedResponse>) mock(StreamObserver.class);

    // Act
    adminService.checkPaused(Empty.getDefaultInstance(), responseObserver);

    // Assert
    ArgumentCaptor<CheckPausedResponse> captor = ArgumentCaptor.forClass(CheckPausedResponse.class);
    verify(responseObserver).onNext(captor.capture());
    assertThat(captor.getValue().getPaused()).isTrue();
    verify(responseObserver).onCompleted();
  }

  private static PauseRequest pauseRequest(boolean waitOutstanding, long maxPauseWaitTime) {
    return PauseRequest.newBuilder()
        .setWaitOutstanding(waitOutstanding)
        .setMaxPauseWaitTime(maxPauseWaitTime)
        .build();
  }

  /**
   * Builds a request that leaves {@code max_pause_wait_time} unset. The field tracks presence, so
   * this is not the same as specifying 0: only an unset field falls back to the default.
   */
  private static PauseRequest pauseRequestWithoutMaxPauseWaitTime(boolean waitOutstanding) {
    return PauseRequest.newBuilder().setWaitOutstanding(waitOutstanding).build();
  }

  @SuppressWarnings("unchecked")
  private static StreamObserver<Empty> emptyResponseObserver() {
    return (StreamObserver<Empty>) mock(StreamObserver.class);
  }

  /**
   * Asserts the machine-readable outcome carried by the error. This, rather than the description
   * text, is what a caller has to branch on to decide whether unpausing is safe, so it is asserted
   * separately from the human-readable message everywhere a pause can fail.
   */
  private static void assertReason(StatusRuntimeException error, PauseResult expected) {
    com.google.rpc.Status status = StatusProto.fromThrowable(error);
    assertThat(status).isNotNull();

    ErrorInfo errorInfo = null;
    for (Any detail : status.getDetailsList()) {
      if (detail.is(ErrorInfo.class)) {
        try {
          errorInfo = detail.unpack(ErrorInfo.class);
        } catch (InvalidProtocolBufferException e) {
          throw new AssertionError("the ErrorInfo detail could not be unpacked", e);
        }
        break;
      }
    }

    assertThat(errorInfo).as("the error must carry an ErrorInfo detail").isNotNull();
    assertThat(errorInfo.getReason()).isEqualTo(expected.name());
    assertThat(errorInfo.getDomain()).isEqualTo("com.scalar.dl.admin");
  }

  private static StatusRuntimeException captureError(StreamObserver<Empty> responseObserver) {
    ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
    verify(responseObserver).onError(captor.capture());
    assertThat(captor.getValue()).isInstanceOf(StatusRuntimeException.class);
    return (StatusRuntimeException) captor.getValue();
  }
}
