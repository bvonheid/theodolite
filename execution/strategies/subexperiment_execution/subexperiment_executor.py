# Wrapper that makes the execution method of a subexperiment interchangable.

import os
import run_uc

def execute(subexperiment_config):
    run_uc.main(
        exp_id=subexperiment_config.exp_id,
        uc_id=subexperiment_config.use_case,
        dim_value=int(subexperiment_config.dim_value),
        instances=int(subexperiment_config.replicas),
        partitions=subexperiment_config.partitions,
        cpu_limit=subexperiment_config.cpu_limit,
        memory_limit=subexperiment_config.memory_limit,
        execution_minutes=int(subexperiment_config.execution_minutes),
        prometheus_base_url=subexperiment_config.prometheus_base_url,
        reset=subexperiment_config.reset,
        ns=subexperiment_config.namespace,
        result_path=subexperiment_config.result_path,
        configurations=subexperiment_config.configurations)
