package com.scalar.dl.ledger.server;

import static com.scalar.dl.ledger.server.ThreadTestUtils.awaitThreadState;
import static com.scalar.dl.ledger.server.ThreadTestUtils.joinPromptly;
import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.dl.ledger.server.GateKeeper.PauseResult;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GateKeeperTest {

  /**
   * How long to give a waiter the chance to wake up when a test asserts that it must <em>not</em>
   * wake up. Asserting immediately would let a regression slip through whenever the waiter simply
   * had not been scheduled yet.
   */
  private static final long MUST_NOT_WAKE_UP_MILLIS = 100;

  private GateKeeper gateKeeper;

  @BeforeEach
  public void setUp() {
    gateKeeper = new GateKeeper();
  }

  @Test
  public void letIn_GateOpened_ShouldIncrementOutstanding() {
    // Arrange
    // Act
    gateKeeper.letIn();

    // Assert
    assertThat(gateKeeper.getNumOutstandingRequests()).isEqualTo(1);
  }

  @Test
  public void letIn_WhileGateIsClosed_ShouldBlockUntilGateIsReopened() throws InterruptedException {
    // Arrange
    gateKeeper.close();

    AtomicBoolean letIn = new AtomicBoolean(false);
    Thread request =
        new Thread(
            () -> {
              gateKeeper.letIn();
              letIn.set(true);
            });
    request.start();
    try {
      // A paused server must hold new requests at the gate. Reaching WAITING here is the assertion
      // that the request was not admitted; if it were, the thread would have terminated instead.
      awaitThreadState(request, Thread.State.WAITING);
      assertThat(letIn.get()).isFalse();
    } finally {
      // Act -- and also the cleanup: letIn() waits without a timeout, so a failure above would
      // otherwise leave this non-daemon thread blocked forever and hang the build instead of
      // failing the test.
      gateKeeper.open();
    }
    joinPromptly(request);

    // Assert: the held request is admitted once the gate reopens.
    assertThat(letIn.get()).isTrue();
    assertThat(gateKeeper.getNumOutstandingRequests()).isEqualTo(1);
  }

  @Test
  public void letOut_OnlyOneOutstandingExists_ShouldDecrementOutstanding() {
    // Arrange
    gateKeeper.letIn();

    // Act
    gateKeeper.letOut();

    // Assert
    assertThat(gateKeeper.getNumOutstandingRequests()).isEqualTo(0);
  }

  @Test
  public void open_OnAnyCondition_ShouldOpen() {
    // Arrange
    // Act
    gateKeeper.open();

    // Assert
    assertThat(gateKeeper.isOpen()).isTrue();
  }

  @Test
  public void close_WhenGateIsOpen_ShouldCloseGateAndReportOwnership() {
    // Arrange
    // Act
    boolean closedByThisCall = gateKeeper.close();

    // Assert: the caller that made the transition owns the closure and may undo it.
    assertThat(closedByThisCall).isTrue();
    assertThat(gateKeeper.isOpen()).isFalse();
  }

  @Test
  public void close_WhenGateIsAlreadyClosed_ShouldReportNoOwnership() {
    // Arrange: an earlier pause already closed the gate and may already have reported success for
    // it.
    gateKeeper.close();

    // Act
    boolean closedByThisCall = gateKeeper.close();

    // Assert: inheriting an existing closure establishes nothing, so this caller must not undo it.
    assertThat(closedByThisCall).isFalse();
    assertThat(gateKeeper.isOpen()).isFalse();
  }

  @Test
  public void close_WhenGateIsReopenedInBetween_ShouldReportOwnershipAgain() {
    // Arrange
    gateKeeper.close();
    // An unpause reopens the gate, so the next close makes a genuinely new closure.
    gateKeeper.open();

    // Act
    boolean closedByThisCall = gateKeeper.close();

    // Assert
    assertThat(closedByThisCall).isTrue();
    assertThat(gateKeeper.isOpen()).isFalse();
  }

  @Test
  public void awaitDrained_GateReopenedByUnpauseBeforeCall_ShouldReturnGateOpen() {
    // Arrange
    gateKeeper.close();
    // A concurrent unpause reopens the gate before the drain check runs.
    gateKeeper.open();

    // Act
    PauseResult pauseResult = gateKeeper.awaitDrained(10, TimeUnit.SECONDS);

    // Assert: a reopened gate must not be reported as a drained pause, even with nothing
    // outstanding.
    assertThat(pauseResult).isEqualTo(PauseResult.GATE_OPEN);
  }

  @Test
  public void awaitDrained_GateReopenedByUnpauseDuringWait_ShouldReturnGateOpen()
      throws InterruptedException {
    // Arrange
    gateKeeper.letIn(); // register the in-flight request while the gate is still open
    gateKeeper.close(); // one request still outstanding, so awaitDrained blocks

    AtomicReference<PauseResult> pauseResult = new AtomicReference<>();
    Thread pauseWaiter =
        new Thread(() -> pauseResult.set(gateKeeper.awaitDrained(10, TimeUnit.SECONDS)));
    pauseWaiter.start();
    awaitThreadState(pauseWaiter, Thread.State.TIMED_WAITING);

    // Act: a concurrent unpause reopens the gate while the pause is still waiting to drain.
    gateKeeper.open();
    joinPromptly(pauseWaiter);

    // Assert
    assertThat(pauseResult.get()).isEqualTo(PauseResult.GATE_OPEN);
  }

  @Test
  public void awaitDrained_OutstandingRequestsDrainWhileStillClosed_ShouldReturnPaused()
      throws InterruptedException {
    // Arrange
    gateKeeper.letIn(); // register the in-flight request while the gate is still open
    gateKeeper.close();

    AtomicReference<PauseResult> pauseResult = new AtomicReference<>();
    Thread pauseWaiter =
        new Thread(() -> pauseResult.set(gateKeeper.awaitDrained(10, TimeUnit.SECONDS)));
    pauseWaiter.start();
    awaitThreadState(pauseWaiter, Thread.State.TIMED_WAITING);

    // Act: the outstanding request finishes with no concurrent unpause.
    gateKeeper.letOut();
    joinPromptly(pauseWaiter);

    // Assert: reaching a genuine paused-and-drained state is a successful pause.
    assertThat(pauseResult.get()).isEqualTo(PauseResult.PAUSED);
  }

  @Test
  public void awaitDrained_NoOutstandingRequestsWhileClosed_ShouldReturnPaused() {
    // Arrange
    gateKeeper.close();

    // Act
    PauseResult pauseResult = gateKeeper.awaitDrained(10, TimeUnit.SECONDS);

    // Assert
    assertThat(pauseResult).isEqualTo(PauseResult.PAUSED);
  }

  @Test
  public void awaitDrained_TimeoutWithOutstandingRequestsWhileClosed_ShouldReturnTimedOut() {
    // Arrange
    gateKeeper.letIn(); // register the in-flight request while the gate is still open
    gateKeeper.close(); // never drained, never reopened

    // Act
    PauseResult pauseResult = gateKeeper.awaitDrained(50, TimeUnit.MILLISECONDS);

    // Assert: timeout with the gate still closed and work outstanding stays a failure.
    assertThat(pauseResult).isEqualTo(PauseResult.TIMED_OUT);
  }

  @Test
  public void
      awaitDrained_WithMultipleOutstandingRequests_ShouldReturnPausedOnlyAfterTheLastFinishes()
          throws InterruptedException {
    // Arrange
    gateKeeper.letIn();
    gateKeeper.letIn();
    gateKeeper.close();

    AtomicReference<PauseResult> pauseResult = new AtomicReference<>();
    Thread pauseWaiter =
        new Thread(() -> pauseResult.set(gateKeeper.awaitDrained(10, TimeUnit.SECONDS)));
    pauseWaiter.start();
    awaitThreadState(pauseWaiter, Thread.State.TIMED_WAITING);

    // Act: one of the two requests finishes.
    gateKeeper.letOut();

    // Assert: a partial drain is not a drained pause, so the waiter must stay put. Reporting
    // success here would let a pause claim the server is quiet while a request is still running.
    pauseWaiter.join(MUST_NOT_WAKE_UP_MILLIS);
    assertThat(pauseWaiter.isAlive()).isTrue();
    assertThat(pauseResult.get()).isNull();

    // Act: the last outstanding request finishes.
    gateKeeper.letOut();
    joinPromptly(pauseWaiter);

    // Assert
    assertThat(pauseResult.get()).isEqualTo(PauseResult.PAUSED);
  }

  @Test
  public void pause_WhenGateIsOpen_ShouldCloseGate() {
    // Arrange
    // Act
    PauseResult pauseResult = gateKeeper.pause();

    // Assert
    assertThat(pauseResult).isEqualTo(PauseResult.PAUSED);
    assertThat(gateKeeper.isOpen()).isFalse();
  }

  @Test
  public void pause_WhileAnotherPauseIsDraining_ShouldReturnPauseInProgress()
      throws InterruptedException {
    // Arrange: a non-waiting pause must not be able to claim success on a gate that a draining
    // pause is still entitled to reopen.
    gateKeeper.letIn();

    AtomicReference<PauseResult> firstResult = new AtomicReference<>();
    // The first pause has to still be draining when the second one arrives, so give it a time limit
    // it cannot reach during the test rather than one the second call has to beat.
    Thread firstPause =
        new Thread(() -> firstResult.set(gateKeeper.pauseAndAwaitDrained(10, TimeUnit.SECONDS)));
    firstPause.start();
    awaitThreadState(firstPause, Thread.State.TIMED_WAITING);

    // Act
    PauseResult secondResult = gateKeeper.pause();

    // Assert
    assertThat(secondResult).isEqualTo(PauseResult.PAUSE_IN_PROGRESS);

    // The first pause still completes normally once its request drains.
    gateKeeper.letOut();
    joinPromptly(firstPause);
    assertThat(firstResult.get()).isEqualTo(PauseResult.PAUSED);
  }

  @Test
  public void pauseAndAwaitDrained_WhenDrained_ShouldReturnPausedAndKeepGateClosed() {
    // Arrange
    // Act
    PauseResult pauseResult = gateKeeper.pauseAndAwaitDrained(10, TimeUnit.SECONDS);

    // Assert
    assertThat(pauseResult).isEqualTo(PauseResult.PAUSED);
    assertThat(gateKeeper.isOpen()).isFalse();
  }

  @Test
  public void pauseAndAwaitDrained_WhenItClosedTheGateAndTimedOut_ShouldReopenGate() {
    // Arrange: the only pause in play is this one, so it owns the closure it makes.
    gateKeeper.letIn();

    // Act
    PauseResult pauseResult = gateKeeper.pauseAndAwaitDrained(50, TimeUnit.MILLISECONDS);

    // Assert: a pause that fails on its own must not leave the server paused.
    assertThat(pauseResult).isEqualTo(PauseResult.TIMED_OUT);
    assertThat(gateKeeper.isOpen()).isTrue();
  }

  @Test
  public void pauseAndAwaitDrained_WhenItClosedTheGateAndGateWasReopened_ShouldReturnGateOpen()
      throws InterruptedException {
    // Arrange: this call closes the gate itself, so it owns the closure and is entitled to undo it.
    gateKeeper.letIn();

    AtomicReference<PauseResult> pauseResult = new AtomicReference<>();
    Thread pauseWaiter =
        new Thread(() -> pauseResult.set(gateKeeper.pauseAndAwaitDrained(10, TimeUnit.SECONDS)));
    pauseWaiter.start();
    awaitThreadState(pauseWaiter, Thread.State.TIMED_WAITING);

    // Act: a concurrent unpause reopens the gate while this call is still draining.
    gateKeeper.open();
    joinPromptly(pauseWaiter);

    // Assert: a reopened gate must not be reported as a drained pause, and the pause must leave it
    // open rather than reasserting a closure the unpause deliberately undid.
    assertThat(pauseResult.get()).isEqualTo(PauseResult.GATE_OPEN);
    assertThat(gateKeeper.isOpen()).isTrue();
  }

  @Test
  public void
      pauseAndAwaitDrained_WhenItInheritedTheClosureAndGateWasReopened_ShouldReturnGateOpen()
          throws InterruptedException {
    // Arrange: an earlier pause closed the gate, and this call inherits that closure.
    gateKeeper.letIn();
    assertThat(gateKeeper.pause()).isEqualTo(PauseResult.PAUSED);

    AtomicReference<PauseResult> pauseResult = new AtomicReference<>();
    Thread pauseWaiter =
        new Thread(() -> pauseResult.set(gateKeeper.pauseAndAwaitDrained(10, TimeUnit.SECONDS)));
    pauseWaiter.start();
    awaitThreadState(pauseWaiter, Thread.State.TIMED_WAITING);

    // Act: an unpause reopens the gate while this call is waiting.
    gateKeeper.open();
    joinPromptly(pauseWaiter);

    // Assert: the gate really is open now, so this stays GATE_OPEN rather than being reported as
    // still paused.
    assertThat(pauseResult.get()).isEqualTo(PauseResult.GATE_OPEN);
    assertThat(gateKeeper.isOpen()).isTrue();
  }

  @Test
  public void pauseAndAwaitDrained_WhenItInheritedTheClosureAndTimedOut_ShouldKeepGateClosed() {
    // Arrange: an earlier pause already closed the gate and was told it succeeded, so this call
    // inherits that closure rather than establishing one of its own.
    gateKeeper.letIn();
    assertThat(gateKeeper.pause()).isEqualTo(PauseResult.PAUSED);

    // Act
    PauseResult pauseResult = gateKeeper.pauseAndAwaitDrained(50, TimeUnit.MILLISECONDS);

    // Assert: reopening here would revoke a pause whose caller was already told "Paused", letting
    // new requests through while a backup tool believes the server is paused. The outcome has to
    // say so, or this caller could take its own failure for an unpaused server.
    assertThat(pauseResult).isEqualTo(PauseResult.TIMED_OUT_STILL_PAUSED);
    assertThat(gateKeeper.isOpen()).isFalse();
  }

  @Test
  public void pauseAndAwaitDrained_WhileAnotherPauseIsDraining_ShouldWaitForItAndThenPause()
      throws InterruptedException {
    // Arrange: hold a request open so the first pause blocks while draining.
    gateKeeper.letIn();

    AtomicReference<PauseResult> firstResult = new AtomicReference<>();
    Thread firstPause =
        new Thread(() -> firstResult.set(gateKeeper.pauseAndAwaitDrained(10, TimeUnit.SECONDS)));
    firstPause.start();
    awaitThreadState(firstPause, Thread.State.TIMED_WAITING);

    // Act: a second pause arrives while the first is still draining. It waits for its turn instead
    // of failing fast, because a caller that unpauses to recover from a failure would answer a
    // rejection with an unpause, reopening the gate the first pause had closed and taking that
    // pause down with it.
    AtomicReference<PauseResult> secondResult = new AtomicReference<>();
    Thread secondPause =
        new Thread(() -> secondResult.set(gateKeeper.pauseAndAwaitDrained(10, TimeUnit.SECONDS)));
    secondPause.start();
    awaitThreadState(secondPause, Thread.State.TIMED_WAITING);
    assertThat(secondResult.get()).isNull();
    // It waits without having touched the gate: the first pause still owns the closure it made.
    assertThat(gateKeeper.isOpen()).isFalse();

    gateKeeper.letOut();
    joinPromptly(firstPause);
    joinPromptly(secondPause);

    // Assert: both pauses succeed and the gate stays closed, as they did before pauses were
    // serialized -- but now without either of them being able to reopen the other's closure.
    assertThat(firstResult.get()).isEqualTo(PauseResult.PAUSED);
    assertThat(secondResult.get()).isEqualTo(PauseResult.PAUSED);
    assertThat(gateKeeper.isOpen()).isFalse();
  }

  @Test
  public void pauseAndAwaitDrained_WhenAnotherPauseDoesNotFinishInTime_ShouldReturnPauseInProgress()
      throws InterruptedException {
    // Arrange: the first pause holds the gate for far longer than the second is willing to wait.
    gateKeeper.letIn();

    AtomicReference<PauseResult> firstResult = new AtomicReference<>();
    Thread firstPause =
        new Thread(() -> firstResult.set(gateKeeper.pauseAndAwaitDrained(10, TimeUnit.SECONDS)));
    firstPause.start();
    awaitThreadState(firstPause, Thread.State.TIMED_WAITING);

    // Act
    PauseResult secondResult = gateKeeper.pauseAndAwaitDrained(50, TimeUnit.MILLISECONDS);

    // Assert: waiting is bounded by the caller's own time limit, and giving up leaves the gate
    // exactly as the first pause left it rather than closing it behind that pause's back.
    assertThat(secondResult).isEqualTo(PauseResult.PAUSE_IN_PROGRESS);
    assertThat(gateKeeper.isOpen()).isFalse();

    // The first pause still completes normally once its request drains.
    gateKeeper.letOut();
    joinPromptly(firstPause);
    assertThat(firstResult.get()).isEqualTo(PauseResult.PAUSED);
  }

  @Test
  public void pauseAndAwaitDrained_AfterAnEarlierPauseFinished_ShouldNotReportPauseInProgress() {
    // Arrange: the in-progress marker must be cleared once a pause finishes, or every later pause
    // would be rejected.
    assertThat(gateKeeper.pauseAndAwaitDrained(10, TimeUnit.SECONDS)).isEqualTo(PauseResult.PAUSED);

    // Act
    PauseResult pauseResult = gateKeeper.pauseAndAwaitDrained(10, TimeUnit.SECONDS);

    // Assert
    assertThat(pauseResult).isEqualTo(PauseResult.PAUSED);
  }

  @Test
  public void pauseAndAwaitDrained_AfterAnEarlierPauseTimedOutAndReopenedTheGate_ShouldPause() {
    // Arrange: a pause that fails has to clear the in-progress marker just as one that succeeds
    // does. Leaving it set would turn a single timed-out pause into a permanently unusable pause
    // endpoint, with every later request reported as PAUSE_IN_PROGRESS and nothing to recover it.
    gateKeeper.letIn();
    assertThat(gateKeeper.pauseAndAwaitDrained(50, TimeUnit.MILLISECONDS))
        .isEqualTo(PauseResult.TIMED_OUT);
    gateKeeper.letOut();

    // Act
    PauseResult pauseResult = gateKeeper.pauseAndAwaitDrained(10, TimeUnit.SECONDS);

    // Assert
    assertThat(pauseResult).isEqualTo(PauseResult.PAUSED);
    assertThat(gateKeeper.isOpen()).isFalse();
  }

  @Test
  public void pauseAndAwaitDrained_AfterAnEarlierPauseTimedOutWhileStillPaused_ShouldPause() {
    // Arrange: the other timeout outcome returns without reopening the gate, so it leaves the
    // in-progress marker by a different path than the one above and needs its own coverage.
    gateKeeper.letIn();
    assertThat(gateKeeper.pause()).isEqualTo(PauseResult.PAUSED);
    assertThat(gateKeeper.pauseAndAwaitDrained(50, TimeUnit.MILLISECONDS))
        .isEqualTo(PauseResult.TIMED_OUT_STILL_PAUSED);
    gateKeeper.letOut();

    // Act
    PauseResult pauseResult = gateKeeper.pauseAndAwaitDrained(10, TimeUnit.SECONDS);

    // Assert
    assertThat(pauseResult).isEqualTo(PauseResult.PAUSED);
    assertThat(gateKeeper.isOpen()).isFalse();
  }
}
