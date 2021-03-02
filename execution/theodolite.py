#!/usr/bin/env python

import argparse
from lib.cli_parser import benchmark_parser
import logging  # logging
import os
import run_uc
import sys
from strategies.config import ExperimentConfig
import strategies.strategies.domain_restriction.lower_bound_strategy as lower_bound_strategy
import strategies.strategies.domain_restriction.no_lower_bound_strategy as no_lower_bound_strategy
import strategies.strategies.search.check_all_strategy as check_all_strategy
import strategies.strategies.search.linear_search_strategy as linear_search_strategy
import strategies.strategies.search.binary_search_strategy as binary_search_strategy
from strategies.experiment_execution import ExperimentExecutor
import strategies.subexperiment_execution.subexperiment_executor as subexperiment_executor
import strategies.subexperiment_evaluation.subexperiment_evaluator as subexperiment_evaluator


def load_variables():
    """Load the CLI variables given at the command line"""
    print('Load CLI variables')
    parser = benchmark_parser("Run theodolite benchmarking")
    args = parser.parse_args()
    print(args)
    if (args.uc is None or args.loads is None or args.instances_list is None) and not args.reset_only:
        print('The options --uc, --loads and --instances are mandatory.')
        print('Some might not be set!')
        sys.exit(1)
    return args


def main(uc, loads, instances_list, partitions, cpu_limit, memory_limit,
         duration, domain_restriction, search_strategy, threshold,
         prometheus_base_url, reset, namespace, result_path, configurations):

    print(
        f"Domain restriction of search space activated: {domain_restriction}")
    print(f"Chosen search strategy: {search_strategy}")

    counter_path = f"{result_path}/exp_counter.txt"

    if os.path.exists(counter_path):
        with open(counter_path, mode="r") as read_stream:
            exp_id = int(read_stream.read())
    else:
        exp_id = 0
        # Create the directory if not exists
        os.makedirs(result_path, exist_ok=True)

    # Store metadata
    separator = ","
    lines = [
        f'UC={uc}\n',
        f'DIM_VALUES={separator.join(map(str, loads))}\n',
        f'REPLICAS={separator.join(map(str, instances_list))}\n',
        f'PARTITIONS={partitions}\n',
        f'CPU_LIMIT={cpu_limit}\n',
        f'MEMORY_LIMIT={memory_limit}\n',
        f'EXECUTION_MINUTES={duration}\n',
        f'DOMAIN_RESTRICTION={domain_restriction}\n',
        f'SEARCH_STRATEGY={search_strategy}\n',
        f'CONFIGURATIONS={configurations}'
    ]
    with open(f"{result_path}/exp{exp_id}_uc{uc}_meta.txt", "w") as stream:
        stream.writelines(lines)

    with open(counter_path, mode="w") as write_stream:
        write_stream.write(str(exp_id + 1))

    domain_restriction_strategy = None
    search_strategy_method = None

    # Select domain restriction
    if domain_restriction:
        # domain restriction
        domain_restriction_strategy = lower_bound_strategy
    else:
        # no domain restriction
        domain_restriction_strategy = no_lower_bound_strategy

    # select search strategy
    if search_strategy == "linear-search":
        print(
            f"Going to execute at most {len(loads)+len(instances_list)-1} subexperiments in total..")
        search_strategy_method = linear_search_strategy
    elif search_strategy == "binary-search":
        search_strategy_method = binary_search_strategy
    else:
        print(
            f"Going to execute {len(loads)*len(instances_list)} subexperiments in total..")
        search_strategy_method = check_all_strategy

    experiment_config = ExperimentConfig(
        use_case=uc,
        exp_id=exp_id,
        dim_values=loads,
        replicass=instances_list,
        partitions=partitions,
        cpu_limit=cpu_limit,
        memory_limit=memory_limit,
        execution_minutes=duration,
        prometheus_base_url=prometheus_base_url,
        reset=reset,
        namespace=namespace,
        configurations=configurations,
        result_path=result_path,
        domain_restriction_strategy=domain_restriction_strategy,
        search_strategy=search_strategy_method,
        threshold=threshold,
        subexperiment_executor=subexperiment_executor,
        subexperiment_evaluator=subexperiment_evaluator)

    executor = ExperimentExecutor(experiment_config)
    executor.execute()


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    args = load_variables()
    if args.reset_only:
        print('Only reset the cluster')
        run_uc.main(None, None, None, None, None, None, None, None, None,
                    None, args.namespace, None, None, reset_only=True)
    else:
        main(args.uc, args.loads, args.instances_list, args.partitions,
             args.cpu_limit, args.memory_limit, args.duration,
             args.domain_restriction, args.search_strategy,
             args.threshold, args.prometheus, args.reset, args.namespace,
             args.path, args.configurations)
