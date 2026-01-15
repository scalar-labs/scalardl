package com.scalar.dl.ledger.database;

import com.scalar.dl.ledger.contract.ContractEntry;
import java.util.List;

public interface ContractRegistry {

  void bind(ContractEntry entry);

  void unbind(ContractEntry.Key key);

  ContractEntry lookup(ContractEntry.Key key);

  List<ContractEntry> scan(String certId);

  List<ContractEntry> scan(String certId, int certVersion);
}
