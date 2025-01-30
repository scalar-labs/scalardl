package com.scalar.dl.ledger.server;

import com.google.inject.Inject;
import com.google.protobuf.Empty;
import com.scalar.admin.rpc.AdminGrpc;
import com.scalar.admin.rpc.CheckPausedResponse;
import com.scalar.admin.rpc.PauseRequest;
import com.scalar.admin.rpc.StatsResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.ThreadSafe;
import javax.json.Json;
import javax.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
public class AdminService extends AdminGrpc.AdminImplBase {
  private static final Logger LOGGER = LoggerFactory.getLogger(AdminService.class.getName());
  private static final long DEFAULT_MAX_PAUSE_WAIT_TIME_MILLIS = 30000; // 30 seconds
  private final Stats stats;
  private final GateKeeper gateKeeper;

  @Inject
  public AdminService(Stats stats, GateKeeper gateKeeper) {
    this.stats = stats;
    this.gateKeeper = gateKeeper;
  }

  @Override
  public void pause(PauseRequest request, StreamObserver<Empty> responseObserver) {
    gateKeeper.close();

    if (request.getWaitOutstanding()) {
      boolean drained = false;
      long maxPauseWaitTime =
          request.getMaxPauseWaitTime() != 0
              ? request.getMaxPauseWaitTime()
              : DEFAULT_MAX_PAUSE_WAIT_TIME_MILLIS;

      try {
        drained = gateKeeper.awaitDrained(maxPauseWaitTime, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        // ignored
      }
      if (!drained) {
        gateKeeper.open();
        LOGGER.warn("failed to finish processing outstanding requests within the time limit.");
        responseObserver.onError(new StatusRuntimeException(Status.FAILED_PRECONDITION));
        return;
      }
    }
    LOGGER.warn("Paused");

    responseObserver.onNext(Empty.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void unpause(Empty request, StreamObserver<Empty> responseObserver) {
    gateKeeper.open();
    LOGGER.warn("Unpaused");
    responseObserver.onNext(Empty.newBuilder().build());
    responseObserver.onCompleted();
  }

  // TODO: removed soon
  @Deprecated
  @Override
  public void stats(Empty request, StreamObserver<StatsResponse> responseObserver) {
    JsonObject json =
        Json.createObjectBuilder()
            .add("outstanding", gateKeeper.getNumOutstandingRequests())
            .add("total_succeeded", stats.getTotalSuccess())
            .add("total_failed", stats.getTotalFailure())
            .build();
    responseObserver.onNext(StatsResponse.newBuilder().setStats(json.toString()).build());
    responseObserver.onCompleted();
  }

  @Override
  public void checkPaused(Empty request, StreamObserver<CheckPausedResponse> responseObserver) {
    responseObserver.onNext(
        CheckPausedResponse.newBuilder().setPaused(!gateKeeper.isOpen()).build());
    responseObserver.onCompleted();
  }
}
