#!/usr/bin/env python

import sys
import os
from strategies.config import ExperimentConfig
import strategies.strategies.domain_restriction.lower_bound_strategy as lower_bound_strategy
import strategies.strategies.domain_restriction.no_lower_bound_strategy as no_lower_bound_strategy
import strategies.strategies.search.check_all_strategy as check_all_strategy
import strategies.strategies.search.linear_search_strategy as linear_search_strategy
import strategies.strategies.search.binary_search_strategy as binary_search_strategy
from strategies.experiment_execution import ExperimentExecutor
import strategies.subexperiment_execution.subexperiment_executor as subexperiment_executor
import strategies.subexperiment_evaluation.subexperiment_evaluator as subexperiment_evaluator

uc=sys.argv[1]
dim_values=sys.argv[2].split(',')
replicas=sys.argv[3].split(',')
partitions=sys.argv[4] if len(sys.argv) >= 5 and sys.argv[4] else 40
cpu_limit=sys.argv[5] if len(sys.argv) >= 6 and sys.argv[5] else "1000m"
memory_limit=sys.argv[6] if len(sys.argv) >= 7 and sys.argv[6] else "4Gi"
kafka_streams_commit_interval_ms=sys.argv[7] if len(sys.argv) >= 8 and sys.argv[7] else 100
execution_minutes=sys.argv[8] if len(sys.argv) >= 9 and sys.argv[8] else 5
domain_restriction=bool(sys.argv[9]) if len(sys.argv) >= 10 and sys.argv[9] == "restrict-domain" else False
search_strategy=sys.argv[10] if len(sys.argv) >= 11 and (sys.argv[10] == "linear-search" or sys.argv[10] == "binary-search") else "check-all"

print(f"Domain restriction of search space activated: {domain_restriction}")
print(f"Chosen search strategy: {search_strategy}")

if os.path.exists("exp_counter.txt"):
    with open("exp_counter.txt", mode="r") as read_stream:
        exp_id = int(read_stream.read())
else:
    exp_id = 0

with open("exp_counter.txt", mode="w") as write_stream:
    write_stream.write(str(exp_id+1))

# Store metadata
separator = ","
lines = [
        f"UC={uc}\n",
        f"DIM_VALUES={separator.join(dim_values)}\n",
        f"REPLICAS={separator.join(replicas)}\n",
        f"PARTITIONS={partitions}\n",
        f"CPU_LIMIT={cpu_limit}\n",
        f"MEMORY_LIMIT={memory_limit}\n",
        f"KAFKA_STREAMS_COMMIT_INTERVAL_MS={kafka_streams_commit_interval_ms}\n",
        f"EXECUTION_MINUTES={execution_minutes}\n",
        f"DOMAIN_RESTRICTION={domain_restriction}\n",
        f"SEARCH_STRATEGY={search_strategy}"
        ]
with open(f"exp{exp_id}_uc{uc}_meta.txt", "w") as stream:
    stream.writelines(lines)

# domain restriction
if domain_restriction:
    # domain restriction + linear-search
    if search_strategy == "linear-search":
        print(f"Going to execute at most {len(dim_values)+len(replicas)-1} subexperiments in total..")
        experiment_config = ExperimentConfig(
            use_case=uc,
            exp_id=exp_id,
            dim_values=dim_values,
            replicass=replicas,
            partitions=partitions,
            cpu_limit=cpu_limit,
            memory_limit=memory_limit,
            kafka_streams_commit_interval_ms=kafka_streams_commit_interval_ms,
            execution_minutes=execution_minutes,
            domain_restriction_strategy=lower_bound_strategy,
            search_strategy=linear_search_strategy,
            subexperiment_executor=subexperiment_executor,
            subexperiment_evaluator=subexperiment_evaluator)
    # domain restriction + binary-search
    elif search_strategy == "binary-search":
        experiment_config = ExperimentConfig(
            use_case=uc,
            exp_id=exp_id,
            dim_values=dim_values,
            replicass=replicas,
            partitions=partitions,
            cpu_limit=cpu_limit,
            memory_limit=memory_limit,
            kafka_streams_commit_interval_ms=kafka_streams_commit_interval_ms,
            execution_minutes=execution_minutes,
            domain_restriction_strategy=lower_bound_strategy,
            search_strategy=binary_search_strategy,
            subexperiment_executor=subexperiment_executor,
            subexperiment_evaluator=subexperiment_evaluator)
    # domain restriction + check-all
    else:
        print(f"Going to execute {len(dim_values)*len(replicas)} subexperiments in total..")
        experiment_config = ExperimentConfig(
            use_case=uc,
            exp_id=exp_id,
            dim_values=dim_values,
            replicass=replicas,
            partitions=partitions,
            cpu_limit=cpu_limit,
            memory_limit=memory_limit,
            kafka_streams_commit_interval_ms=kafka_streams_commit_interval_ms,
            execution_minutes=execution_minutes,
            domain_restriction_strategy=lower_bound_strategy,
            search_strategy=check_all_strategy,
            subexperiment_executor=subexperiment_executor,
            subexperiment_evaluator=subexperiment_evaluator)
# no domain restriction
else:
    # no domain restriction + linear-search
    if search_strategy == "linear-search":
        print(f"Going to execute at most {len(dim_values)*len(replicas)} subexperiments in total..")
        experiment_config = ExperimentConfig(
            use_case=uc,
            exp_id=exp_id,
            dim_values=dim_values,
            replicass=replicas,
            partitions=partitions,
            cpu_limit=cpu_limit,
            memory_limit=memory_limit,
            kafka_streams_commit_interval_ms=kafka_streams_commit_interval_ms,
            execution_minutes=execution_minutes,
            domain_restriction_strategy=no_lower_bound_strategy,
            search_strategy=linear_search_strategy,
            subexperiment_executor=subexperiment_executor,
            subexperiment_evaluator=subexperiment_evaluator)
    # no domain restriction + binary-search
    elif search_strategy == "binary-search":
        experiment_config = ExperimentConfig(
            use_case=uc,
            exp_id=exp_id,
            dim_values=dim_values,
            replicass=replicas,
            partitions=partitions,
            cpu_limit=cpu_limit,
            memory_limit=memory_limit,
            kafka_streams_commit_interval_ms=kafka_streams_commit_interval_ms,
            execution_minutes=execution_minutes,
            domain_restriction_strategy=no_lower_bound_strategy,
            search_strategy=binary_search_strategy,
            subexperiment_executor=subexperiment_executor,
            subexperiment_evaluator=subexperiment_evaluator)
    # no domain restriction + check-all
    else:
        print(f"Going to execute {len(dim_values)*len(replicas)} subexperiments in total..")
        experiment_config = ExperimentConfig(
            use_case=uc,
            exp_id=exp_id,
            dim_values=dim_values,
            replicass=replicas,
            partitions=partitions,
            cpu_limit=cpu_limit,
            memory_limit=memory_limit,
            kafka_streams_commit_interval_ms=kafka_streams_commit_interval_ms,
            execution_minutes=execution_minutes,
            domain_restriction_strategy=no_lower_bound_strategy,
            search_strategy=check_all_strategy,
            subexperiment_executor=subexperiment_executor,
            subexperiment_evaluator=subexperiment_evaluator)

executor = ExperimentExecutor(experiment_config)
executor.execute()