package com.scalar.dl.ledger.test;

/** Common test constants shared across all Ledger test classes. */
public final class TestConstants {
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

  private TestConstants() {}
}
