package no.skatteetaten.aurora.gobo.graphql.applicationdeploymentdetails

import java.net.URL

class DeploymentSpecs(
    val deploymentSpecCurrent: URL?,
    val deploymentSpecDeployed: URL?
)

data class DeploymentSpec(val jsonRepresentation: String)
