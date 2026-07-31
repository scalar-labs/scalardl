package com.scalar.dl.ledger.server;

import com.google.inject.Inject;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.rpc.ErrorInfo;
import com.scalar.admin.rpc.AdminGrpc;
import com.scalar.admin.rpc.CheckPausedResponse;
import com.scalar.admin.rpc.PauseRequest;
import com.scalar.dl.ledger.server.GateKeeper.PauseResult;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
public class AdminService extends AdminGrpc.AdminImplBase {
  private static final Logger LOGGER = LoggerFactory.getLogger(AdminService.class.getName());
  private static final long DEFAULT_MAX_PAUSE_WAIT_TIME_MILLIS = 30000; // 30 seconds

  /**
   * The domain of the {@link ErrorInfo} detail attached to a failed pause. Deliberately distinct
   * from ScalarDL's own error domain, because these reasons are {@link PauseResult} values rather
   * than ScalarDL error codes, and the two must not be parsed by the same code.
   */
  private static final String ERROR_DOMAIN = "com.scalar.dl.admin";

  private final GateKeeper gateKeeper;

  @Inject
  public AdminService(GateKeeper gateKeeper) {
    this.gateKeeper = gateKeeper;
  }

  /**
   * Pauses the server, optionally waiting for outstanding requests to finish first.
   *
   * <p>A request that waits is rejected with {@code INVALID_ARGUMENT} if it specifies a
   * non-positive {@code max_pause_wait_time}, since such a limit has already elapsed when the wait
   * begins and would make a request that asked to wait behave as one that did not. That rejection
   * carries no {@link ErrorInfo} detail: no pause was attempted, so there is no outcome to report,
   * and the status code says all there is to say.
   *
   * <p>A pause that was attempted and failed carries an {@link ErrorInfo} detail in the status
   * trailers, whose domain is {@code com.scalar.dl.admin} and whose reason is the {@link
   * PauseResult} name. The reason, rather than the status code or the description text, is what a
   * caller branches on, because two outcomes share a status code while needing opposite recovery:
   *
   * <ul>
   *   <li>{@code PAUSE_IN_PROGRESS} / {@code GATE_OPEN} ({@code ABORTED}) -- another admin call won
   *       the race, so the gate is not this caller's to act on. Retry.
   *   <li>{@code TIMED_OUT} ({@code FAILED_PRECONDITION}) -- the closure this call made has been
   *       undone, so the gate is open again. Back off, or raise the wait time.
   *   <li>{@code TIMED_OUT_STILL_PAUSED} ({@code FAILED_PRECONDITION}) -- the gate stays closed
   *       under an earlier pause. The server is paused despite this failure, so unpausing to
   *       "recover" would revoke a pause whose caller was already told it succeeded.
   * </ul>
   */
  @Override
  public void pause(PauseRequest request, StreamObserver<Empty> responseObserver) {
    PauseResult pauseResult;
    long maxPauseWaitTime = 0;
    if (request.getWaitOutstanding()) {
      // The field is only validated here, on the path that uses it: a request that asked not to
      // wait has always ignored the value, and rejecting it there would fail calls that work today.
      if (request.hasMaxPauseWaitTime()) {
        maxPauseWaitTime = request.getMaxPauseWaitTime();
        if (maxPauseWaitTime <= 0) {
          // Passing this on would silently turn a request that asked to wait into one that does
          // not: the deadline would already have passed by the time the wait began. The field is
          // optional, so a caller that has no time limit in mind leaves it unset instead.
          String message =
              "max_pause_wait_time must be greater than 0 ms, but was "
                  + maxPauseWaitTime
                  + " ms; leave it unset to use the default of "
                  + DEFAULT_MAX_PAUSE_WAIT_TIME_MILLIS
                  + " ms";
          LOGGER.warn(message);
          responseObserver.onError(
              Status.INVALID_ARGUMENT.withDescription(message).asRuntimeException());
          return;
        }
      } else {
        maxPauseWaitTime = DEFAULT_MAX_PAUSE_WAIT_TIME_MILLIS;
      }
      // The gatekeeper logs which phase the pause is actually in, since it is the only place that
      // can tell waiting for another pause apart from draining outstanding requests.
      LOGGER.info("Pausing...");
      pauseResult = gateKeeper.pauseAndAwaitDrained(maxPauseWaitTime, TimeUnit.MILLISECONDS);
    } else {
      pauseResult = gateKeeper.pause();
    }

    if (pauseResult != PauseResult.PAUSED) {
      // The outcome was decided atomically inside the gatekeeper, so it describes what actually
      // happened rather than whatever the gate state looks like by now.
      String message = failureMessage(pauseResult, maxPauseWaitTime);
      LOGGER.warn(message);
      responseObserver.onError(failureException(pauseResult, message));
      return;
    }
    LOGGER.warn("Paused");

    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
  }

  /**
   * Builds the error reported for a failed pause, carrying the outcome as a machine-readable {@link
   * ErrorInfo} reason alongside the human-readable description.
   *
   * <p>The reason is the part a caller is meant to branch on, and it is what makes the outcomes
   * distinguishable at all: {@link PauseResult#TIMED_OUT} and {@link
   * PauseResult#TIMED_OUT_STILL_PAUSED} share a gRPC status code but need opposite recovery, since
   * only the former leaves the gate open and may therefore be followed by an unpause. The
   * description says the same thing for humans, but it is not a contract and must not be matched
   * on.
   *
   * <p>The detail rides in the status trailers rather than the response message, so this adds no
   * requirement on the {@code Admin} service definition, which is owned by a different project.
   * Callers that ignore the detail keep the behavior they had before it existed.
   */
  private static StatusRuntimeException failureException(PauseResult pauseResult, String message) {
    return StatusProto.toStatusRuntimeException(
        com.google.rpc.Status.newBuilder()
            .setCode(failureStatus(pauseResult).getCode().value())
            .setMessage(message)
            .addDetails(
                Any.pack(
                    ErrorInfo.newBuilder()
                        .setReason(pauseResult.name())
                        .setDomain(ERROR_DOMAIN)
                        .build()))
            .build());
  }

  private static String failureMessage(PauseResult pauseResult, long maxPauseWaitTime) {
    switch (pauseResult) {
      case GATE_OPEN:
        return "Pause did not complete: the gate was reopened by a concurrent unpause";
      case PAUSE_IN_PROGRESS:
        return "Pause did not start: another pause request is in progress";
      case TIMED_OUT_STILL_PAUSED:
        return "Pause did not complete: outstanding requests were not drained within "
            + maxPauseWaitTime
            + " ms; the gate stays closed by an earlier pause";
      case TIMED_OUT:
        return "Pause did not complete: outstanding requests were not drained within "
            + maxPauseWaitTime
            + " ms; the gate has been reopened";
      default:
        // PAUSED is not a failure and is filtered out by the caller.
        throw new AssertionError("Unexpected pause result: " + pauseResult);
    }
  }

  private static Status failureStatus(PauseResult pauseResult) {
    switch (pauseResult) {
      case PAUSE_IN_PROGRESS:
      case GATE_OPEN:
        // Losing a race with another admin call is a transient conflict that the caller can simply
        // retry, so it is reported as ABORTED rather than as a failed precondition.
        return Status.ABORTED;
      case TIMED_OUT:
      case TIMED_OUT_STILL_PAUSED:
        // A drain that ran out of time is not retryable on its own terms: retrying it unchanged
        // will most likely time out again.
        return Status.FAILED_PRECONDITION;
      default:
        // PAUSED is not a failure and is filtered out by the caller.
        throw new AssertionError("Unexpected pause result: " + pauseResult);
    }
  }

  @Override
  public void unpause(Empty request, StreamObserver<Empty> responseObserver) {
    gateKeeper.open();
    LOGGER.warn("Unpaused");
    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void checkPaused(Empty request, StreamObserver<CheckPausedResponse> responseObserver) {
    responseObserver.onNext(
        CheckPausedResponse.newBuilder().setPaused(!gateKeeper.isOpen()).build());
    responseObserver.onCompleted();
  }
}
