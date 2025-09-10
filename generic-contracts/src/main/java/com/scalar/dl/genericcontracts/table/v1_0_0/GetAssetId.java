package com.scalar.dl.genericcontracts.table.v1_0_0;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.genericcontracts.table.Constants;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Ledger;
import javax.annotation.Nullable;

public class GetAssetId extends JacksonBasedContract {

  /**
   * Returns the asset ID based on the specified asset type (prefix) and values. Specify the values
   * as an array of {@code JsonNode}. For the table asset, only include a {@code TextNode} of the
   * table name in the array. For the record and index asset, include a {@code TextNode} of the
   * table name, a {@code TextNode} of the primary or index key column name, and a {@code JsonNode}
   * of the key column value.
   *
   * @param ledger ledger
   * @param arguments contract argument that includes the asset ID prefix and values
   * @param properties pre-registered contract properties
   * @return {@code TextNode}
   */
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode arguments, @Nullable JsonNode properties) {
    String prefix = arguments.get(Constants.ASSET_ID_PREFIX).asText();
    ArrayNode values = (ArrayNode) arguments.get(Constants.ASSET_ID_VALUES);
    return TextNode.valueOf(getAssetId(prefix, values));
  }

  private String getAssetId(String prefix, ArrayNode values) {
    switch (prefix) {
      case Constants.PREFIX_TABLE:
        return getAssetIdForTable(values.get(0).asText());
      case Constants.PREFIX_RECORD:
        return getAssetIdForRecord(values.get(0).asText(), values.get(1).asText(), values.get(2));
      case Constants.PREFIX_INDEX:
        if (values.get(2).isNull()) {
          return getAssetIdForNullIndex(values.get(0).asText(), values.get(1).asText());
        } else {
          return getAssetIdForIndex(values.get(0).asText(), values.get(1).asText(), values.get(2));
        }
      default:
        throw new ContractContextException(Constants.ILLEGAL_ARGUMENT);
    }
  }

  @VisibleForTesting
  static String getAssetIdForTable(String tableName) {
    return Constants.PREFIX_TABLE + tableName;
  }

  @VisibleForTesting
  static String getAssetIdForRecord(String tableName, String primaryKey, JsonNode value) {
    return Constants.PREFIX_RECORD
        + tableName
        + Constants.ASSET_ID_SEPARATOR
        + primaryKey
        + Constants.ASSET_ID_SEPARATOR
        + toStringFrom(value);
  }

  @VisibleForTesting
  static String getAssetIdForIndex(String tableName, String indexKey, JsonNode value) {
    return Constants.PREFIX_INDEX
        + tableName
        + Constants.ASSET_ID_SEPARATOR
        + indexKey
        + Constants.ASSET_ID_SEPARATOR
        + toStringFrom(value);
  }

  @VisibleForTesting
  static String getAssetIdForNullIndex(String tableName, String indexKey) {
    return Constants.PREFIX_INDEX + tableName + Constants.ASSET_ID_SEPARATOR + indexKey;
  }

  private static String toStringFrom(JsonNode value) {
    if (value.canConvertToExactIntegral()) {
      return value.bigIntegerValue().toString();
    } else if (value.isNumber()) {
      return value.decimalValue().toString();
    } else {
      return value.asText();
    }
  }
}
