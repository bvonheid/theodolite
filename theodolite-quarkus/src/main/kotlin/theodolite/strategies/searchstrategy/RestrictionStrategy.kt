package theodolite.strategies.searchstrategy

import theodolite.util.Results
import theodolite.util.LoadDimension
import theodolite.util.Resource

abstract class RestrictionStrategy(val results: Results, val loads: List<LoadDimension>) {
    public abstract fun next(load: LoadDimension, resources: List<Resource>): List<Resource>;
}