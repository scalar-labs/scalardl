package com.scalar.application.bankaccount.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AccountTest {
  private static final String ACCOUNT_ID = "account-id";
  private static final long BALANCE = 123;
  private Account account;

  @BeforeEach
  public void setUp() {
    account = new Account(ACCOUNT_ID, BALANCE);
  }

  @Test
  public void constructor_ShouldCreateANewAccount() {
    // Act-assert
    assertThatCode(() -> new Account(ACCOUNT_ID, BALANCE)).doesNotThrowAnyException();
  }

  @Test
  public void getId_OnAnAccount_ShouldReturnTheCorrectId() {
    // Assert
    assertThat(account.getId()).isEqualTo(ACCOUNT_ID);
  }

  @Test
  public void getBalance_OnANewAccount_ShouldReturnCorrectBalance() {
    // Assert
    assertThat(account.getBalance()).isEqualTo(BALANCE);
  }
}
