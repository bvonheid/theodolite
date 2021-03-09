package theodolite.uc2.application;

import com.google.common.math.Stats;
import java.io.IOException;
import java.util.Properties;
import org.apache.commons.configuration2.Configuration;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.contrib.streaming.state.RocksDBStateBackend;
import org.apache.flink.formats.avro.registry.confluent.ConfluentRegistryAvroDeserializationSchema;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.runtime.state.memory.MemoryStateBackend;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.kafka.common.serialization.Serdes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import theodolite.commons.flink.serialization.FlinkKafkaKeyValueSerde;
import theodolite.commons.flink.serialization.StatsSerializer;
import titan.ccp.common.configuration.ServiceConfigurations;
import titan.ccp.model.records.ActivePowerRecord;


/**
 * The History microservice implemented as a Flink job.
 */
public class HistoryServiceFlinkJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(HistoryServiceFlinkJob.class);

  private final Configuration config = ServiceConfigurations.createWithDefaults();

  private void run() {
    final String applicationName = this.config.getString(ConfigurationKeys.APPLICATION_NAME);
    final String applicationVersion = this.config.getString(ConfigurationKeys.APPLICATION_VERSION);
    final String applicationId = applicationName + "-" + applicationVersion;
    final int commitIntervalMs = this.config.getInt(ConfigurationKeys.COMMIT_INTERVAL_MS);
    final String kafkaBroker = this.config.getString(ConfigurationKeys.KAFKA_BOOTSTRAP_SERVERS);
    final String inputTopic = this.config.getString(ConfigurationKeys.KAFKA_INPUT_TOPIC);
    final String outputTopic = this.config.getString(ConfigurationKeys.KAFKA_OUTPUT_TOPIC);
    final String schemaRegistryUrl = this.config.getString(ConfigurationKeys.SCHEMA_REGISTRY_URL);
    final int windowDuration = this.config.getInt(ConfigurationKeys.KAFKA_WINDOW_DURATION_MINUTES);
    final String stateBackend =
        this.config.getString(ConfigurationKeys.FLINK_STATE_BACKEND, "").toLowerCase();
    final String stateBackendPath = this.config
        .getString(ConfigurationKeys.FLINK_STATE_BACKEND_PATH, "/opt/flink/statebackend");
    final int memoryStateBackendSize =
        this.config.getInt(ConfigurationKeys.FLINK_STATE_BACKEND_MEMORY_SIZE,
            MemoryStateBackend.DEFAULT_MAX_STATE_SIZE);
    final boolean checkpointing = this.config.getBoolean(ConfigurationKeys.CHECKPOINTING, true);

    final Properties kafkaProps = new Properties();
    kafkaProps.setProperty("bootstrap.servers", kafkaBroker);
    kafkaProps.setProperty("group.id", applicationId);

    final DeserializationSchema<ActivePowerRecord> sourceSerde =
        ConfluentRegistryAvroDeserializationSchema.forSpecific(
            ActivePowerRecord.class,
            schemaRegistryUrl);

    final FlinkKafkaConsumer<ActivePowerRecord> kafkaSource = new FlinkKafkaConsumer<>(
        inputTopic, sourceSerde, kafkaProps);

    kafkaSource.setStartFromGroupOffsets();
    if (checkpointing) {
      kafkaSource.setCommitOffsetsOnCheckpoints(true);
    }
    kafkaSource.assignTimestampsAndWatermarks(WatermarkStrategy.forMonotonousTimestamps());

    final KafkaSerializationSchema<Tuple2<String, String>> sinkSerde =
        new FlinkKafkaKeyValueSerde<>(outputTopic,
            Serdes::String,
            Serdes::String,
            TypeInformation.of(new TypeHint<Tuple2<String, String>>() {}));
    kafkaProps.setProperty("transaction.timeout.ms", "" + 5 * 60 * 1000);
    final FlinkKafkaProducer<Tuple2<String, String>> kafkaSink = new FlinkKafkaProducer<>(
        outputTopic, sinkSerde, kafkaProps, FlinkKafkaProducer.Semantic.AT_LEAST_ONCE);
    kafkaSink.setWriteTimestampToKafka(true);

    final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
    if (checkpointing) {
      env.enableCheckpointing(commitIntervalMs);
    }

    // State Backend
    if (stateBackend.equals("filesystem")) {
      env.setStateBackend(new FsStateBackend(stateBackendPath));
    } else if (stateBackend.equals("rocksdb")) {
      try {
        env.setStateBackend(new RocksDBStateBackend(stateBackendPath, true));
      } catch (final IOException e) {
        LOGGER.error("Cannot create RocksDB state backend.", e);
      }
    } else {
      env.setStateBackend(new MemoryStateBackend(memoryStateBackendSize));
    }

    env.getConfig().registerTypeWithKryoSerializer(Stats.class, new StatsSerializer());

    env.getConfig().getRegisteredTypesWithKryoSerializers()
        .forEach((c, s) -> LOGGER.info("Class " + c.getName() + " registered with serializer "
            + s.getSerializer().getClass().getName()));

    final DataStream<ActivePowerRecord> stream = env.addSource(kafkaSource)
        .name("[Kafka Consumer] Topic: " + inputTopic);

    stream
        .rebalance()
        .keyBy((KeySelector<ActivePowerRecord, String>) ActivePowerRecord::getIdentifier)
        .window(TumblingEventTimeWindows.of(Time.minutes(windowDuration)))
        .aggregate(new StatsAggregateFunction(), new StatsProcessWindowFunction())
        .map(new MapFunction<Tuple2<String, Stats>, Tuple2<String, String>>() {
          @Override
          public Tuple2<String, String> map(final Tuple2<String, Stats> t) {
            final String key = t.f0;
            final String value = t.f1.toString();
            LOGGER.info("{}: {}", key, value);
            return new Tuple2<>(key, value);
          }
        }).name("map")
        .addSink(kafkaSink).name("[Kafka Producer] Topic: " + outputTopic);

    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Execution Plan: " + env.getExecutionPlan());
    }

    try {
      env.execute(applicationId);
    } catch (final Exception e) { // NOPMD Execution thrown by Flink
      LOGGER.error("An error occured while running this job.", e);
    }
  }

  public static void main(final String[] args) {
    new HistoryServiceFlinkJob().run();
  }
}
