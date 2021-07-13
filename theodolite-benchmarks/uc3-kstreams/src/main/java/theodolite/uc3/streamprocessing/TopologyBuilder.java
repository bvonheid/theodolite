package theodolite.uc3.streamprocessing;

import com.google.common.math.Stats;
import de.tub.dima.scotty.core.windowType.SlidingWindow;
import de.tub.dima.scotty.core.windowType.WindowMeasure;
import de.tub.dima.scotty.kafkastreamsconnector.KeyedScottyWindowTransformerSupplier;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Properties;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import theodolite.uc3.streamprocessing.util.StatsFactory;
import titan.ccp.common.kafka.GenericSerde;
import titan.ccp.common.kafka.avro.SchemaRegistryAvroSerdeFactory;
import titan.ccp.model.records.ActivePowerRecord;

/**
 * Builds Kafka Stream Topology for the History microservice.
 */
public class TopologyBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(TopologyBuilder.class);

  private final ZoneId zone = ZoneId.of("Europe/Paris"); // TODO as parameter


  private final String inputTopic;
  private final String outputTopic;
  private final SchemaRegistryAvroSerdeFactory srAvroSerdeFactory;
  private final Duration aggregationDuration;
  private final Duration aggregationAdvance;
  private final String windowProcessor;

  private final StreamsBuilder builder = new StreamsBuilder();

  /**
   * Create a new {@link TopologyBuilder} using the given topics.
   */
  public TopologyBuilder(final String inputTopic, final String outputTopic,
      final SchemaRegistryAvroSerdeFactory srAvroSerdeFactory,
      final Duration aggregationDuration, final Duration aggregationAdvance,
      final String windowProcessor) {
    this.inputTopic = inputTopic;
    this.outputTopic = outputTopic;
    this.srAvroSerdeFactory = srAvroSerdeFactory;
    this.aggregationDuration = aggregationDuration;
    this.aggregationAdvance = aggregationAdvance;
    this.windowProcessor = windowProcessor;
  }

  /**
   * Build the {@link Topology} for the History microservice.
   */
  public Topology build(final Properties properties) {
    final StatsKeyFactory<HourOfDayKey> keyFactory = new HourOfDayKeyFactory();
    final Serde<HourOfDayKey> keySerde = HourOfDayKeySerde.create();

    final KStream<HourOfDayKey, ActivePowerRecord> newKeyStream = this.builder
        .stream(this.inputTopic,
            Consumed.with(Serdes.String(),
                this.srAvroSerdeFactory.<ActivePowerRecord>forValues()))
        .selectKey((key, value) -> {
          final Instant instant = Instant.ofEpochMilli(value.getTimestamp());
          final LocalDateTime dateTime = LocalDateTime.ofInstant(instant, this.zone);
          return keyFactory.createKey(value.getIdentifier(), dateTime);
        });

    KStream<String, String> resultStream;

    if ("scotty".equals(this.windowProcessor)) {
      LOGGER.info("Use Scotty Window Function with {} window time and {} advance time",
          this.aggregationDuration, this.aggregationAdvance);
      final SlidingWindow slidingWindow = new SlidingWindow(WindowMeasure.Time,
          this.aggregationDuration.toMillis(), this.aggregationAdvance.toMillis());
      // CHECKSTYLE.OFF: LineLength
      final KeyedScottyWindowTransformerSupplier<HourOfDayKey, ActivePowerRecord, KeyValue<HourOfDayKey, Stats>> scottyTransformerSupplier =
          new KeyedScottyWindowTransformerSupplier<>(new StatsWindowFunction(), 0);
      // CHECKSTYLE.ON: LineLength
      scottyTransformerSupplier.addWindow(slidingWindow);
      resultStream =
          newKeyStream
              // optional, same keys, should be on same partition
              // .repartition(Repartitioned.with(keySerde, this.srAvroSerdeFactory.forValues()))
              .transform(scottyTransformerSupplier)
              .map((key, stats) -> KeyValue.pair(
                  key.getSensorId(),
                  stats.toString()));

    } else {
      LOGGER.info("Use KStreams Window Function with {} window time and {} advance time",
          this.aggregationDuration, this.aggregationAdvance);
      resultStream =
          newKeyStream
              .groupByKey(Grouped.with(keySerde, this.srAvroSerdeFactory.forValues()))
              .windowedBy(
                  TimeWindows.of(this.aggregationDuration).advanceBy(this.aggregationAdvance))
              .aggregate(
                  () -> Stats.of(),
                  (k, record, stats) -> StatsFactory.accumulate(stats, record.getValueInW()),
                  Materialized.with(keySerde,
                      GenericSerde.from(Stats::toByteArray, Stats::fromByteArray)))
              .toStream()
              .map((key, stats) -> KeyValue.pair(
                  keyFactory.getSensorId(key.key()),
                  stats.toString()));
    }

    resultStream
        .peek((k, v) -> LOGGER.info("{}: {}", k, v)) // TODO Temp logging
        .to(
            this.outputTopic,
            Produced.with(
                Serdes.String(),
                Serdes.String()));

    return this.builder.build(properties);
  }
}
