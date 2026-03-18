package com.scalar.dl.ledger.database;

import com.scalar.dl.ledger.contract.ContractEntry;
import java.util.List;

public interface ContractRegistry {

  void bind(String namespace, ContractEntry entry);

  void unbind(String namespace, ContractEntry.Key key);

  ContractEntry lookup(String namespace, ContractEntry.Key key);

  List<ContractEntry> scan(String namespace, String certId);

  List<ContractEntry> scan(String namespace, String certId, int certVersion);
}
