package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.coxautodev.graphql.tools.GraphQLMutationResolver
import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.coxautodev.graphql.tools.GraphQLResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.resolvers.application.Application
import no.skatteetaten.aurora.gobo.resolvers.application.createApplicationEdge
import no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails.ApplicationDeploymentDetails
import no.skatteetaten.aurora.gobo.resolvers.loader
import no.skatteetaten.aurora.gobo.resolvers.namespace.Namespace
import no.skatteetaten.aurora.gobo.security.currentUser
import no.skatteetaten.aurora.gobo.service.ApplicationUpgradeService
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentQueryResolver(private val applicationService: ApplicationServiceBlocking) :
    GraphQLQueryResolver {

    fun getApplicationDeployment(id: String): ApplicationDeployment? =
        applicationService.getApplicationDeployment(id).let {
            ApplicationDeployment.create(it)
        }
}

@Component
class ApplicationDeploymentMutationResolver(
    private val applicationUpgradeService: ApplicationUpgradeService
) : GraphQLMutationResolver {

    fun redeployWithVersion(input: ApplicationDeploymentVersionInput, dfe: DataFetchingEnvironment): Boolean {
        applicationUpgradeService.upgrade(dfe.currentUser().token, input.applicationDeploymentId, input.version)
        return true
    }

    fun refreshApplicationDeployment(input: ApplicationDeploymentRefreshInput, dfe: DataFetchingEnvironment) =
        applicationUpgradeService.refreshApplicationDeployment(dfe.currentUser().token, input.applicationDeploymentId)
}

@Component
class ApplicationDeploymentResolver(
    private val applicationService: ApplicationService
) : GraphQLResolver<ApplicationDeployment> {

    fun affiliation(applicationDeployment: ApplicationDeployment): Affiliation =
        Affiliation(applicationDeployment.affiliationId)

    fun namespace(applicationDeployment: ApplicationDeployment): Namespace =
        Namespace(applicationDeployment.namespaceId, applicationDeployment.affiliationId)

    fun details(applicationDeployment: ApplicationDeployment, dfe: DataFetchingEnvironment) =
        dfe.loader(ApplicationDeploymentDetails::class).load(applicationDeployment.id)

    fun application(applicationDeployment: ApplicationDeployment): Application? {
        return applicationService.getApplication(applicationDeployment.applicationId)
            .map { createApplicationEdge(it).node }
            .block()
    }
}