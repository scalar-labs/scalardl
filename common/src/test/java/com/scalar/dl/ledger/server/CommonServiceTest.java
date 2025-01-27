package com.scalar.dl.ledger.server;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Empty;
import com.scalar.dl.ledger.exception.LedgerException;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.service.ThrowableConsumer;
import com.scalar.dl.ledger.service.ThrowableFunction;
import io.grpc.stub.StreamObserver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CommonServiceTest {
  @Mock private Stats stats;
  @Mock private GateKeeper gateKeeper;
  @Mock private StreamObserver<Empty> observerWithEmpty;
  @Mock private StreamObserver<String> observerWithString;
  private CommonService service;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void
      serve_StatsAndGateKeeperNonnullAndConsumerGiven_ShouldProcessGivenConsumerWithStats() {
    // Arrange
    service = new CommonService(stats, gateKeeper);
    when(gateKeeper.letIn()).thenReturn(true);
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
    when(gateKeeper.letIn()).thenReturn(true);
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
    when(gateKeeper.letIn()).thenReturn(true);
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
    when(gateKeeper.letIn()).thenReturn(true);
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
  public void serve_GateKeeperNonnullButClosedAndConsumerGiven_ShouldCallOnError() {
    // Arrange
    service = new CommonService(stats, gateKeeper);
    when(gateKeeper.letIn()).thenReturn(false);
    ThrowableConsumer<String> f = r -> {};
    String request = "test";

    // Act
    service.serve(f, request, observerWithEmpty);

    // Assert
    verify(gateKeeper).letIn();
    verify(gateKeeper, never()).letOut();
    verify(stats, never()).incrementTotalSuccess();
    verify(stats).incrementTotalFailure();
    verify(stats, never()).incrementCounter(request.getClass().getSimpleName(), true);
    verify(stats).incrementCounter(request.getClass().getSimpleName(), false);
    verify(observerWithEmpty).onError(any());
  }

  @Test
  public void serve_GateKeeperNonnullButClosedAndFunctionGiven_ShouldThrowLedgerException() {
    // Arrange
    service = new CommonService(stats, gateKeeper);
    when(gateKeeper.letIn()).thenReturn(false);
    ThrowableFunction<String, String> f = r -> r;
    String request = "test";

    // Act
    service.serve(f, request, observerWithString);

    // Assert
    verify(gateKeeper).letIn();
    verify(gateKeeper, never()).letOut();
    verify(stats, never()).incrementTotalSuccess();
    verify(stats).incrementTotalFailure();
    verify(stats, never()).incrementCounter(request.getClass().getSimpleName(), true);
    verify(stats).incrementCounter(request.getClass().getSimpleName(), false);
    verify(observerWithString).onError(any());
  }

  @Test
  public void
      serve_StatsAndGateKeeperNullAndConsumerGiven_ShouldProcessGivenConsumerWithoutStats() {
    // Arrange
    service = new CommonService(null, null);
    ThrowableConsumer<String> f = r -> {};
    String request = "test";

    // Act
    service.serve(f, request, observerWithEmpty);

    // Assert
    verify(gateKeeper, never()).letIn();
    verify(gateKeeper, never()).letOut();
    verify(stats, never()).incrementTotalSuccess();
    verify(stats, never()).incrementTotalFailure();
    verify(stats, never()).incrementCounter(request.getClass().getSimpleName(), true);
    verify(stats, never()).incrementCounter(request.getClass().getSimpleName(), false);
    verify(observerWithEmpty).onNext(any());
    verify(observerWithEmpty).onCompleted();
  }

  @Test
  public void
      serve_StatsAndGateKeeperNullAndFunctionGiven_ShouldProcessGivenFunctionWithoutStats() {
    // Arrange
    service = new CommonService(null, null);
    ThrowableFunction<String, String> f = r -> r;
    String request = "test";

    // Act
    service.serve(f, request, observerWithString);

    // Assert
    verify(gateKeeper, never()).letIn();
    verify(gateKeeper, never()).letOut();
    verify(stats, never()).incrementTotalSuccess();
    verify(stats, never()).incrementTotalFailure();
    verify(stats, never()).incrementCounter(request.getClass().getSimpleName(), true);
    verify(stats, never()).incrementCounter(request.getClass().getSimpleName(), false);
    verify(observerWithString).onNext(any());
    verify(observerWithString).onCompleted();
  }
}
