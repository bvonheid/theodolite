package theodolite.uc3.application;

import com.google.common.math.Stats;
import com.google.common.math.StatsAccumulator;
import org.apache.flink.api.common.functions.AggregateFunction;
import theodolite.uc3.application.util.KeyAndStats;
import theodolite.uc3.application.util.StatsFactory;
import titan.ccp.model.records.ActivePowerRecord;


/**
 * Statistical aggregation of {@link ActivePowerRecord}s using {@link Stats}.
 */
public class StatsAggregateFunction
    implements AggregateFunction<ActivePowerRecord, Stats, Stats>,
    // CHECKSTYLE.OFF: LineLength
    de.tub.dima.scotty.core.windowFunction.AggregateFunction<ActivePowerRecord, KeyAndStats, KeyAndStats> {
  // CHECKSTYLE.ON: LineLength

  private static final long serialVersionUID = -8873572990921515499L; // NOPMD

  // Flink Aggregate Function methods

  @Override
  public Stats createAccumulator() {
    return Stats.of();
  }

  @Override
  public Stats add(final ActivePowerRecord value, final Stats accumulator) {
    return StatsFactory.accumulate(accumulator, value.getValueInW());
  }

  @Override
  public Stats getResult(final Stats accumulator) {
    return accumulator;
  }

  @Override
  public Stats merge(final Stats a, final Stats b) {
    final StatsAccumulator statsAccumulator = new StatsAccumulator();
    statsAccumulator.addAll(a);
    statsAccumulator.addAll(b);
    return statsAccumulator.snapshot();
  }


  // Scotty Aggregate Function methods

  @Override
  public KeyAndStats lift(final ActivePowerRecord apr) {
    final StatsAccumulator statsAccumulator = new StatsAccumulator();
    statsAccumulator.add(apr.getValueInW());
    return new KeyAndStats(apr.getIdentifier(), statsAccumulator.snapshot());
  }

  @Override
  public KeyAndStats combine(final KeyAndStats ks1, final KeyAndStats ks2) {
    final StatsAccumulator statsAccumulator = new StatsAccumulator();
    statsAccumulator.addAll(ks1.getStats());
    statsAccumulator.addAll(ks2.getStats());
    return new KeyAndStats(ks1.getKey(), statsAccumulator.snapshot());
  }

  @Override
  public KeyAndStats lower(final KeyAndStats ks) {
    return ks;
  }
}
