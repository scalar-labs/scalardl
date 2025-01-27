package com.scalar.dl.ledger.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.scalar.dl.ledger.crypto.CertificateEntry;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

public class ContractEntryKeyTest {
  private static final String CONTRACT_ID1 = "contract_id1";
  private static final String CONTRACT_ID2 = "contract_id2";
  private static final String ENTITY_ID1 = "entity_id1";
  private static final String ENTITY_ID2 = "entity_id2";
  private static final String VALUE1 = "value1";
  private static final String VALUE2 = "value2";
  private static final int VERSION1 = 1;
  private static final int VERSION2 = 2;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @SuppressWarnings("SelfEquals")
  @Test
  public void equals_SameKeyInstanceGiven_ShouldReturnTrue() {
    // Arrange
    ContractEntry.Key key1 =
        new ContractEntry.Key(CONTRACT_ID1, new CertificateEntry.Key(ENTITY_ID1, VERSION1));
    Map<ContractEntry.Key, String> map = new HashMap<>();

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
    ContractEntry.Key key1 =
        new ContractEntry.Key(CONTRACT_ID1, new CertificateEntry.Key(ENTITY_ID1, VERSION1));
    ContractEntry.Key key2 =
        new ContractEntry.Key(CONTRACT_ID1, new CertificateEntry.Key(ENTITY_ID1, VERSION1));
    Map<ContractEntry.Key, String> map = new HashMap<>();

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
  public void equals_DifferentContractIDGiven_ShouldReturnFalse() {
    // Arrange
    ContractEntry.Key key1 =
        new ContractEntry.Key(CONTRACT_ID1, new CertificateEntry.Key(ENTITY_ID1, VERSION1));
    ContractEntry.Key key2 =
        new ContractEntry.Key(CONTRACT_ID2, new CertificateEntry.Key(ENTITY_ID1, VERSION1));
    Map<ContractEntry.Key, String> map = new HashMap<>();

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
  public void equals_DifferentEntityIDGiven_ShouldReturnFalse() {
    // Arrange
    ContractEntry.Key key1 =
        new ContractEntry.Key(CONTRACT_ID1, new CertificateEntry.Key(ENTITY_ID1, VERSION1));
    ContractEntry.Key key2 =
        new ContractEntry.Key(CONTRACT_ID1, new CertificateEntry.Key(ENTITY_ID2, VERSION1));
    Map<ContractEntry.Key, String> map = new HashMap<>();

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
    ContractEntry.Key key1 =
        new ContractEntry.Key(CONTRACT_ID1, new CertificateEntry.Key(ENTITY_ID1, VERSION1));
    ContractEntry.Key key2 =
        new ContractEntry.Key(CONTRACT_ID1, new CertificateEntry.Key(ENTITY_ID1, VERSION2));
    Map<ContractEntry.Key, String> map = new HashMap<>();

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
