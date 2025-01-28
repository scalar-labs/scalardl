package com.scalar.dl.ledger.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CertificateEntryKeyTest {
  private static final String ENTITY_ID1 = "entity_id1";
  private static final String ENTITY_ID2 = "entity_id2";
  private static final int VERSION1 = 1;
  private static final int VERSION2 = 2;
  private static final String VALUE1 = "value1";
  private static final String VALUE2 = "value2";
  private AutoCloseable closeable;

  @BeforeEach
  public void setUp() {
    closeable = openMocks(this);
  }

  @AfterEach
  public void tearDown() throws Exception {
    closeable.close();
  }

  @SuppressWarnings("SelfEquals")
  @Test
  public void equals_SameKeyInstanceGiven_ShouldReturnTrue() {
    // Arrange
    CertificateEntry.Key key1 = new CertificateEntry.Key(ENTITY_ID1, VERSION1);
    Map<CertificateEntry.Key, String> map = new HashMap<>();

    // Act
    boolean actual = key1.equals(key1);
    map.put(key1, VALUE1);

    // Assert
    assertThat(actual).isTrue();
    assertThat(map).containsOnly(entry(key1, VALUE1));
  }

  @Test
  public void equals_SameKeyGiven_ShouldReturnTrue() {
    // Arrange
    CertificateEntry.Key key1 = new CertificateEntry.Key(ENTITY_ID1, VERSION1);
    CertificateEntry.Key key2 = new CertificateEntry.Key(ENTITY_ID1, VERSION1);
    Map<CertificateEntry.Key, String> map = new HashMap<>();

    // Act
    boolean actual = key1.equals(key2);
    int hash1 = key1.hashCode();
    int hash2 = key2.hashCode();
    map.put(key1, VALUE1);
    map.put(key2, VALUE2);

    // Assert
    assertThat(actual).isTrue();
    assertThat(hash1).isEqualTo(hash2);
    assertThat(map.get(key1).equals(map.get(key2))).isTrue();
    assertThat(map).containsOnly(entry(key2, VALUE2));
  }

  @Test
  public void equals_DifferentEntityIDGiven_ShouldReturnFalse() {
    // Arrange
    CertificateEntry.Key key1 = new CertificateEntry.Key(ENTITY_ID1, VERSION1);
    CertificateEntry.Key key2 = new CertificateEntry.Key(ENTITY_ID2, VERSION1);
    Map<CertificateEntry.Key, String> map = new HashMap<>();

    // Act
    boolean actual = key1.equals(key2);
    int hash1 = key1.hashCode();
    int hash2 = key2.hashCode();
    map.put(key1, VALUE1);
    map.put(key2, VALUE2);

    // Assert
    assertThat(actual).isFalse();
    assertThat(hash1).isNotEqualTo(hash2);
    assertThat(map.get(key1).equals(map.get(key2))).isFalse();
    assertThat(map).containsOnly(entry(key1, VALUE1), entry(key2, VALUE2));
  }

  @Test
  public void equals_DifferentVersionGiven_ShouldReturnFalse() {
    // Arrange
    CertificateEntry.Key key1 = new CertificateEntry.Key(ENTITY_ID1, VERSION1);
    CertificateEntry.Key key2 = new CertificateEntry.Key(ENTITY_ID1, VERSION2);
    Map<CertificateEntry.Key, String> map = new HashMap<>();

    // Act
    boolean actual = key1.equals(key2);
    int hash1 = key1.hashCode();
    int hash2 = key2.hashCode();
    map.put(key1, VALUE1);
    map.put(key2, VALUE2);

    // Assert
    assertThat(actual).isFalse();
    assertThat(hash1).isNotEqualTo(hash2);
    assertThat(map.get(key1).equals(map.get(key2))).isFalse();
    assertThat(map).containsOnly(entry(key1, VALUE1), entry(key2, VALUE2));
  }
}
