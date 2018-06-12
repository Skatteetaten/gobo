package no.skatteetaten.aurora.gobo.resolvers.namespace

import no.skatteetaten.aurora.gobo.resolvers.application.ApplicationsConnection

data class Namespace(val name: String, val affiliationId: String, val applications: ApplicationsConnection?)
