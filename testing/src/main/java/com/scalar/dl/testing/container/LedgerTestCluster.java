package com.scalar.dl.testing.container;

import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.testing.config.TransactionMode;
import com.scalar.dl.testing.util.TestCertificates;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;

/**
 * Test cluster for Ledger integration tests using TestContainers.
 *
 * <p>This cluster starts a MySQL container for storage and a Ledger container.
 */
public class LedgerTestCluster extends AbstractTestCluster {
  private static final Logger logger = LoggerFactory.getLogger(LedgerTestCluster.class);

  private static final String LEDGER_NETWORK_ALIAS = "ledger";

  private final String ledgerImage;
  private LedgerContainer ledger;

  /**
   * Creates a test cluster with default settings.
   *
   * @param authenticationMethod Authentication method
   */
  public LedgerTestCluster(AuthenticationMethod authenticationMethod) {
    this(authenticationMethod, TransactionMode.CONSENSUS_COMMIT);
  }

  /**
   * Creates a test cluster with specified transaction mode.
   *
   * @param authenticationMethod Authentication method
   * @param transactionMode Transaction mode
   */
  public LedgerTestCluster(
      AuthenticationMethod authenticationMethod, TransactionMode transactionMode) {
    this(authenticationMethod, transactionMode, LedgerContainer.DEFAULT_IMAGE);
  }

  /**
   * Creates a test cluster with specified Docker image.
   *
   * @param authenticationMethod Authentication method
   * @param transactionMode Transaction mode
   * @param ledgerImage Ledger Docker image
   */
  public LedgerTestCluster(
      AuthenticationMethod authenticationMethod,
      TransactionMode transactionMode,
      String ledgerImage) {
    super(authenticationMethod, transactionMode);
    this.ledgerImage = ledgerImage;
  }

  @Override
  @SuppressWarnings("resource")
  protected void startContainers() {
    ledger = new LedgerContainer(ledgerImage);
    ledger.withNetwork(getNetwork()).withNetworkAliases(LEDGER_NETWORK_ALIAS);
    ledger.withStorageConfig(getStorageConfig());
    ledger.withAuthenticationMethod(authenticationMethod);
    ledger.withProofEnabled();
    ledger.withProofPrivateKey(TestCertificates.LEDGER_PRIVATE_KEY);
    ledger.withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("ledger"));
    ledger.start();
    logger.info("Ledger container started on port {}", ledger.getPort());
  }

  @Override
  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP",
      justification = "Exposing internal container is intentional for test access")
  public LedgerContainer getLedger() {
    return ledger;
  }

  @Override
  public void close() {
    logger.info("Stopping LedgerTestCluster");

    if (ledger != null) {
      try {
        ledger.stop();
      } catch (Exception e) {
        logger.warn("Error stopping Ledger container", e);
      }
    }

    super.close();
  }
}
