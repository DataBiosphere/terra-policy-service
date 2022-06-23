package bio.terra.policy.library;

import bio.terra.common.migrate.LiquibaseMigrator;
import bio.terra.policy.library.configuration.TpsDatabaseConfiguration;
import org.springframework.context.ApplicationContext;

/**
 * "Top" of the policy service handles initialization. I'm using a static for the dataSource to
 * avoid any sequencing confusion over autowiring.
 */
public class TpsMain {
  private static final String CHANGELOG_PATH = "policydb/changelog.xml";

  public static void initialize(
      ApplicationContext applicationContext, LiquibaseMigrator migrateService) {
    TpsDatabaseConfiguration tpsDatabaseConfiguration =
        applicationContext.getBean(TpsDatabaseConfiguration.class);

    if (tpsDatabaseConfiguration.isInitializeOnStart()) {
      migrateService.initialize(CHANGELOG_PATH, tpsDatabaseConfiguration.getDataSource());
    } else if (tpsDatabaseConfiguration.isUpgradeOnStart()) {
      migrateService.upgrade(CHANGELOG_PATH, tpsDatabaseConfiguration.getDataSource());
    }
  }
}
