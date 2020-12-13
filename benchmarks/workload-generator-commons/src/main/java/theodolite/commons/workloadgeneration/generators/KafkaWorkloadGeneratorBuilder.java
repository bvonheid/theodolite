package theodolite.commons.workloadgeneration.generators;

import java.time.Duration;
import java.util.Objects;
import org.apache.avro.specific.SpecificRecord;
import theodolite.commons.workloadgeneration.communication.kafka.KafkaRecordSender;
import theodolite.commons.workloadgeneration.dimensions.KeySpace;
import theodolite.commons.workloadgeneration.functions.BeforeAction;
import theodolite.commons.workloadgeneration.functions.MessageGenerator;
import theodolite.commons.workloadgeneration.misc.ZooKeeper;

/**
 * Builder for {@link workload generators}.
 *
 * @param <T> the record for which the builder is dedicated for.
 */
public final class KafkaWorkloadGeneratorBuilder<T extends SpecificRecord> { // NOPMD

  private int instances; // NOPMD
  private ZooKeeper zooKeeper; // NOPMD
  private KeySpace keySpace; // NOPMD
  private int threads; // NOPMD
  private Duration period; // NOPMD
  private Duration duration; // NOPMD
  private BeforeAction beforeAction; // NOPMD
  private MessageGenerator<T> generatorFunction; // NOPMD
  private KafkaRecordSender<T> kafkaRecordSender; // NOPMD

  private KafkaWorkloadGeneratorBuilder() {

  }

  /**
   * Get a builder for the {@link KafkaWorkloadGenerator}.
   *
   * @return the builder.
   */
  public static <T extends SpecificRecord> KafkaWorkloadGeneratorBuilder<T> builder() {
    return new KafkaWorkloadGeneratorBuilder<>();
  }

  /**
   * Set the number of instances.
   *
   * @param instances the number of instances.
   * @return the builder.
   */
  public KafkaWorkloadGeneratorBuilder<T> instances(final int instances) {
    this.instances = instances;
    return this;
  }

  /**
   * Set the ZooKeeper reference.
   *
   * @param zooKeeper a reference to the ZooKeeper instance.
   * @return the builder.
   */
  public KafkaWorkloadGeneratorBuilder<T> zooKeeper(final ZooKeeper zooKeeper) {
    this.zooKeeper = zooKeeper;
    return this;
  }

  /**
   * Set the before action for the {@link KafkaWorkloadGenerator}.
   *
   * @param beforeAction the {@link BeforeAction}.
   * @return the builder.
   */
  public KafkaWorkloadGeneratorBuilder<T> beforeAction(final BeforeAction beforeAction) {
    this.beforeAction = beforeAction;
    return this;
  }

  /**
   * Set the key space for the {@link KafkaWorkloadGenerator}.
   *
   * @param keySpace the {@link KeySpace}.
   * @return the builder.
   */
  public KafkaWorkloadGeneratorBuilder<T> keySpace(final KeySpace keySpace) {
    this.keySpace = keySpace;
    return this;
  }

  /**
   * Set the key space for the {@link KafkaWorkloadGenerator}.
   *
   * @param threads the number of threads.
   * @return the builder.
   */
  public KafkaWorkloadGeneratorBuilder<T> threads(final int threads) {
    this.threads = threads;
    return this;
  }

  /**
   * Set the period for the {@link KafkaWorkloadGenerator}.
   *
   * @param period the {@link Period}
   * @return the builder.
   */
  public KafkaWorkloadGeneratorBuilder<T> period(final Duration period) {
    this.period = period;
    return this;
  }

  /**
   * Set the durtion for the {@link KafkaWorkloadGenerator}.
   *
   * @param duration the {@link Duration}.
   * @return the builder.
   */
  public KafkaWorkloadGeneratorBuilder<T> duration(final Duration duration) {
    this.duration = duration;
    return this;
  }

  /**
   * Set the generator function for the {@link KafkaWorkloadGenerator}.
   *
   * @param generatorFunction the generator function.
   * @return the builder.
   */
  public KafkaWorkloadGeneratorBuilder<T> generatorFunction(
      final MessageGenerator<T> generatorFunction) {
    this.generatorFunction = generatorFunction;
    return this;
  }

  /**
   * Set the {@link KafkaRecordSender} for the {@link KafkaWorkloadGenerator}.
   *
   * @param kafkaRecordSender the record sender to use.
   * @return the builder.
   */
  public KafkaWorkloadGeneratorBuilder<T> kafkaRecordSender(
      final KafkaRecordSender<T> kafkaRecordSender) {
    this.kafkaRecordSender = kafkaRecordSender;
    return this;
  }

  /**
   * Build the actual {@link KafkaWorkloadGenerator}. The following parameters are must be
   * specicified before this method is called:
   * <ul>
   * <li>zookeeper</li>
   * <li>key space</li>
   * <li>period</li>
   * <li>duration</li>
   * <li>generator function</li>
   * <li>kafka record sender</li>
   * </ul>
   *
   * @return the built instance of the {@link KafkaWorkloadGenerator}.
   */
  public KafkaWorkloadGenerator<T> build() {
    if (this.instances < 1) { // NOPMD
      throw new IllegalArgumentException(
          "Please specify a valid number of instances. Currently: " + this.instances);
    }
    Objects.requireNonNull(this.zooKeeper, "Please specify the ZooKeeper instance.");
    if (this.threads < 1) { // NOPMD
      this.threads = 1;
    }
    Objects.requireNonNull(this.keySpace, "Please specify the key space.");
    Objects.requireNonNull(this.period, "Please specify the period.");
    Objects.requireNonNull(this.duration, "Please specify the duration.");
    this.beforeAction = Objects.requireNonNullElse(this.beforeAction, () -> {
    });
    Objects.requireNonNull(this.generatorFunction, "Please specify the generator function.");
    Objects.requireNonNull(this.kafkaRecordSender, "Please specify the kafka record sender.");

    return new KafkaWorkloadGenerator<>(
        this.instances,
        this.zooKeeper,
        this.keySpace,
        this.threads,
        this.period,
        this.duration,
        this.beforeAction,
        this.generatorFunction,
        this.kafkaRecordSender);
  }
}
