package theodolite.uc3.streamprocessing;

import com.google.common.math.Stats;
import com.google.common.math.StatsAccumulator;
import de.tub.dima.scotty.core.windowFunction.AggregateFunction;
import titan.ccp.model.records.ActivePowerRecord;


@SuppressWarnings("serial")
public class StatsWindowFunction implements AggregateFunction<ActivePowerRecord, Stats, Stats> {

  @Override
  public Stats lift(final ActivePowerRecord apr) {
    final StatsAccumulator statsAccumulator = new StatsAccumulator();
    statsAccumulator.add(apr.getValueInW());
    return statsAccumulator.snapshot();
  }

  @Override
  public Stats combine(final Stats s1, final Stats s2) {
    final StatsAccumulator statsAccumulator = new StatsAccumulator();
    statsAccumulator.addAll(s1);
    statsAccumulator.addAll(s2);
    return statsAccumulator.snapshot();
  }

  @Override
  public Stats lower(final Stats s) {
    return s;
  }

}
