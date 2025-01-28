package com.scalar.dl.ledger.server;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.Uninterruptibles;
import com.google.protobuf.Empty;
import com.scalar.dl.ledger.exception.LedgerException;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.service.ThrowableConsumer;
import com.scalar.dl.ledger.service.ThrowableFunction;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class CommonServiceTest {
  @Mock private Stats stats;
  @Spy private GateKeeper gateKeeper;
  @Mock private StreamObserver<Empty> observerWithEmpty;
  @Mock private StreamObserver<String> observerWithString;
  private CommonService service;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void
      serve_StatsAndGateKeeperNonnullAndConsumerGiven_ShouldProcessGivenConsumerWithStats() {
    // Arrange
    service = new CommonService(stats, gateKeeper);
    ThrowableConsumer<String> f = r -> {};
    String request = "test";

    // Act
    service.serve(f, request, observerWithEmpty);

    // Assert
    verify(gateKeeper).letIn();
    verify(gateKeeper).letOut();
    verify(stats).incrementTotalSuccess();
    verify(stats, never()).incrementTotalFailure();
    verify(stats).incrementCounter(request.getClass().getSimpleName(), true);
    verify(stats, never()).incrementCounter(request.getClass().getSimpleName(), false);
    verify(observerWithEmpty).onNext(any());
    verify(observerWithEmpty).onCompleted();
  }

  @Test
  public void
      serve_StatsAndGateKeeperNonnullAndFunctionGiven_ShouldProcessGivenFunctionWithStats() {
    // Arrange
    service = new CommonService(stats, gateKeeper);
    ThrowableFunction<String, String> f = r -> r;
    String request = "test";

    // Act
    service.serve(f, request, observerWithString);

    // Assert
    verify(gateKeeper).letIn();
    verify(gateKeeper).letOut();
    verify(stats).incrementTotalSuccess();
    verify(stats, never()).incrementTotalFailure();
    verify(stats).incrementCounter(request.getClass().getSimpleName(), true);
    verify(stats, never()).incrementCounter(request.getClass().getSimpleName(), false);
    verify(observerWithString).onNext(any());
    verify(observerWithString).onCompleted();
  }

  @Test
  public void
      serve_StatsAndGateKeeperNonnullAndConsumerGivenButLedgerExceptionThrown_ShouldCallOnError() {
    // Arrange
    service = new CommonService(stats, gateKeeper);
    ThrowableConsumer<String> f =
        r -> {
          throw new LedgerException("test", StatusCode.DATABASE_ERROR);
        };
    String request = "test";

    // Act
    service.serve(f, request, observerWithEmpty);

    // Assert
    verify(gateKeeper).letIn();
    verify(gateKeeper).letOut();
    verify(stats, never()).incrementTotalSuccess();
    verify(stats).incrementTotalFailure();
    verify(stats, never()).incrementCounter(request.getClass().getSimpleName(), true);
    verify(stats).incrementCounter(request.getClass().getSimpleName(), false);
    verify(observerWithEmpty).onError(any());
  }

  @Test
  public void
      serve_StatsAndGateKeeperNonnullAndFunctionGivenButLedgerExceptionThrown_ShouldCallOnError() {
    // Arrange
    service = new CommonService(stats, gateKeeper);
    doNothing().when(gateKeeper).letIn();
    ThrowableFunction<String, String> f =
        r -> {
          throw new LedgerException("test", StatusCode.DATABASE_ERROR);
        };
    String request = "test";

    // Act
    service.serve(f, request, observerWithString);

    // Assert
    verify(gateKeeper).letIn();
    verify(gateKeeper).letOut();
    verify(stats, never()).incrementTotalSuccess();
    verify(stats).incrementTotalFailure();
    verify(stats, never()).incrementCounter(request.getClass().getSimpleName(), true);
    verify(stats).incrementCounter(request.getClass().getSimpleName(), false);
    verify(observerWithString).onError(any());
  }

  @Test
  public void serve_GateKeeperNonnullButClosedAndConsumerGiven_ShouldBlockWithoutAccept() {
    // Arrange
    gateKeeper.close();
    service = new CommonService(stats, gateKeeper);
    ThrowableConsumer<String> f = r -> {};
    String request = "test";

    // Act
    new Thread(() -> service.serve(f, request, observerWithEmpty)).start();
    Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);

    // Assert
    verify(gateKeeper).letIn();
    verify(gateKeeper, never()).letOut();
    verify(stats, never()).incrementTotalSuccess();
    verify(stats, never()).incrementTotalFailure();
    verify(stats, never()).incrementCounter(request.getClass().getSimpleName(), true);
    verify(stats, never()).incrementCounter(request.getClass().getSimpleName(), false);
    verify(observerWithEmpty, never()).onNext(any());
    verify(observerWithEmpty, never()).onError(any());
    gateKeeper.open();
    Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
  }

  @Test
  public void serve_GateKeeperNonnullButClosedAndFunctionGiven_ShouldBlockWithoutApply() {
    // Arrange
    gateKeeper.close();
    service = new CommonService(stats, gateKeeper);
    ThrowableFunction<String, String> f = r -> r;
    String request = "test";

    // Ac
    new Thread(() -> service.serve(f, request, observerWithString)).start();
    Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);

    // Assert
    verify(gateKeeper).letIn();
    verify(gateKeeper, never()).letOut();
    verify(stats, never()).incrementTotalSuccess();
    verify(stats, never()).incrementTotalFailure();
    verify(stats, never()).incrementCounter(request.getClass().getSimpleName(), true);
    verify(stats, never()).incrementCounter(request.getClass().getSimpleName(), false);
    verify(observerWithString, never()).onNext(any());
    verify(observerWithString, never()).onError(any());
    gateKeeper.open();
    Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
  }

  @Test
  public void serve_StatsNullAndConsumerGiven_ShouldProcessGivenConsumerWithoutStats() {
    // Arrange
    service = new CommonService(null, gateKeeper);
    ThrowableConsumer<String> f = r -> {};
    String request = "test";

    // Act
    service.serve(f, request, observerWithEmpty);

    // Assert
    verify(gateKeeper).letIn();
    verify(gateKeeper).letOut();
    verify(stats, never()).incrementTotalSuccess();
    verify(stats, never()).incrementTotalFailure();
    verify(stats, never()).incrementCounter(request.getClass().getSimpleName(), true);
    verify(stats, never()).incrementCounter(request.getClass().getSimpleName(), false);
    verify(observerWithEmpty).onNext(any());
    verify(observerWithEmpty).onCompleted();
  }

  @Test
  public void serve_StatsNullAndFunctionGiven_ShouldProcessGivenFunctionWithoutStats() {
    // Arrange
    service = new CommonService(null, gateKeeper);
    ThrowableFunction<String, String> f = r -> r;
    String request = "test";

    // Act
    service.serve(f, request, observerWithString);

    // Assert
    verify(gateKeeper).letIn();
    verify(gateKeeper).letOut();
    verify(stats, never()).incrementTotalSuccess();
    verify(stats, never()).incrementTotalFailure();
    verify(stats, never()).incrementCounter(request.getClass().getSimpleName(), true);
    verify(stats, never()).incrementCounter(request.getClass().getSimpleName(), false);
    verify(observerWithString).onNext(any());
    verify(observerWithString).onCompleted();
  }
}
