package theodolite.execution

import io.quarkus.runtime.annotations.QuarkusMain
import mu.KotlinLogging
import theodolite.benchmark.BenchmarkExecution
import theodolite.benchmark.KubernetesBenchmark
import theodolite.util.YamlParser
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

@QuarkusMain(name = "TheodoliteYamlExecutor")
object TheodoliteYamlExecutor {
    @JvmStatic
    fun main(args: Array<String>) {
        logger.info { "Theodolite started" }

        val executionPath = System.getenv("THEODOLITE_EXECUTION") ?: "./config/BenchmarkExecution.yaml"
        val benchmarkPath = System.getenv("THEODOLITE_BENCHMARK") ?: "./config/BenchmarkType.yaml"

        logger.info { "Using $executionPath for BenchmarkExecution" }
        logger.info { "Using $benchmarkPath for BenchmarkType" }


        // load the BenchmarkExecution and the BenchmarkType
        val parser = YamlParser()
        val benchmarkExecution =
            parser.parse(path = executionPath, E = BenchmarkExecution::class.java)!!
        val benchmark =
            parser.parse(path = benchmarkPath, E = KubernetesBenchmark::class.java)!!

        val shutdown = Shutdown(benchmarkExecution, benchmark)
        Runtime.getRuntime().addShutdownHook(shutdown)

        val executor = TheodoliteExecutor(benchmarkExecution, benchmark)
        executor.run()
        logger.info { "Theodolite finished" }
        Runtime.getRuntime().removeShutdownHook(shutdown)
        exitProcess(0)
    }
}
