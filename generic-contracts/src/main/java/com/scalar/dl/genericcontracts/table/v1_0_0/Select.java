package com.scalar.dl.genericcontracts.table.v1_0_0;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Ledger;
import javax.annotation.Nullable;

public class Select extends JacksonBasedContract {

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode arguments, @Nullable JsonNode properties) {

    // Check the arguments
    if ((arguments.size() != 2 && arguments.size() != 3)
        || !arguments.has(Constants.QUERY_TABLE)
        || !arguments.get(Constants.QUERY_TABLE).isTextual()
        || !arguments.has(Constants.QUERY_CONDITIONS)
        || !arguments.get(Constants.QUERY_CONDITIONS).isArray()) {
      throw new ContractContextException(Constants.INVALID_QUERY_FORMAT);
    }

    // Check the format of each condition
    JsonNode conditions = arguments.get(Constants.QUERY_CONDITIONS);
    validateConditions(conditions);

    // Check projections
    JsonNode projections = getObjectMapper().createArrayNode();
    if (arguments.has(Constants.QUERY_PROJECTIONS)) {
      projections = arguments.get(Constants.QUERY_PROJECTIONS);
      validateProjections(projections);
    }

    // Scan and project records
    return project(invokeSubContract(Constants.CONTRACT_SCAN, ledger, arguments), projections);
  }

  private JsonNode project(JsonNode records, JsonNode projections) {
    if (projections.isEmpty()) {
      return records;
    } else {
      ArrayNode projectedResults = getObjectMapper().createArrayNode();
      for (JsonNode record : records) {
        projectedResults.add(projectRecord(record, projections));
      }
      return projectedResults;
    }
  }

  private JsonNode projectRecord(JsonNode record, JsonNode projections) {
    ObjectNode newRecord = getObjectMapper().createObjectNode();
    for (JsonNode projection : projections) {
      String column = projection.asText();
      if (record.has(column)) {
        newRecord.set(column, record.get(column));
      }
    }
    return newRecord;
  }

  private void validateConditions(JsonNode conditions) {
    for (JsonNode condition : conditions) {
      if (!condition.isObject()
          || !condition.has(Constants.CONDITION_COLUMN)
          || !condition.get(Constants.CONDITION_COLUMN).isTextual()
          || !condition.has(Constants.CONDITION_OPERATOR)
          || !condition.get(Constants.CONDITION_OPERATOR).isTextual()) {
        throw new ContractContextException(Constants.INVALID_CONDITION_FORMAT + condition);
      }

      String operator = condition.get(Constants.CONDITION_OPERATOR).asText();
      if (!isSupportedOperator(operator)) {
        throw new ContractContextException(Constants.INVALID_OPERATOR + condition);
      }

      if (operator.equalsIgnoreCase(Constants.OPERATOR_IS_NULL)
          || operator.equalsIgnoreCase(Constants.OPERATOR_IS_NOT_NULL)) {
        // For IS_NULL or IS_NOT_NULL
        if (condition.size() != 2) {
          throw new ContractContextException(Constants.INVALID_CONDITION_FORMAT + condition);
        }
      } else {
        // For other operators
        if (condition.size() != 3
            || !condition.has(Constants.CONDITION_VALUE)
            || condition.get(Constants.CONDITION_VALUE).isNull()) {
          throw new ContractContextException(Constants.INVALID_CONDITION_FORMAT + condition);
        }
        if (condition.get(Constants.CONDITION_VALUE).isBoolean()
            && (!operator.equalsIgnoreCase(Constants.OPERATOR_EQ)
                && !operator.equalsIgnoreCase(Constants.OPERATOR_NE))) {
          throw new ContractContextException(Constants.INVALID_OPERATOR + condition);
        }
      }
    }
  }

  private boolean isSupportedOperator(String type) {
    return type.equalsIgnoreCase(Constants.OPERATOR_EQ)
        || type.equalsIgnoreCase(Constants.OPERATOR_NE)
        || type.equalsIgnoreCase(Constants.OPERATOR_LT)
        || type.equalsIgnoreCase(Constants.OPERATOR_LTE)
        || type.equalsIgnoreCase(Constants.OPERATOR_GT)
        || type.equalsIgnoreCase(Constants.OPERATOR_GTE)
        || type.equalsIgnoreCase(Constants.OPERATOR_IS_NULL)
        || type.equalsIgnoreCase(Constants.OPERATOR_IS_NOT_NULL);
  }

  private void validateProjections(JsonNode projections) {
    if (!projections.isArray()) {
      throw new ContractContextException(Constants.INVALID_PROJECTIONS_FORMAT);
    }

    for (JsonNode projection : projections) {
      if (!projection.isTextual()) {
        throw new ContractContextException(Constants.INVALID_PROJECTIONS_FORMAT);
      }
    }
  }

  @VisibleForTesting
  JsonNode invokeSubContract(String contractId, Ledger<JsonNode> ledger, JsonNode arguments) {
    return invoke(contractId, ledger, arguments);
  }
}
