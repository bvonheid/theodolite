package theodolite.patcher

import io.fabric8.kubernetes.api.model.KubernetesResource
import theodolite.util.PatcherDefinition

/**
 * The Patcher factory creates Patchers
 *
 * @constructor Create empty Patcher factory
 */
class PatcherFactory {
    /**
     * Create patcher a patcher based on the given PatcherDefinition and
     * the list of KubernetesResources.
     *
     * @param patcherDefinition The PatcherDefinition for which are
     *     Patcher should be created
     * @param k8sResources List of all available Kubernetes resources.
     *     This is a list of pairs<String, KubernetesResource>.
     *     The string corresponds to the filename where
     *     the Kubernetes resources were defined.
     * @return The created Patcher, throws `IllegalArgumentException` if no patcher can be created.
     */
    fun createPatcher(patcherDefinition: PatcherDefinition,
                      k8sResources: List<Pair<String, KubernetesResource>>) : Patcher {
        val resource =
            k8sResources.filter { it.first == patcherDefinition.resource }.map { resource -> resource.second }[0]
        return when (patcherDefinition.type) {
            "ReplicaPatcher" -> ReplicaPatcher(resource)
            "EnvVarPatcher" -> EnvVarPatcher(resource, patcherDefinition.container, patcherDefinition.variableName)
            "NodeSelectorPatcher" -> NodeSelectorPatcher(resource, patcherDefinition.variableName)
            "ResourceLimitPatcher" -> ResourceLimitPatcher(
                resource,
                patcherDefinition.container,
                patcherDefinition.variableName
            )
            "ResourceRequestPatcher" -> ResourceRequestPatcher(
                resource,
                patcherDefinition.container,
                patcherDefinition.variableName
            )
            "SchedulerNamePatcher" -> SchedulerNamePatcher(resource)
            else -> throw IllegalArgumentException("Patcher type ${patcherDefinition.type} not found")
        }
    }
}