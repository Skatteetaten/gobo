package no.skatteetaten.aurora.gobo.openshift

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.OffsetDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenshiftUser(
        val kind: String = "MISSING",
        val apiVersion: String = "MISSING",
        val fullName: String = "MISSING",
        val metadata: OpenShiftUserMetadata,
        val groups: List<String> = emptyList(),
        val identities: List<String> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenShiftUserMetadata(
        val name: String = "MISSING",
        val uid: String = "MISSING",
        val selfLink: String = "MISSING",
        val resourceVersion: String = "MISSING",
        val creationTimestamp: OffsetDateTime? = null
)
