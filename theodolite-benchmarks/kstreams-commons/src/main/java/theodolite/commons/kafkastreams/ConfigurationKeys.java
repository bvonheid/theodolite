package theodolite.commons.kafkastreams;

/**
 * Keys to access configuration parameters.
 */
public final class ConfigurationKeys {
  // Common keys
  public static final String APPLICATION_NAME = "application.name";

  public static final String APPLICATION_VERSION = "application.version";

  public static final String KAFKA_BOOTSTRAP_SERVERS = "kafka.bootstrap.servers";

  public static final String SCHEMA_REGISTRY_URL = "schema.registry.url";

  public static final String KAFKA_INPUT_TOPIC = "kafka.input.topic";

  // Additional topics
  public static final String KAFKA_FEEDBACK_TOPIC = "kafka.feedback.topic";

  public static final String KAFKA_OUTPUT_TOPIC = "kafka.output.topic";

  public static final String KAFKA_CONFIGURATION_TOPIC = "kafka.configuration.topic";


  // UC2
  public static final String KAFKA_WINDOW_DURATION_MINUTES = "kafka.window.duration.minutes";

  // UC3
  public static final String AGGREGATION_DURATION = "aggregation.duration";

  public static final String AGGREGATION_ADVANCE = "aggregation.advance";

  public static final String WINDOW_PROCESSOR = "window.processor";

  // UC4
  public static final String EMIT_PERIOD_MS = "emit.period.ms";

  public static final String GRACE_PERIOD_MS = "grace.period.ms";

  private ConfigurationKeys() {}

}
