package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import java.net.URL

class DeploymentSpecs(
    val deploymentSpecCurrent: URL?,
    val deploymentSpecDeployed: URL?
)

data class DeploymentSpec(val jsonRepresentation: String)
