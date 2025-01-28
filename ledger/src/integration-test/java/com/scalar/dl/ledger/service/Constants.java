package com.scalar.dl.ledger.service;

public class Constants {
  public static final String ASSET_ATTRIBUTE_NAME = "asset_id";
  public static final String ASSETS_ATTRIBUTE_NAME = "asset_ids";
  public static final String AMOUNT_ATTRIBUTE_NAME = "amount";
  public static final String BALANCE_ATTRIBUTE_NAME = "balance";
  public static final String CONTRACT_ID_ATTRIBUTE_NAME = "contract_id";
  public static final String EXECUTE_NESTED_ATTRIBUTE_NAME = "execute_nested";
  public static final String NAMESPACE_ATTRIBUTE_NAME = "namespace";
  public static final String CREATE_CONTRACT_ID1 = "Create";
  public static final String CREATE_CONTRACT_ID2 = "CreateWithJsonp";
  public static final String CREATE_CONTRACT_ID3 = "CreateWithJackson";
  public static final String CREATE_CONTRACT_ID4 = "CreateWithString";
  public static final String PAYMENT_CONTRACT_ID1 = "Payment";
  public static final String PAYMENT_CONTRACT_ID2 = "PaymentWithJsonp";
  public static final String PAYMENT_CONTRACT_ID3 = "PaymentWithJackson";
  public static final String PAYMENT_CONTRACT_ID4 = "PaymentWithString";
  public static final String CREATE_FUNCTION_ID1 = "CreateFunction";
  public static final String CREATE_FUNCTION_ID2 = "CreateFunctionWithJsonp";
  public static final String CREATE_FUNCTION_ID3 = "CreateFunctionWithJackson";
  public static final String CREATE_FUNCTION_ID4 = "CreateFunctionWithString";
  public static final String HOLDER_CHECKER_CONTRACT_ID = "HolderChecker";
  public static final String GET_BALANCE_CONTRACT_ID1 = "GetBalance";
  public static final String GET_BALANCE_CONTRACT_ID2 = "GetBalanceWithJsonp";
  public static final String GET_BALANCE_CONTRACT_ID3 = "GetBalanceWithJackson";
  public static final String GET_BALANCE_CONTRACT_ID4 = "GetBalanceWithString";

  public static final String FUNCTION_NAMESPACE = "test";
  public static final String FUNCTION_TABLE = "function_test";
  public static final String ID_ATTRIBUTE_NAME = "id";
  public static final String SOME_ID = "some_id";
  public static final int SOME_BALANCE = 10;

  public static final String SOME_ASSET_ID_1 = "A";
  public static final String SOME_ASSET_ID_2 = "B";
  public static final String SOME_NONCE = "nonce";
  public static final int SOME_AMOUNT_1 = 1000;
  public static final int SOME_AMOUNT_2 = 100;
  public static final int SOME_AMOUNT_3 = 200;

  public static final String ENTITY_ID_A = "entity_a";
  public static final String ENTITY_ID_B = "entity_b";
  public static final String ENTITY_ID_C = "entity_c";
  public static final String ENTITY_ID_D = "entity_d";
  public static final String AUDITOR_ENTITY_ID = "auditor_entity";
  public static final int KEY_VERSION = 1;

  public static final String ASSET_ID_COLUMN_NAME = "id";
  public static final String ASSET_AGE_COLUMN_NAME = "age";
  public static final String ASSET_OUTPUT_COLUMN_NAME = "output";

  public static final String PRIVATE_KEY_A =
      "-----BEGIN EC PRIVATE KEY-----\n"
          + "MHcCAQEEIF4SjQxTArRcZaROSFjlBP2rR8fAKtL8y+kmGiSlM5hEoAoGCCqGSM49\n"
          + "AwEHoUQDQgAEY0i/iAFxIBS3etbjoSC1/aUKQV66+wiawL4bZqklu86ObIc7wrif\n"
          + "HExPmVhKFSklOyZqGoOiVZA0zf0LZeFaPA==\n"
          + "-----END EC PRIVATE KEY-----";
  public static final String PRIVATE_KEY_B =
      "-----BEGIN EC PRIVATE KEY-----\n"
          + "MHcCAQEEIAHSsi6IZaB4aO7qbvkf4uv4HIAHNdMH2l6YDGyyYzY+oAoGCCqGSM49\n"
          + "AwEHoUQDQgAEDhDSlG3KmPN2zK16AFB68vSa4M5MLuEtNSL7c1/ul8b6HKrq9Ivo\n"
          + "xmxDUidA3pmIotkcjPtMSAxoDC98NjV2Aw==\n"
          + "-----END EC PRIVATE KEY-----";
  public static final String CERTIFICATE_A =
      "-----BEGIN CERTIFICATE-----\n"
          + "MIICQTCCAeagAwIBAgIUEKARigcZQ3sLEXdlEtjYissVx0cwCgYIKoZIzj0EAwIw\n"
          + "QTELMAkGA1UEBhMCSlAxDjAMBgNVBAgTBVRva3lvMQ4wDAYDVQQHEwVUb2t5bzES\n"
          + "MBAGA1UEChMJU2FtcGxlIENBMB4XDTE4MDYyMTAyMTUwMFoXDTE5MDYyMTAyMTUw\n"
          + "MFowRTELMAkGA1UEBhMCSlAxDjAMBgNVBAgTBVRva3lvMQ4wDAYDVQQHEwVUb2t5\n"
          + "bzEWMBQGA1UEChMNU2FtcGxlIENsaWVudDBZMBMGByqGSM49AgEGCCqGSM49AwEH\n"
          + "A0IABGNIv4gBcSAUt3rW46Egtf2lCkFeuvsImsC+G2apJbvOjmyHO8K4nxxMT5lY\n"
          + "ShUpJTsmahqDolWQNM39C2XhWjyjgbcwgbQwDgYDVR0PAQH/BAQDAgWgMB0GA1Ud\n"
          + "JQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjAMBgNVHRMBAf8EAjAAMB0GA1UdDgQW\n"
          + "BBTpBQl/JxB7yr77uMVT9mMicPeVJTAfBgNVHSMEGDAWgBQrJo3N3/0j3oPS6F6m\n"
          + "wunHe8xLpzA1BgNVHREELjAsghJjbGllbnQuZXhhbXBsZS5jb22CFnd3dy5jbGll\n"
          + "bnQuZXhhbXBsZS5jb20wCgYIKoZIzj0EAwIDSQAwRgIhAJPtXSzuncDJXnM+7us8\n"
          + "46MEVjGHJy70bRY1My23RkxbAiEA5oFgTKMvls8e4UpnmUgFNP+FH8a5bF4tUPaV\n"
          + "BQiBbgk=\n"
          + "-----END CERTIFICATE-----";
  public static final String CERTIFICATE_B =
      "-----BEGIN CERTIFICATE-----\n"
          + "MIICjDCCAjKgAwIBAgIUTnLDk2Y+84DRD8bbQuZE1xlxidkwCgYIKoZIzj0EAwIw\n"
          + "bzELMAkGA1UEBhMCSlAxDjAMBgNVBAgTBVRva3lvMQ4wDAYDVQQHEwVUb2t5bzEf\n"
          + "MB0GA1UEChMWU2FtcGxlIEludGVybWVkaWF0ZSBDQTEfMB0GA1UEAxMWU2FtcGxl\n"
          + "IEludGVybWVkaWF0ZSBDQTAeFw0xODA4MDkwNzAwMDBaFw0yMTA4MDgwNzAwMDBa\n"
          + "MEUxCzAJBgNVBAYTAkFVMRMwEQYDVQQIEwpTb21lLVN0YXRlMSEwHwYDVQQKExhJ\n"
          + "bnRlcm5ldCBXaWRnaXRzIFB0eSBMdGQwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNC\n"
          + "AAQOENKUbcqY83bMrXoAUHry9Jrgzkwu4S01IvtzX+6Xxvocqur0i+jGbENSJ0De\n"
          + "mYii2RyM+0xIDGgML3w2NXYDo4HVMIHSMA4GA1UdDwEB/wQEAwIFoDATBgNVHSUE\n"
          + "DDAKBggrBgEFBQcDAjAMBgNVHRMBAf8EAjAAMB0GA1UdDgQWBBSsklJebvmvOepv\n"
          + "QhvsCVFO4h+z+jAfBgNVHSMEGDAWgBT0HscZ7eRWv8QlQgfbtaT7BDNQEzAxBggr\n"
          + "BgEFBQcBAQQlMCMwIQYIKwYBBQUHMAGGFWh0dHA6Ly9sb2NhbGhvc3Q6ODg4OTAq\n"
          + "BgNVHR8EIzAhMB+gHaAbhhlodHRwOi8vbG9jYWxob3N0Ojg4ODgvY3JsMAoGCCqG\n"
          + "SM49BAMCA0gAMEUCIAJavUnxqZm/a/szytCNdmESZdL++H71+YHHuTkxud8DAiEA\n"
          + "6GUKwnt7oDqLgoavBNhBVmbmxMJjo+D3YEwTOJ/X4bs=\n"
          + "-----END CERTIFICATE-----";
  public static final String SECRET_KEY_A = "secret_key_A";
  public static final String SECRET_KEY_B = "secret_key_B";
  public static final String SOME_CIPHER_KEY = "cipher_key";
}
