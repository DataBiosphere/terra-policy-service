package bio.terra.policy.app;

import bio.terra.common.migrate.LiquibaseMigrator;
import bio.terra.policy.library.TpsMain;
import org.springframework.context.ApplicationContext;

public final class StartupInitializer {
  private static final String CHANGELOG_PATH = "policydb/changelog.xml";

  public static void initialize(ApplicationContext applicationContext) {
    // Initialize or upgrade the database depending on the configuration
    LiquibaseMigrator migrateService = applicationContext.getBean(LiquibaseMigrator.class);

    TpsDatabaseConfiguration tpsDatabaseConfiguration =
        applicationContext.getBean(TpsDatabaseConfiguration.class);

    if (tpsDatabaseConfiguration.isInitializeOnStart()) {
      migrateService.initialize(CHANGELOG_PATH, tpsDatabaseConfiguration.getDataSource());
    } else if (tpsDatabaseConfiguration.isUpgradeOnStart()) {
      migrateService.upgrade(CHANGELOG_PATH, tpsDatabaseConfiguration.getDataSource());
    }

    TpsMain.initialize(tpsDatabaseConfiguration.getDataSource());
  }
}
