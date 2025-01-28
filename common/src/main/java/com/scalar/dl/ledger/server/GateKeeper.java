package com.scalar.dl.ledger.server;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/** A gatekeeper that manages the front gate of the server that processes requests. */
@ThreadSafe
public class GateKeeper {

  @GuardedBy("this")
  private boolean isOpen;

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
   * Closes the gate to disallow incoming requests to be processed. After this call returns, new
   * requests are rejected. Note that this method will not wait for outstanding requests to finish
   * before returning. awaitDrained(long, TimeUnit) needs to be called to wait for outstanding
   * requests to finish.
   */
  public synchronized void close() {
    isOpen = false;
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
   * Waits for the server to finish outstanding requests, giving up if the timeout is reached.
   *
   * @param timeout the maximum time to wait
   * @param unit the time unit of the timeout argument
   * @return true if the server finishes outstanding requests and false otherwise
   */
  public synchronized boolean awaitDrained(long timeout, TimeUnit unit) {
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
      return numOutstandingRequests == 0;
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
    if (--numOutstandingRequests == 0) {
      notifyAll();
    }
  }
}
