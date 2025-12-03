package com.scalar.dl.ledger.server;

import com.google.inject.Inject;
import com.google.protobuf.Empty;
import com.scalar.dl.ledger.exception.LedgerException;
import com.scalar.dl.ledger.server.Stats.TimerContext;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.service.ThrowableConsumer;
import com.scalar.dl.ledger.service.ThrowableFunction;
import com.scalar.dl.rpc.Status;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.StreamObserver;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonService {
  private static final Logger LOGGER = LoggerFactory.getLogger(CommonService.class.getName());
  private static final Metadata.Key<Status> STATUS_TRAILER_KEY =
      ProtoUtils.keyForProto(Status.getDefaultInstance());
  private final Stats stats;
  private final GateKeeper gateKeeper;

  @Inject
  public CommonService(Stats stats, @Nonnull GateKeeper gateKeeper) {
    this.stats = stats;
    this.gateKeeper = gateKeeper;
  }

  public <T> void serve(ThrowableConsumer<T> f, T request, StreamObserver<Empty> responseObserver) {
    boolean isGatePassed = false;
    try (TimerContext unused = measureTime(request.getClass().getSimpleName())) {
      gateKeeper.letIn();
      isGatePassed = true;

      f.accept(request);
      incrementCounter(request.getClass().getSimpleName(), true);

      responseObserver.onNext(Empty.newBuilder().build());
      responseObserver.onCompleted();
    } catch (LedgerException e) {
      LOGGER.error(e.getMessage(), e);
      incrementCounter(request.getClass().getSimpleName(), false);
      responseObserver.onError(getExceptionWithTrailers(e.getCode(), e.getMessage()));
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      incrementCounter(request.getClass().getSimpleName(), false);
      responseObserver.onError(getExceptionWithTrailers(StatusCode.RUNTIME_ERROR, e.getMessage()));
    } finally {
      if (isGatePassed) {
        gateKeeper.letOut();
      }
    }
  }

  public <T, R> void serve(
      ThrowableFunction<T, R> f, T request, StreamObserver<R> responseObserver) {
    boolean isGatePassed = false;

    try (TimerContext unused = measureTime(request.getClass().getSimpleName())) {
      gateKeeper.letIn();
      isGatePassed = true;

      R response = f.apply(request);
      incrementCounter(request.getClass().getSimpleName(), true);

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (LedgerException e) {
      LOGGER.error(e.getMessage(), e);
      incrementCounter(request.getClass().getSimpleName(), false);
      responseObserver.onError(getExceptionWithTrailers(e.getCode(), e.getMessage()));
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      incrementCounter(request.getClass().getSimpleName(), false);
      responseObserver.onError(getExceptionWithTrailers(StatusCode.RUNTIME_ERROR, e.getMessage()));
    } finally {
      if (isGatePassed) {
        gateKeeper.letOut();
      }
    }
  }

  public StatusRuntimeException getExceptionWithTrailers(StatusCode code, String message) {
    Metadata trailers = new Metadata();
    trailers.put(
        STATUS_TRAILER_KEY, Status.newBuilder().setCode(code.get()).setMessage(message).build());
    return new StatusRuntimeException(convert(code), trailers);
  }

  private void incrementCounter(String name, boolean isSucceeded) {
    if (stats != null) {
      stats.incrementCounter(name, isSucceeded);
      if (isSucceeded) {
        stats.incrementTotalSuccess();
      } else {
        stats.incrementTotalFailure();
      }
    }
  }

  private TimerContext measureTime(String name) {
    if (stats != null) {
      return stats.measureTime(name);
    }
    return Stats.emptyTimerContext();
  }

  private io.grpc.Status convert(StatusCode code) {
    switch (code) {
      case OK:
        return io.grpc.Status.OK;
      case INVALID_HASH:
      case INVALID_PREV_HASH:
      case INVALID_CONTRACT:
      case INVALID_OUTPUT:
      case INVALID_NONCE:
      case INCONSISTENT_STATES:
      case INCONSISTENT_REQUEST:
      case DATABASE_ERROR:
      case UNKNOWN_TRANSACTION_STATUS:
      case RUNTIME_ERROR:
        return io.grpc.Status.INTERNAL;
      case INVALID_SIGNATURE:
      case UNLOADABLE_KEY:
      case UNLOADABLE_CONTRACT:
      case INVALID_REQUEST:
      case CONTRACT_CONTEXTUAL_ERROR:
      case UNLOADABLE_FUNCTION:
      case INVALID_FUNCTION:
      case INVALID_ARGUMENT:
        return io.grpc.Status.INVALID_ARGUMENT;
      case CERTIFICATE_NOT_FOUND:
      case CONTRACT_NOT_FOUND:
      case ASSET_NOT_FOUND:
      case FUNCTION_NOT_FOUND:
      case SECRET_NOT_FOUND:
        return io.grpc.Status.NOT_FOUND;
      case CERTIFICATE_ALREADY_REGISTERED:
      case SECRET_ALREADY_REGISTERED:
      case CONTRACT_ALREADY_REGISTERED:
      case NAMESPACE_ALREADY_EXISTS:
        return io.grpc.Status.ALREADY_EXISTS;
      case UNAVAILABLE:
        return io.grpc.Status.UNAVAILABLE;
      case CONFLICT:
        return io.grpc.Status.FAILED_PRECONDITION;
      default:
        LOGGER.warn(code + " is not mapped to gRPC status code.");
        return io.grpc.Status.INTERNAL;
    }
  }
}
