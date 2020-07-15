package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.expediagroup.graphql.spring.operations.Mutation
import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentService
import no.skatteetaten.aurora.gobo.integration.boober.DeleteApplicationDeploymentInput
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.integration.skap.RouteService
import no.skatteetaten.aurora.gobo.load
import no.skatteetaten.aurora.gobo.resolvers.AccessDeniedException
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.resolvers.application.Application
import no.skatteetaten.aurora.gobo.resolvers.application.DockerRegistry
import no.skatteetaten.aurora.gobo.resolvers.application.createApplicationEdge
import no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails.ApplicationDeploymentDetails
import no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails.ApplicationDeploymentDetailsDataLoader
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository
import no.skatteetaten.aurora.gobo.resolvers.loader
import no.skatteetaten.aurora.gobo.resolvers.namespace.Namespace
import no.skatteetaten.aurora.gobo.resolvers.route.BigipJob
import no.skatteetaten.aurora.gobo.resolvers.route.Route
import no.skatteetaten.aurora.gobo.resolvers.route.WebsealJob
import no.skatteetaten.aurora.gobo.security.currentUser
import no.skatteetaten.aurora.gobo.security.isAnonymousUser
import no.skatteetaten.aurora.gobo.service.ApplicationUpgradeService
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentQueryResolver(
    private val applicationService: ApplicationServiceBlocking,
    private val dockerRegistry: DockerRegistry
) : Query {

    fun getApplicationDeployment(id: String): ApplicationDeployment? =
        applicationService.getApplicationDeployment(id).let { resource ->
            val imageRepo = resource.dockerImageRepo
                .takeIf { it != null && !dockerRegistry.isInternal(it) }
                ?.let { ImageRepository.fromRepoString(it) }
            ApplicationDeployment.create(resource, imageRepo)
        }
}

@Component
class ApplicationDeploymentMutationResolver(
    private val applicationUpgradeService: ApplicationUpgradeService,
    private val applicationDeploymentService: ApplicationDeploymentService
) : Mutation {

    fun redeployWithVersion(input: ApplicationDeploymentVersionInput, dfe: DataFetchingEnvironment): Boolean {
        applicationUpgradeService.upgrade(dfe.currentUser().token, input.applicationDeploymentId, input.version)
        return true
    }

    fun redeployWithCurrentVersion(input: ApplicationDeploymentIdInput, dfe: DataFetchingEnvironment): Boolean {
        applicationUpgradeService.deployCurrentVersion(dfe.currentUser().token, input.applicationDeploymentId)
        return true
    }

    fun refreshApplicationDeployment(input: RefreshByApplicationDeploymentIdInput, dfe: DataFetchingEnvironment) =
        applicationUpgradeService.refreshApplicationDeployment(dfe.currentUser().token, input.applicationDeploymentId)

    fun refreshApplicationDeployments(input: RefreshByAffiliationsInput, dfe: DataFetchingEnvironment) =
        applicationUpgradeService.refreshApplicationDeployments(dfe.currentUser().token, input.affiliations)

    fun deleteApplicationDeployment(input: DeleteApplicationDeploymentInput, dfe: DataFetchingEnvironment) =
        applicationDeploymentService.deleteApplicationDeployment(dfe.currentUser().token, input)
}

@Component
class ApplicationDeploymentResolver(
    private val applicationService: ApplicationServiceBlocking,
    private val routeService: RouteService
) : Query {

    fun affiliation(applicationDeployment: ApplicationDeployment): Affiliation =
        Affiliation(applicationDeployment.affiliationId)

    fun namespace(applicationDeployment: ApplicationDeployment): Namespace =
        Namespace(applicationDeployment.namespaceId, applicationDeployment.affiliationId)

    suspend fun details(applicationDeployment: ApplicationDeployment, dfe: DataFetchingEnvironment) =
        dfe.load<String, ApplicationDeploymentDetails>(applicationDeployment.id)

    fun route(
        applicationDeployment: ApplicationDeployment,
        dfe: DataFetchingEnvironment
    ): Route? {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get WebSEAL/BigIp jobs")
        return Route(
            websealJobs =
            routeService.getSkapJobs(
                namespace(applicationDeployment).name,
                "${applicationDeployment.name}-webseal"
            ).map { WebsealJob.create(it) },
            bigipJobs = routeService.getSkapJobs(
                namespace(applicationDeployment).name,
                "${applicationDeployment.name}-bigip"
            ).map { BigipJob.create(it) }
        )
    }

    fun application(applicationDeployment: ApplicationDeployment): Application? {
        val application = applicationService.getApplication(applicationDeployment.applicationId)
        return createApplicationEdge(application).node
    }
}
