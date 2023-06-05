package bio.terra.policy.common;

import io.micrometer.core.instrument.Metrics;

public class MetricsUtils {
  static final String SERVICE_NAME = "tps";

  /** Emit a metric for the number of policies explained. */
  public static void incrementPaoExplain() {
    Metrics.globalRegistry.counter(String.format("%s.pao.explain.count", SERVICE_NAME)).increment();
  }

  /** Emit a metric for the number of policies created. */
  public static void incrementPaoCreation() {
    Metrics.globalRegistry.counter(String.format("%s.pao.create.count", SERVICE_NAME)).increment();
  }

  /** Emit a metric for the number of policies requested. */
  public static void incrementPaoGet() {
    Metrics.globalRegistry.counter(String.format("%s.pao.get.count", SERVICE_NAME)).increment();
  }
}
