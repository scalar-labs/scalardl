package com.ypeckstadt.escrow.common;

public class Constants {
  // Account related json names
  public static final String ACCOUNT_ASSET_TYPE = "account";
  public static final String ACCOUNT_ID = "id";
  public static final String ACCOUNT_NAME = "name";
  public static final String ACCOUNT_TIMESTAMP = "timestamp";
  public static final String ACCOUNT_BALANCE = "balance";
  public static final String ACCOUNT_BALANCE_CHANGE = "balance_change";

  // Contract error messages
  public static final String CONTRACT_ADD_ACCOUNT_MISSING_ARGUMENTS_ERROR =
      "wrong or missing arguments to create an account";
  public static final String CONTRACT_ADD_ACCOUNT_DUPLICATE_ERROR =
      "an account with this id already exists";
}
