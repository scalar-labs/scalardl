package com.scalar.dl.ledger.server;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.google.common.annotations.VisibleForTesting;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A gatekeeper that manages the front gate of the server that processes requests. */
@ThreadSafe
public class GateKeeper {
  private static final Logger LOGGER = LoggerFactory.getLogger(GateKeeper.class.getName());

  @GuardedBy("this")
  private boolean isOpen;

  /**
   * True while a pause request is between closing the gate and deciding whether it has to undo that
   * closure. Pause requests are serialized on this flag, so that one of them can never reach a
   * paused state and report it while another is still in a position to reopen the gate underneath
   * it. Clearing it notifies the monitor, so a pause waiting for its turn can take the gate.
   */
  @GuardedBy("this")
  private boolean pauseInProgress;

  @GuardedBy("this")
  private int numOutstandingRequests;

  public GateKeeper() {
    isOpen = true;
  }

  /** Opens the gate to allow incoming requests to be processed. */
  public synchronized void open() {
    isOpen = true;
    notifyAll();
  }

  /**
   * Closes the gate on behalf of a pause request, without waiting for outstanding requests to
   * finish. After this method returns {@link PauseResult#PAUSED}, new requests are held at the
   * gate.
   *
   * <p>Unlike {@link #pauseAndAwaitDrained(long, TimeUnit)}, this method never waits for a pause
   * that is already in progress: its caller asked not to wait, and that pause can still reopen the
   * gate, so there is nothing to report but {@link PauseResult#PAUSE_IN_PROGRESS}.
   *
   * @return {@link PauseResult#PAUSED} if the gate is closed on the caller's behalf; {@link
   *     PauseResult#PAUSE_IN_PROGRESS} if another pause request is already in progress
   */
  public synchronized PauseResult pause() {
    if (pauseInProgress) {
      return PauseResult.PAUSE_IN_PROGRESS;
    }
    close();
    return PauseResult.PAUSED;
  }

  /**
   * Closes the gate on behalf of a pause request and waits for outstanding requests to finish,
   * giving up if the timeout is reached.
   *
   * <p>If a paused state is not reached, a closure made by this call is undone before returning, so
   * that a pause which failed does not leave the server paused. A closure merely inherited from an
   * earlier pause is left alone, because that pause has already been reported as successful to its
   * own caller and reopening the gate would silently revoke it.
   *
   * <p>Pause requests are serialized against each other: while one is in progress, another waits
   * for it to finish instead of closing the gate behind its back. Waiting rather than failing fast
   * matters because a caller that unpauses to recover from a failed pause would answer a fail-fast
   * rejection with an unpause, and that unpause would reopen the gate the pause in progress had
   * closed, taking that pause down as well. The wait is bounded by the caller's own timeout, which
   * covers waiting for the pause in progress and draining together, so a pause never blocks longer
   * than it was asked to. {@link #open()} is deliberately left unserialized, so an unpause can
   * always interrupt a pause that is stuck draining.
   *
   * @param timeout the maximum time to wait, covering both waiting for a pause already in progress
   *     and waiting for outstanding requests to finish
   * @param unit the time unit of the timeout argument
   * @return {@link PauseResult#PAUSED} if the gate is closed and all outstanding requests have
   *     finished; {@link PauseResult#TIMED_OUT} if the timeout was reached with requests still
   *     outstanding and the gate has been reopened; {@link PauseResult#TIMED_OUT_STILL_PAUSED} if
   *     the timeout was reached but the gate stays closed under an earlier pause; {@link
   *     PauseResult#GATE_OPEN} if a concurrent unpause reopened the gate; {@link
   *     PauseResult#PAUSE_IN_PROGRESS} if another pause request was still in progress when the
   *     timeout was reached
   */
  public synchronized PauseResult pauseAndAwaitDrained(long timeout, TimeUnit unit) {
    long deadlineNanos = System.nanoTime() + unit.toNanos(timeout);
    boolean interrupted = false;
    try {
      // The two waits below are indistinguishable to the caller until the call returns, yet they
      // call for different remedies: draining points at a long-running request, waiting here
      // points at whoever holds the pause. Log them separately, in the order they happen.
      if (pauseInProgress) {
        LOGGER.info("Waiting for another pause request in progress to finish");
      }
      while (pauseInProgress) {
        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0) {
          // The pause in progress owns the gate and may still reopen it, so this call can neither
          // report a paused state nor touch the gate.
          return PauseResult.PAUSE_IN_PROGRESS;
        }
        try {
          NANOSECONDS.timedWait(this, remainingNanos);
        } catch (InterruptedException ignored) {
          interrupted = true;
        }
      }
      pauseInProgress = true;
      try {
        boolean closedByThisCall = close();
        LOGGER.info("Waiting until outstanding requests are all finished");
        // Whatever is left of the caller's time limit after waiting for a pause in progress. A
        // non-positive remainder still lets awaitDrained report an already-drained gate as paused.
        PauseResult pauseResult = awaitDrained(deadlineNanos - System.nanoTime(), NANOSECONDS);
        if (pauseResult == PauseResult.PAUSED) {
          return pauseResult;
        }
        if (closedByThisCall) {
          open();
          return pauseResult;
        }
        // The gate is left as it is, so the caller has to be told that it is still closed.
        // Otherwise it could take its own failure for an unpaused server and "recover" by
        // unpausing, which would revoke the earlier pause that owns the gate.
        return pauseResult == PauseResult.TIMED_OUT
            ? PauseResult.TIMED_OUT_STILL_PAUSED
            : pauseResult;
      } finally {
        pauseInProgress = false;
        // Hand the gate over to a pause request waiting for this one to finish.
        notifyAll();
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Returns if the gate is open or not.
   *
   * @return true if the gate is open
   */
  public synchronized boolean isOpen() {
    return isOpen;
  }

  /**
   * Returns the number of outstanding requests.
   *
   * @return the number of outstanding requests
   */
  public synchronized int getNumOutstandingRequests() {
    return numOutstandingRequests;
  }

  /**
   * Closes the gate to disallow incoming requests to be processed.
   *
   * @return true if this call closed the gate, false if it was already closed. Only the caller that
   *     closed the gate may reopen it to undo its own closure.
   */
  @VisibleForTesting
  synchronized boolean close() {
    if (!isOpen) {
      return false;
    }
    isOpen = false;
    return true;
  }

  /**
   * Waits for the server to finish outstanding requests, giving up if the timeout is reached. The
   * gate is expected to have been closed before this is called.
   *
   * @param timeout the maximum time to wait
   * @param unit the time unit of the timeout argument
   * @return {@link PauseResult#PAUSED} if the gate is still closed and all outstanding requests
   *     have finished; {@link PauseResult#TIMED_OUT} if the timeout was reached with requests still
   *     outstanding; {@link PauseResult#GATE_OPEN} if the gate is open
   */
  @VisibleForTesting
  synchronized PauseResult awaitDrained(long timeout, TimeUnit unit) {
    boolean interrupted = false;
    try {
      long timeoutNanos = unit.toNanos(timeout);
      long endTimeNanos = System.nanoTime() + timeoutNanos;
      while (!isOpen
          && numOutstandingRequests > 0
          && (timeoutNanos = endTimeNanos - System.nanoTime()) > 0) {
        try {
          NANOSECONDS.timedWait(this, timeoutNanos);
        } catch (InterruptedException ignored) {
          interrupted = true;
        }
      }
      // The outcome is decided under this synchronized method, so it is atomic w.r.t. open() /
      // close() / letIn() / letOut(). That keeps a concurrent unpause (open()) from being mistaken
      // for a drained pause, and lets the caller report the cause it actually observed instead of
      // re-deriving it from a later, separately-locked state snapshot.
      if (isOpen) {
        return PauseResult.GATE_OPEN;
      }
      return numOutstandingRequests == 0 ? PauseResult.PAUSED : PauseResult.TIMED_OUT;
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Lets a new request in to process it in the server if the gate is open. If the gate is closed,
   * waits until the gate is open.
   */
  public synchronized void letIn() {
    boolean interrupted = false;
    try {
      while (!isOpen) {
        try {
          wait();
        } catch (InterruptedException ignored) {
          interrupted = true;
        }
      }
      numOutstandingRequests++;
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /** Lets a processed request out. */
  public synchronized void letOut() {
    // Clamped so that a call not paired with a letIn() cannot drive the count below zero. A
    // negative count would leave the drain loop below with nothing to wait for while never
    // satisfying its success condition either, so every later pause that waits for outstanding
    // requests would report a timeout it never waited for, with no way back short of a restart.
    if (numOutstandingRequests > 0) {
      numOutstandingRequests--;
    }
    if (numOutstandingRequests == 0) {
      notifyAll();
    }
  }

  /** The outcome of a pause request. */
  public enum PauseResult {
    /**
     * The gate is closed on the caller's behalf. When returned from {@link
     * #pauseAndAwaitDrained(long, TimeUnit)}, no request is outstanding either; when returned from
     * {@link #pause()}, outstanding requests are not waited for and may still be running.
     */
    PAUSED,
    /**
     * Requests were still outstanding when the timeout was reached, and the closure this call made
     * has been undone, so the gate is open again.
     */
    TIMED_OUT,
    /**
     * Requests were still outstanding when the timeout was reached, but the gate stays closed
     * because an earlier pause owns it. The server is paused even though this request failed, so
     * unpausing to recover from this outcome would revoke that earlier pause.
     */
    TIMED_OUT_STILL_PAUSED,
    /**
     * The gate is open, so no paused state can be reported. For a caller that closed the gate
     * first, this means a concurrent {@link #open()} reopened it.
     */
    GATE_OPEN,
    /**
     * Another pause request was in progress and this one did not get the gate before its time ran
     * out, so it did not touch the gate. The other pause may hold the gate closed, so unpausing to
     * recover from this outcome would revoke it; retrying is the only safe response.
     */
    PAUSE_IN_PROGRESS
  }
}
