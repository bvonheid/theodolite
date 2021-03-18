package theodolite.patcher

import io.fabric8.kubernetes.api.model.Container
import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.EnvVarSource
import io.fabric8.kubernetes.api.model.KubernetesResource
import io.fabric8.kubernetes.api.model.apps.Deployment

class EnvVarPatcher(
    private val k8sResource: KubernetesResource,
    private val container: String,
    private val variableName: String
) : AbstractPatcher(k8sResource, container, variableName) {

    override fun <String> patch(value: String) {
        if (k8sResource is Deployment) {
            this.setEnv(
                k8sResource, this.container,
                mapOf(this.variableName to value) as Map<kotlin.String, kotlin.String>
            )
        }
    }

    /**
     * Sets the ContainerEnvironmentVariables, creates new if variable does not exist.
     * @param container - The Container
     * @param map - Map of k=Name,v =Value of EnvironmentVariables
     */
    private fun setContainerEnv(container: Container, map: Map<String, String>) {
        map.forEach { (k, v) ->
            // filter for matching name and set value
            val x = container.env.filter { envVar -> envVar.name == k }

            if (x.isEmpty()) {
                val newVar = EnvVar(k, v, EnvVarSource())
                container.env.add(newVar)
            } else {
                x.forEach {
                    it.value = v
                }
            }
        }
    }

    /**
     * Set the environment Variable for a container
     */
    private fun setEnv(workloadDeployment: Deployment, containerName: String, map: Map<String, String>) {
        workloadDeployment.spec.template.spec.containers.filter { it.name == containerName }
            .forEach { setContainerEnv(it, map) }
    }
}