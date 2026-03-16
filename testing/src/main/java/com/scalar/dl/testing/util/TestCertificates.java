package com.scalar.dl.testing.util;

/** Test certificates and keys for integration testing. */
public final class TestCertificates {

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

  // Ledger server's private key in PEM format (for proof signing)
  public static final String LEDGER_PRIVATE_KEY =
      "-----BEGIN EC PRIVATE KEY-----\n"
          + "MHcCAQEEIMcku82ns3JtMIQWk72bmcqvuT5kAcvDUumWvD7N1EvVoAoGCCqGSM49\n"
          + "AwEHoUQDQgAEncyLrxHGKYj9yDBPDDFPeb9zIBD1G6zgcZ1M4mqJLEOnLuySXR9n\n"
          + "gISxFtC9O4iOutbshteNJAbmaBa4801uow==\n"
          + "-----END EC PRIVATE KEY-----";

  // Ledger server's certificate in PEM format
  public static final String LEDGER_CERTIFICATE =
      "-----BEGIN CERTIFICATE-----\n"
          + "MIICDjCCAbSgAwIBAgIUOPXZ/Da6rgRiI93Bshgo4pRv9UQwCgYIKoZIzj0EAwIw\n"
          + "cDELMAkGA1UEBhMCSlAxDjAMBgNVBAgMBVRva3lvMRQwEgYDVQQHDAtTaGluanVr\n"
          + "dS1rdTEVMBMGA1UECgwMU2NhbGFyLCBJbmMuMSQwIgYDVQQDDBtsZWRnZXIudGVz\n"
          + "dC5zY2FsYXItbGFicy5jb20wIBcNMjQwNDA4MDczOTAzWhgPMjEyNDAzMTUwNzM5\n"
          + "MDNaMHAxCzAJBgNVBAYTAkpQMQ4wDAYDVQQIDAVUb2t5bzEUMBIGA1UEBwwLU2hp\n"
          + "bmp1a3Uta3UxFTATBgNVBAoMDFNjYWxhciwgSW5jLjEkMCIGA1UEAwwbbGVkZ2Vy\n"
          + "LnRlc3Quc2NhbGFyLWxhYnMuY29tMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE\n"
          + "ncyLrxHGKYj9yDBPDDFPeb9zIBD1G6zgcZ1M4mqJLEOnLuySXR9ngISxFtC9O4iO\n"
          + "utbshteNJAbmaBa4801uo6MqMCgwJgYDVR0RBB8wHYIbbGVkZ2VyLnRlc3Quc2Nh\n"
          + "bGFyLWxhYnMuY29tMAoGCCqGSM49BAMCA0gAMEUCIQC83rAPTBjyZJQ8VpGgAoCi\n"
          + "apQT1px1yNYWnSoNAKxu2AIgUKwW+bTF5nH/cA+O/SU8EEgonWRg1JWmH204L6em\n"
          + "FHM=\n"
          + "-----END CERTIFICATE-----";

  // Ledger server's holder ID
  public static final String LEDGER_CERT_HOLDER_ID = "ledger";

  // HMAC test keys
  public static final String SECRET_KEY_A = "secret_key_A";
  public static final String SECRET_KEY_B = "secret_key_B";
  public static final String CIPHER_KEY = "cipher_key_0123456789abcdef";

  // Default test entity IDs
  public static final String ENTITY_ID_A = "test_entity_a";
  public static final String ENTITY_ID_B = "test_entity_b";
  public static final int KEY_VERSION = 1;

  private TestCertificates() {}
}
