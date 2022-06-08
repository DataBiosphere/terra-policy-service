package bio.terra.policy.service.status;

import bio.terra.policy.app.configuration.StatusCheckConfiguration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseStatusService {
  private static final Logger logger = LoggerFactory.getLogger(BaseStatusService.class);
  private static final int PARALLELISM_THRESHOLD = 1;
  /** cached status */
  private final AtomicBoolean statusOk;
  /** configuration parameters */
  private final StatusCheckConfiguration configuration;
  /** set of status methods to check */
  private final ConcurrentHashMap<String, Supplier<Boolean>> statusCheckMap;
  /** scheduler */
  private final ScheduledExecutorService scheduler;
  /** last time cache was updated */
  private Instant lastStatusUpdate;

  public BaseStatusService(StatusCheckConfiguration configuration) {
    this.configuration = configuration;
    this.statusCheckMap = new ConcurrentHashMap<>();

    this.statusOk = new AtomicBoolean(false);
    this.lastStatusUpdate = Instant.now();
    this.scheduler = Executors.newScheduledThreadPool(1);
  }

  @PostConstruct
  public void startStatusChecking() {
    if (configuration.enabled()) {
      scheduler.scheduleAtFixedRate(
          this::checkStatus,
          configuration.startupWaitSeconds(),
          configuration.pollingIntervalSeconds(),
          TimeUnit.SECONDS);
    }
  }

  public void registerStatusCheck(String name, Supplier<Boolean> checkFn) {
    statusCheckMap.put(name, checkFn);
  }

  public void checkStatus() {
    if (configuration.enabled()) {
      AtomicBoolean summaryOk = new AtomicBoolean(true);
      statusCheckMap.forEach(
          PARALLELISM_THRESHOLD,
          (name, fn) -> {
            boolean isOk;
            try {
              isOk = fn.get();
            } catch (Exception e) {
              logger.warn("Status check exception for " + name, e);
              isOk = false;
            }
            // If not OK, set to summary to false. We only ever go from true -> false
            // so there are no concurrency issues here.
            if (!isOk) {
              summaryOk.set(false);
            }
          });
      statusOk.set(summaryOk.get());
      lastStatusUpdate = Instant.now();
    }
  }

  public boolean getCurrentStatus() {
    if (configuration.enabled()) {
      // If staleness time (last update + stale threshold) is before the current time, then
      // we are officially not OK.
      if (lastStatusUpdate
          .plusSeconds(configuration.stalenessThresholdSeconds())
          .isBefore(Instant.now())) {
        logger.warn("Status has not been updated since {}", lastStatusUpdate);
        statusOk.set(false);
      }
      return statusOk.get();
    }
    return true;
  }
}
