package no.skatteetaten.aurora.gobo.application

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.hateoas.ResourceSupport

@JsonIgnoreProperties(ignoreUnknown = true)
data class StatusResource(val code: String, val comment: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VersionResource(val deployTag: String, val auroraVersion: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApplicationResource(
    val affiliation: String,
    val environment: String,
    val name: String,
    val status: StatusResource,
    val version: VersionResource
) : ResourceSupport()