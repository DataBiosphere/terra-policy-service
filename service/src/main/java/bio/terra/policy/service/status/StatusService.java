package bio.terra.policy.service.status;

import bio.terra.policy.app.configuration.StatusCheckConfiguration;
import java.sql.Connection;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class StatusService extends BaseStatusService {
  private static final Logger logger = LoggerFactory.getLogger(StatusService.class);

  private final NamedParameterJdbcTemplate jdbcTemplate;
  private final int databaseCheckTimeout;

  @Autowired
  public StatusService(
      NamedParameterJdbcTemplate jdbcTemplate, StatusCheckConfiguration configuration) {
    super(configuration);
    // Heuristic for database timeout - half of the polling interval
    this.databaseCheckTimeout = configuration.pollingIntervalSeconds() / 2;
    this.jdbcTemplate = jdbcTemplate;
    registerStatusCheck("CloudSQL", this::databaseStatus);
  }

  private Boolean databaseStatus() {
    try {
      logger.debug("Checking database connection valid");
      return jdbcTemplate.getJdbcTemplate().execute(this::isConnectionValid);
    } catch (Exception e) {
      logger.warn("Database status check failed", e);
      return false;
    }
  }

  private Boolean isConnectionValid(Connection connection) throws SQLException {
    return connection.isValid(databaseCheckTimeout);
  }
}
