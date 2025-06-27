package com.scalar.dl.tablestore.client.partiql.statement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.dl.ledger.util.JacksonSerDe;

public abstract class AbstractJacksonBasedContractStatement implements ContractStatement {

  protected static final JacksonSerDe jacksonSerDe = new JacksonSerDe(new ObjectMapper());
}
