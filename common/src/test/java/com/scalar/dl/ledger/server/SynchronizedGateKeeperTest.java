package com.scalar.dl.ledger.server;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;

public class SynchronizedGateKeeperTest {
  private SynchronizedGateKeeper gateKeeper;

  @Before
  public void setUp() {
    gateKeeper = new SynchronizedGateKeeper();
  }

  @Test
  public void letIn_GateOpened_ShouldIncrementOutstandingReturnTrue() {
    // Arrange
    // Act
    boolean actual = gateKeeper.letIn();

    // Assert
    assertThat(actual).isTrue();
    assertThat(gateKeeper.getNumOutstandingRequests()).isEqualTo(1);
  }

  @Test
  public void letIn_GateClosed_ShouldReturnFalseWithoutIncrementing() {
    // Arrange
    gateKeeper.close();

    // Act
    boolean actual = gateKeeper.letIn();

    // Assert
    assertThat(actual).isFalse();
    assertThat(gateKeeper.getNumOutstandingRequests()).isEqualTo(0);
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
  public void awaitDrained_NoOutstandingExists_ShouldReturnTrueWithoutWaiting()
      throws InterruptedException {
    // Arrange
    gateKeeper.close();

    // Act
    boolean actual = gateKeeper.awaitDrained(1, TimeUnit.MILLISECONDS);

    // Assert
    assertThat(actual).isTrue();
  }

  @Test
  public void awaitDrained_OutstandingExists_ShouldWaitUntilNotifiedAndReturnTrue()
      throws InterruptedException {
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
  public void awaitDrained_OutstandingExistsButTimedOut_ShouldWaitAndReturnFalse()
      throws InterruptedException {
    // Arrange
    gateKeeper.letIn();
    gateKeeper.close();

    // Act
    boolean actual = gateKeeper.awaitDrained(1, TimeUnit.MILLISECONDS);

    // Assert
    assertThat(actual).isFalse();
  }
}
