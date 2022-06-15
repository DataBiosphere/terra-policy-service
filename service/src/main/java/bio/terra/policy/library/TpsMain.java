package bio.terra.policy.library;

import javax.sql.DataSource;

/**
 * "Top" of the policy service handles initialization. I'm using a static for the dataSource to
 * avoid any sequencing confusion over autowiring.
 */
public class TpsMain {
  private static final String CHANGELOG_PATH = "/policydb/changelog";

  private static DataSource tpsDataSource;

  public static void initialize(DataSource dataSource) {
    tpsDataSource = dataSource;
  }

  public static DataSource getTpsDataSource() {
    return tpsDataSource;
  }
}
