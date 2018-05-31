package no.skatteetaten.aurora.gobo.application

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.hateoas.ResourceSupport

@JsonIgnoreProperties(ignoreUnknown = true)
data class Status(val code: String, val comment: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Version(val deployTag: String, val auroraVersion: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Application(
    val affiliation: String,
    val environment: String,
    val name: String,
    val status: Status,
    val version: Version
) : ResourceSupport()
