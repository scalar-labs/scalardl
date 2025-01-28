package com.scalar.dl.ledger.contract;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.concurrent.Immutable;

@Immutable
public class ContractScanner {
  private final ContractManager manager;

  public ContractScanner(ContractManager manager) {
    this.manager = manager;
  }

  public List<ContractEntry> scan(String entityId, int keyVersion) {
    return scan(entityId, keyVersion, Optional.empty());
  }

  public List<ContractEntry> scan(String entityId, int keyVersion, Optional<String> contractId) {
    List<ContractEntry> list = manager.scan(entityId, keyVersion);
    if (!contractId.isPresent() || contractId.get().isEmpty()) {
      return list;
    }

    String targetId = contractId.get();
    return list.stream().filter(e -> e.getId().equals(targetId)).collect(Collectors.toList());
  }
}
