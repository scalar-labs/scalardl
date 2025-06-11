package com.scalar.dl.tablestore.client.partiql.statement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.dl.ledger.util.JacksonSerDe;

public interface ContractStatement {

  JacksonSerDe jacksonSerDe = new JacksonSerDe(new ObjectMapper());

  String getContractId();

  String getArguments();
}
