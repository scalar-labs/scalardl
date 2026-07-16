package com.scalar.dl.ledger.database.scalardb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.exception.transaction.CrudException;
import org.junit.jupiter.api.Test;

public class PrivilegedTest {

  @Test
  public void transactionCrud_OperationSucceeding_ShouldReturnResult() throws CrudException {
    // Arrange Act
    String result = Privileged.transactionCrud(() -> "result");

    // Assert
    assertThat(result).isEqualTo("result");
  }

  @Test
  public void transactionCrud_OperationThrowingCrudException_ShouldThrowItAsIs() {
    // Arrange
    CrudException toThrow = new CrudException("failed", "tx-id");

    // Act Assert
    assertThatThrownBy(
            () ->
                Privileged.transactionCrud(
                    () -> {
                      throw toThrow;
                    }))
        .isSameAs(toThrow);
  }

  @Test
  public void transactionCrud_OperationThrowingRuntimeException_ShouldThrowItAsIs() {
    // Arrange
    RuntimeException toThrow = new IllegalArgumentException("failed");

    // Act Assert
    assertThatThrownBy(
            () ->
                Privileged.transactionCrud(
                    () -> {
                      throw toThrow;
                    }))
        .isSameAs(toThrow);
  }

  @Test
  public void storageCrud_OperationSucceeding_ShouldReturnResult() throws ExecutionException {
    // Arrange Act
    String result = Privileged.storageCrud(() -> "result");

    // Assert
    assertThat(result).isEqualTo("result");
  }

  @Test
  public void storageCrud_OperationThrowingExecutionException_ShouldThrowItAsIs() {
    // Arrange
    ExecutionException toThrow = new ExecutionException("failed");

    // Act Assert
    assertThatThrownBy(
            () ->
                Privileged.storageCrud(
                    () -> {
                      throw toThrow;
                    }))
        .isSameAs(toThrow);
  }

  @Test
  public void storageCrud_OperationThrowingRuntimeException_ShouldThrowItAsIs() {
    // Arrange
    RuntimeException toThrow = new IllegalStateException("failed");

    // Act Assert
    assertThatThrownBy(
            () ->
                Privileged.storageCrud(
                    () -> {
                      throw toThrow;
                    }))
        .isSameAs(toThrow);
  }
}
