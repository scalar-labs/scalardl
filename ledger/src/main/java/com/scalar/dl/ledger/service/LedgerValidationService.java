package com.scalar.dl.ledger.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.contract.ContractManager;
import com.scalar.dl.ledger.crypto.ClientKeyValidator;
import com.scalar.dl.ledger.crypto.SignatureValidator;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.Transaction;
import com.scalar.dl.ledger.database.TransactionManager;
import com.scalar.dl.ledger.database.scalardb.AssetProofComposer;
import com.scalar.dl.ledger.exception.LedgerException;
import com.scalar.dl.ledger.exception.ValidationException;
import com.scalar.dl.ledger.model.AssetProofRetrievalRequest;
import com.scalar.dl.ledger.model.LedgerValidationRequest;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.proof.AssetProof;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.validation.LedgerValidator;
import java.util.List;
import java.util.Optional;
import javax.annotation.concurrent.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
public class LedgerValidationService extends ValidationService {
  private static final Logger LOGGER = LoggerFactory.getLogger(LedgerValidationService.class);
  private final LedgerConfig config;
  private final TransactionManager transactionManager;
  private final List<LedgerValidator> givenValidators;

  @Inject
  public LedgerValidationService(
      LedgerConfig config,
      TransactionManager transactionManager,
      ClientKeyValidator clientKeyValidator,
      ContractManager contractManager,
      AssetProofComposer proofComposer) {
    super(clientKeyValidator, contractManager, transactionManager, proofComposer);
    this.config = config;
    this.transactionManager = transactionManager;
    this.givenValidators = null;
  }

  @VisibleForTesting
  LedgerValidationService(
      LedgerConfig config,
      TransactionManager transactionManager,
      ClientKeyValidator clientKeyValidator,
      ContractManager contractManager,
      AssetProofComposer proofComposer,
      List<LedgerValidator> validators) {
    super(clientKeyValidator, contractManager, transactionManager, proofComposer);
    this.config = config;
    this.transactionManager = transactionManager;
    this.givenValidators = validators;
  }

  @Override
  public LedgerValidationResult validate(LedgerValidationRequest request) {
    SignatureValidator validator =
        clientKeyValidator.getValidator(request.getEntityId(), request.getKeyVersion());
    request.validateWith(validator);

    if (config.isAuditorEnabled()) {
      // With Auditor, we only provide linearizable validation by executing the contract.
      throw new LedgerException(
          "Configuration mismatch detected. Check the Auditor setting in the client or Ledger",
          StatusCode.INVALID_REQUEST);
    } else {
      return validate(request.getAssetId(), request.getStartAge(), request.getEndAge());
    }
  }

  public AssetProof retrieve(AssetProofRetrievalRequest request) {
    SignatureValidator validator =
        clientKeyValidator.getValidator(request.getEntityId(), request.getKeyVersion());
    request.validateWith(validator);

    InternalAsset asset = retrieve(request.getAssetId(), request.getAge());
    return proofComposer.create(asset);
  }

  @VisibleForTesting
  LedgerValidationResult validate(String assetId, int startAge, int endAge) {
    List<LedgerValidator> validators = givenValidators;
    if (validators == null) {
      validators = validateInit();
    }

    InternalAsset last = null;
    for (InternalAsset asset : getAssets(assetId, startAge, endAge)) {
      StatusCode code;
      try {
        code = validateEach(validators, asset);
      } catch (ValidationException e) {
        if (e.getCode() != null) {
          code = e.getCode();
        } else {
          throw e;
        }
      }
      if (code != StatusCode.OK) {
        LOGGER.error("Validation failed with " + code);
        return new LedgerValidationResult(code, proofComposer.create(asset), null);
      }
      last = asset;
    }

    if (last == null) {
      return new LedgerValidationResult(StatusCode.ASSET_NOT_FOUND, null, null);
    }
    return new LedgerValidationResult(StatusCode.OK, proofComposer.create(last), null);
  }

  @VisibleForTesting
  InternalAsset retrieve(String assetId, int age) {
    if (age >= 0 && age < Integer.MAX_VALUE) {
      List<InternalAsset> assets = getAssets(assetId, age, age);
      if (assets.size() > 0) {
        // For now, it doesn't do per-ledger validation
        return assets.get(0);
      }
    } else {
      Optional<InternalAsset> asset = getAsset(assetId);
      if (asset.isPresent()) {
        return asset.get();
      }
    }
    throw new ValidationException("The specified asset is not found.", StatusCode.ASSET_NOT_FOUND);
  }

  private Optional<InternalAsset> getAsset(String assetId) {
    Transaction transaction = transactionManager.startWith();

    try {
      Optional<InternalAsset> asset = transaction.getLedger().get(assetId);
      transaction.commit();
      return asset;
    } catch (Exception e) {
      LOGGER.error("Retrieving the specified asset for validation failed.", e);
      transaction.abort();
      throw e;
    }
  }

  private List<InternalAsset> getAssets(String assetId, int startAge, int endAge) {
    Transaction transaction = transactionManager.startWith();

    AssetFilter filter =
        new AssetFilter(assetId)
            .withStartAge(startAge, true)
            .withEndAge(endAge, true)
            .withAgeOrder(AssetFilter.AgeOrder.ASC);

    try {
      List<InternalAsset> assets = transaction.getLedger().scan(filter);
      transaction.commit();
      return assets;
    } catch (Exception e) {
      LOGGER.error("Retrieving the specified assets for validation failed.", e);
      transaction.abort();
      throw e;
    }
  }
}
