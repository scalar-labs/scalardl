package com.scalar.dl.ledger.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.dl.ledger.contract.ContractEntry;
import com.scalar.dl.ledger.util.JacksonSerDe;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TypeConverterTest {
  private static final String SOME_CONTRACT_ID = "contract_id";
  private static final String SOME_CONTRACT_NAME = "com.example.Contract";
  private static final String SOME_ENTITY_ID = "entity_id";
  private static final int SOME_KEY_VERSION = 1;
  private static final byte[] SOME_BYTE_CODE = "bytecode".getBytes(StandardCharsets.UTF_8);
  private static final String SOME_PROPERTIES = "{\"key\":\"value\"}";
  private static final long SOME_REGISTERED_AT = 1234567890L;
  private static final byte[] SOME_SIGNATURE = "signature".getBytes(StandardCharsets.UTF_8);

  private JacksonSerDe jacksonSerDe;

  @BeforeEach
  public void setUp() {
    jacksonSerDe = new JacksonSerDe(new ObjectMapper());
  }

  @Test
  public void convert_SingleContractEntryGiven_ShouldReturnJsonWithContractInfo() {
    // Arrange
    ContractEntry entry =
        new ContractEntry(
            SOME_CONTRACT_ID,
            SOME_CONTRACT_NAME,
            SOME_ENTITY_ID,
            SOME_KEY_VERSION,
            SOME_BYTE_CODE,
            SOME_PROPERTIES,
            SOME_REGISTERED_AT,
            SOME_SIGNATURE);
    List<ContractEntry> entries = Collections.singletonList(entry);

    // Act
    String result = TypeConverter.convert(entries);

    // Assert
    JsonNode jsonNode = jacksonSerDe.deserialize(result);
    assertThat(jsonNode.has(SOME_CONTRACT_ID)).isTrue();
    JsonNode contractNode = jsonNode.get(SOME_CONTRACT_ID);
    assertThat(contractNode.get("contract_name").asText()).isEqualTo(SOME_CONTRACT_NAME);
    assertThat(contractNode.get("entity_id").asText()).isEqualTo(SOME_ENTITY_ID);
    assertThat(contractNode.get("cert_version").asInt()).isEqualTo(SOME_KEY_VERSION);
    assertThat(contractNode.get("contract_properties").asText()).isEqualTo(SOME_PROPERTIES);
    assertThat(contractNode.get("registered_at").asLong()).isEqualTo(SOME_REGISTERED_AT);
    assertThat(contractNode.get("signature").asText())
        .isEqualTo(Base64.getEncoder().encodeToString(SOME_SIGNATURE));
  }

  @Test
  public void convert_MultipleContractEntriesGiven_ShouldReturnJsonWithAllContracts() {
    // Arrange
    ContractEntry entry1 =
        new ContractEntry(
            "contract1",
            "com.example.Contract1",
            SOME_ENTITY_ID,
            SOME_KEY_VERSION,
            SOME_BYTE_CODE,
            null,
            SOME_REGISTERED_AT,
            SOME_SIGNATURE);
    ContractEntry entry2 =
        new ContractEntry(
            "contract2",
            "com.example.Contract2",
            "entity2",
            2,
            SOME_BYTE_CODE,
            SOME_PROPERTIES,
            SOME_REGISTERED_AT + 100,
            SOME_SIGNATURE);
    List<ContractEntry> entries = Arrays.asList(entry1, entry2);

    // Act
    String result = TypeConverter.convert(entries);

    // Assert
    JsonNode jsonNode = jacksonSerDe.deserialize(result);
    assertThat(jsonNode.has("contract1")).isTrue();
    assertThat(jsonNode.has("contract2")).isTrue();

    JsonNode contract1Node = jsonNode.get("contract1");
    assertThat(contract1Node.get("contract_name").asText()).isEqualTo("com.example.Contract1");
    assertThat(contract1Node.get("entity_id").asText()).isEqualTo(SOME_ENTITY_ID);
    assertThat(contract1Node.get("cert_version").asInt()).isEqualTo(SOME_KEY_VERSION);
    assertThat(contract1Node.get("contract_properties").asText()).isEmpty();

    JsonNode contract2Node = jsonNode.get("contract2");
    assertThat(contract2Node.get("contract_name").asText()).isEqualTo("com.example.Contract2");
    assertThat(contract2Node.get("entity_id").asText()).isEqualTo("entity2");
    assertThat(contract2Node.get("cert_version").asInt()).isEqualTo(2);
    assertThat(contract2Node.get("contract_properties").asText()).isEqualTo(SOME_PROPERTIES);
  }

  @Test
  public void convert_EmptyContractListGiven_ShouldReturnEmptyJsonObject() {
    // Arrange
    List<ContractEntry> entries = Collections.emptyList();

    // Act
    String result = TypeConverter.convert(entries);

    // Assert
    JsonNode jsonNode = jacksonSerDe.deserialize(result);
    assertThat(jsonNode.isObject()).isTrue();
    assertThat(jsonNode.size()).isEqualTo(0);
  }

  @Test
  public void convertNamespaces_SingleNamespaceGiven_ShouldReturnJsonWithNamespace() {
    // Arrange
    List<String> namespaces = Collections.singletonList("test_namespace");

    // Act
    String result = TypeConverter.convertNamespaces(namespaces);

    // Assert
    JsonNode jsonNode = jacksonSerDe.deserialize(result);
    assertThat(jsonNode.has("namespaces")).isTrue();
    assertThat(jsonNode.get("namespaces").isArray()).isTrue();
    assertThat(jsonNode.get("namespaces").size()).isEqualTo(1);
    assertThat(jsonNode.get("namespaces").get(0).asText()).isEqualTo("test_namespace");
  }

  @Test
  public void convertNamespaces_MultipleNamespacesGiven_ShouldReturnJsonWithAllNamespaces() {
    // Arrange
    List<String> namespaces = Arrays.asList("ns1", "ns2", "ns3");

    // Act
    String result = TypeConverter.convertNamespaces(namespaces);

    // Assert
    JsonNode jsonNode = jacksonSerDe.deserialize(result);
    assertThat(jsonNode.has("namespaces")).isTrue();
    assertThat(jsonNode.get("namespaces").isArray()).isTrue();
    assertThat(jsonNode.get("namespaces").size()).isEqualTo(3);
    assertThat(jsonNode.get("namespaces").get(0).asText()).isEqualTo("ns1");
    assertThat(jsonNode.get("namespaces").get(1).asText()).isEqualTo("ns2");
    assertThat(jsonNode.get("namespaces").get(2).asText()).isEqualTo("ns3");
  }

  @Test
  public void convertNamespaces_EmptyListGiven_ShouldReturnJsonWithEmptyArray() {
    // Arrange
    List<String> namespaces = Collections.emptyList();

    // Act
    String result = TypeConverter.convertNamespaces(namespaces);

    // Assert
    JsonNode jsonNode = jacksonSerDe.deserialize(result);
    assertThat(jsonNode.has("namespaces")).isTrue();
    assertThat(jsonNode.get("namespaces").isArray()).isTrue();
    assertThat(jsonNode.get("namespaces").size()).isEqualTo(0);
  }
}
