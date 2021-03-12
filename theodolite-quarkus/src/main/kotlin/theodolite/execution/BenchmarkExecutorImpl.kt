package theodolite.execution

import mu.KotlinLogging
import theodolite.benchmark.Benchmark
import theodolite.benchmark.BenchmarkExecution
import theodolite.evaluation.ExternalSLOChecker
import theodolite.util.ConfigurationOverride
import theodolite.util.LoadDimension
import theodolite.util.Resource
import theodolite.util.Results
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {}

class BenchmarkExecutorImpl(
    benchmark: Benchmark,
    results: Results,
    executionDuration: Duration,
    private val configurationOverrides: List<ConfigurationOverride>,
    slo: BenchmarkExecution.Slo
) : BenchmarkExecutor(benchmark, results, executionDuration, configurationOverrides, slo) {
    //TODO ADD SHUTDOWN HOOK HERE
    override fun runExperiment(load: LoadDimension, res: Resource): Boolean {
        val benchmarkDeployment = benchmark.buildDeployment(load, res, this.configurationOverrides)
        benchmarkDeployment.setup()
        this.waitAndLog()
        benchmarkDeployment.teardown()
        // todo evaluate

        var result = false
        try {
            result = ExternalSLOChecker(
                slo.prometheusUrl,
                "sum by(group)(kafka_consumergroup_group_lag >= 0)",
                slo.externalSloUrl,
                slo.threshold,
                Duration.ofHours(slo.offset.toLong()),
                slo.warmup
            )
                .evaluate(
                    Instant.now().minus(executionDuration),
                    Instant.now()
                )
        } catch (e: Exception) {
            logger.error { "Evaluation failed for resource: ${res.get()} and load: ${load.get()} error: $e" }
        }

        this.results.setResult(Pair(load, res), result)
        return result
    }
}
