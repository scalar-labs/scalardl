package com.scalar.dl.ledger.server;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.service.LedgerModule;
import com.scalar.dl.ledger.service.LedgerService;
import com.scalar.dl.ledger.service.LedgerValidationService;

public class LedgerServerModule extends AbstractModule {
  private final LedgerConfig config;
  private final Injector injector;

  public LedgerServerModule(LedgerConfig config) {
    this.config = config;
    injector = Guice.createInjector(new LedgerModule(config));
  }

  @Override
  protected void configure() {
    bind(GateKeeper.class).in(Singleton.class);
    bind(CommonService.class).in(Singleton.class);
  }

  @Provides
  @Singleton
  LedgerService provideLedgerService() {
    return injector.getInstance(LedgerService.class);
  }

  @Provides
  @Singleton
  LedgerValidationService provideLedgerValidationService() {
    return injector.getInstance(LedgerValidationService.class);
  }

  @Provides
  @Singleton
  Stats provideStats() {
    return new Stats(config.getProductName(), config.getServiceName());
  }

  @Provides
  @Singleton
  LedgerConfig provideLedgerConfig() {
    return config;
  }
}
