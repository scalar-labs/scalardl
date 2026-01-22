package com.scalar.dl.ledger.database;

import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.exception.LedgerException;
import com.scalar.dl.ledger.proof.AssetProof;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class NamespaceRestrictedAssetLedger implements TamperEvidentAssetLedger {
  private final TamperEvidentAssetLedger delegate;
  private final String contextNamespace;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public NamespaceRestrictedAssetLedger(
      TamperEvidentAssetLedger delegate, String contextNamespace) {
    this.delegate = delegate;
    this.contextNamespace = contextNamespace;
  }

  private void validateNamespaceAccess(String namespace) {
    if (!contextNamespace.equals(namespace)) {
      throw new LedgerException(
          CommonError.ACCESSING_NAMESPACE_NOT_ALLOWED, namespace, contextNamespace);
    }
  }

  @Override
  public Optional<InternalAsset> get(String assetId) {
    return delegate.get(assetId);
  }

  @Override
  public Optional<InternalAsset> get(String namespace, String assetId) {
    validateNamespaceAccess(namespace);
    return delegate.get(namespace, assetId);
  }

  @Override
  public List<InternalAsset> scan(AssetFilter filter) {
    filter.getNamespace().ifPresent(this::validateNamespaceAccess);
    return delegate.scan(filter);
  }

  @Override
  public void put(String assetId, String data) {
    delegate.put(assetId, data);
  }

  @Override
  public void put(String namespace, String assetId, String data) {
    validateNamespaceAccess(namespace);
    delegate.put(namespace, assetId, data);
  }

  @Override
  public List<AssetProof> commit() {
    return delegate.commit();
  }

  @Override
  public void abort() {
    delegate.abort();
  }
}
