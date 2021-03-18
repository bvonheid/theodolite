package theodolite

import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.quarkus.test.junit.QuarkusTest
import io.smallrye.common.constraint.Assert.assertTrue
import org.junit.jupiter.api.Test
import theodolite.k8s.K8sResourceLoader
import theodolite.patcher.PatcherManager
import theodolite.util.PatcherDefinition

/**
 * Resource patcher test
 *
 * This class tested 4 scenarios for the ResourceLimitPatcher and the ResourceRequestPatcher.
 * The different test cases specifies four possible situations:
 * Case 1:  In the given YAML declaration memory and cpu are defined
 * Case 2:  In the given YAML declaration only cpu is defined
 * Case 3:  In the given YAML declaration only memory is defined
 * Case 4:  In the given YAML declaration neither `Resource Request` nor `Request Limit` is defined
 */
@QuarkusTest
class ResourceLimitPatcherTest {
    val testPath = "./src/main/resources/testYaml/"
    val loader = K8sResourceLoader(DefaultKubernetesClient().inNamespace(""))
    val manager = PatcherManager()

    fun applyTest(fileName: String) {
        val cpuValue = "50m"
        val memValue = "3Gi"
        val k8sResource = loader.loadK8sResource("Deployment", testPath + fileName) as Deployment

        val defCPU = PatcherDefinition()
        defCPU.variableName = "cpu"
        defCPU.resource = "cpu-memory-deployment.yaml"
        defCPU.container = "uc-application"
        defCPU.type = "ResourceLimitPatcher"

        val defMEM = PatcherDefinition()
        defMEM.variableName = "memory"
        defMEM.resource = "cpu-memory-deployment.yaml"
        defMEM.container = "uc-application"
        defMEM.type = "ResourceLimitPatcher"

        manager.applyPatcher(
            patcherDefinition = listOf(defCPU),
            resources = listOf(Pair("cpu-memory-deployment.yaml", k8sResource)),
            value = cpuValue
        )
        manager.applyPatcher(
            patcherDefinition = listOf(defMEM),
            resources = listOf(Pair("cpu-memory-deployment.yaml", k8sResource)),
            value = memValue
        )

        k8sResource.spec.template.spec.containers.filter { it.name == defCPU.container }
            .forEach {
                println(it)
                assertTrue(it.resources.limits["cpu"].toString() == cpuValue)
                assertTrue(it.resources.limits["memory"].toString() == memValue)
            }
    }

    @Test
    fun testWithExistingCpuAndMemoryDeclarations() {
        // Case 1: In the given YAML declaration memory and cpu are defined
        applyTest("cpu-memory-deployment.yaml")
    }

    @Test
    fun testOnlyWithExistingCpuDeclarations() {
        // Case 2:  In the given YAML declaration only cpu is defined
        applyTest("cpu-deployment.yaml")
    }

    @Test
    fun testOnlyWithExistingMemoryDeclarations() {
        //  Case 3:  In the given YAML declaration only memory is defined
        applyTest("memory-deployment.yaml")
    }

    @Test
    fun testWithoutResourceDeclarations() {
        // Case 4: In the given YAML declaration neither `Resource Request` nor `Request Limit` is defined
        applyTest("no-resources-deployment.yaml")
    }
}