package com.scalar.dl.testing.contract;

/** Constants for test contracts and functions. */
public final class Constants {
  // Contract and Function argument keys
  public static final String ASSET_ATTRIBUTE_NAME = "asset_id";
  public static final String ASSETS_ATTRIBUTE_NAME = "asset_ids";
  public static final String AMOUNT_ATTRIBUTE_NAME = "amount";
  public static final String BALANCE_ATTRIBUTE_NAME = "balance";
  public static final String CONTRACT_ID_ATTRIBUTE_NAME = "contract_id";
  public static final String EXECUTE_NESTED_ATTRIBUTE_NAME = "execute_nested";
  public static final String NAMESPACE_ATTRIBUTE_NAME = "namespace";
  public static final String ID_ATTRIBUTE_NAME = "id";
  public static final String ASSET_ID_SEPARATOR = ":";

  // Contract IDs
  public static final String CREATE_CONTRACT_ID1 = "Create";
  public static final String CREATE_CONTRACT_ID2 = "CreateWithJsonp";
  public static final String CREATE_CONTRACT_ID3 = "CreateWithJackson";
  public static final String CREATE_CONTRACT_ID4 = "CreateWithString";
  public static final String PAYMENT_CONTRACT_ID1 = "Payment";
  public static final String PAYMENT_CONTRACT_ID2 = "PaymentWithJsonp";
  public static final String PAYMENT_CONTRACT_ID3 = "PaymentWithJackson";
  public static final String PAYMENT_CONTRACT_ID4 = "PaymentWithString";
  public static final String GET_BALANCE_CONTRACT_ID1 = "GetBalance";
  public static final String GET_BALANCE_CONTRACT_ID2 = "GetBalanceWithJsonp";
  public static final String GET_BALANCE_CONTRACT_ID3 = "GetBalanceWithJackson";
  public static final String GET_BALANCE_CONTRACT_ID4 = "GetBalanceWithString";
  public static final String HOLDER_CHECKER_CONTRACT_ID = "HolderChecker";
  public static final String NAMESPACE_AWARE_CREATE_ID = "NamespaceAwareCreate";
  public static final String NAMESPACE_AWARE_PAYMENT_ID = "NamespaceAwarePayment";
  public static final String NAMESPACE_AWARE_GET_BALANCE_ID = "NamespaceAwareGetBalance";
  public static final String NAMESPACE_AWARE_GET_HISTORY_ID = "NamespaceAwareGetHistory";

  // Function IDs
  public static final String CREATE_FUNCTION_ID1 = "CreateFunction";
  public static final String CREATE_FUNCTION_ID2 = "CreateFunctionWithJsonp";
  public static final String CREATE_FUNCTION_ID3 = "CreateFunctionWithJackson";
  public static final String CREATE_FUNCTION_ID4 = "CreateFunctionWithString";

  private Constants() {}
}
