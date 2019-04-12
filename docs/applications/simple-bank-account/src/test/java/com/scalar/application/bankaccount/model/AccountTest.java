package com.scalar.application.bankaccount.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatCode;

import org.junit.Before;
import org.junit.Test;

public class AccountTest {
  private static final String ACCOUNT_ID = "account-id";
  private static final long BALANCE = 123;
  private Account account;

  @Before
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
