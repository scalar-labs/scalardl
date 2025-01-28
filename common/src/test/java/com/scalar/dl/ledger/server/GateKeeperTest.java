package com.scalar.dl.ledger.server;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GateKeeperTest {
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
  public void letIn_GateClosed_ShouldWaitBeforeIncrementing() {
    // Arrange
    gateKeeper.close();
    AtomicBoolean letInFinished = new AtomicBoolean(false);

    // Act
    new Thread(
            () -> {
              gateKeeper.letIn();
              letInFinished.set(true);
            })
        .start();

    // Assert
    Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
    assertThat(letInFinished.get()).isFalse();
    gateKeeper.open();
    Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
    assertThat(letInFinished.get()).isTrue();
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
  public void close_OnAnyCondition_ShouldClose() {
    // Arrange
    // Act
    gateKeeper.close();

    // Assert
    assertThat(gateKeeper.isOpen()).isFalse();
  }

  @Test
  public void awaitDrained_NoOutstandingExists_ShouldReturnTrueWithoutWaiting() {
    // Arrange
    gateKeeper.close();

    // Act
    boolean actual = gateKeeper.awaitDrained(1, TimeUnit.MILLISECONDS);

    // Assert
    assertThat(actual).isTrue();
  }

  @Test
  public void awaitDrained_OutstandingExists_ShouldWaitUntilNotifiedAndReturnTrue() {
    // Arrange
    new Thread(
            () -> {
              gateKeeper.letIn();
              Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
              gateKeeper.letOut();
            })
        .start();
    Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
    gateKeeper.close();

    // Act
    boolean actual = gateKeeper.awaitDrained(2, TimeUnit.SECONDS);

    // Assert
    assertThat(actual).isTrue();
  }

  @Test
  public void awaitDrained_OutstandingExistsButTimedOut_ShouldWaitAndReturnFalse() {
    // Arrange
    gateKeeper.letIn();
    gateKeeper.close();

    // Act
    boolean actual = gateKeeper.awaitDrained(1, TimeUnit.MILLISECONDS);

    // Assert
    assertThat(actual).isFalse();
  }
}
