package com.scalar.dl.ledger.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Helpers for ordering the interleaving of a concurrency test without sleeping, shared by the tests
 * that drive a gatekeeper from more than one thread.
 */
final class ThreadTestUtils {

  /** The deadline for a thread to reach the state a test is waiting for. */
  private static final long THREAD_STATE_TIMEOUT_MILLIS = 5_000;

  /**
   * The deadline for a blocked thread to be woken up. Must stay well below the timeouts the tests
   * give the threads they observe, so that a waiter that is never notified fails the test instead
   * of quietly succeeding once its own timeout expires.
   */
  private static final long WAKE_UP_TIMEOUT_MILLIS = 2_000;

  private ThreadTestUtils() {}

  /** Spins until the thread is parked in the given state, giving up at the deadline. */
  static void awaitThreadState(Thread thread, Thread.State state) {
    long deadlineNanos =
        System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(THREAD_STATE_TIMEOUT_MILLIS);
    while (thread.getState() != state) {
      if (System.nanoTime() - deadlineNanos > 0) {
        fail(
            "the thread did not reach %s within %d ms (last observed state: %s)",
            state, THREAD_STATE_TIMEOUT_MILLIS, thread.getState());
      }
      LockSupport.parkNanos(1_000L);
    }
  }

  /**
   * Joins the thread, requiring it to have been woken up promptly. A bare join() would also pass
   * when the thread is never notified and merely falls out of its own timeout much later.
   */
  static void joinPromptly(Thread thread) throws InterruptedException {
    thread.join(WAKE_UP_TIMEOUT_MILLIS);
    assertThat(thread.isAlive())
        .withFailMessage(
            "the thread was not woken up within %d ms, so it was never notified",
            WAKE_UP_TIMEOUT_MILLIS)
        .isFalse();
  }
}
