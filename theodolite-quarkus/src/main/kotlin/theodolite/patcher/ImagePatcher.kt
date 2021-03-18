package theodolite.patcher

import io.fabric8.kubernetes.api.model.KubernetesResource
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.StatefulSet

class ImagePatcher(private val k8sResource: KubernetesResource, private val container: String) :
    AbstractPatcher(k8sResource, container) {

    override fun <String> patch(imagePath: String) {
        if (k8sResource is Deployment) {
            k8sResource.spec.template.spec.containers.filter { it.name == container }.forEach {
                it.image = imagePath as kotlin.String
            }
        } else if (k8sResource is StatefulSet) {
            k8sResource.spec.template.spec.containers.filter { it.name == container }.forEach {
                it.image = imagePath as kotlin.String
            }
        }
    }
}