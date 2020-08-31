package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.expediagroup.graphql.spring.operations.Mutation
import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.resolvers.application.Application
import no.skatteetaten.aurora.gobo.resolvers.application.DockerRegistryUtil
import no.skatteetaten.aurora.gobo.resolvers.application.createApplicationEdge
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository
import no.skatteetaten.aurora.gobo.resolvers.token
import no.skatteetaten.aurora.gobo.service.ApplicationUpgradeService
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentMutation(
    private val applicationUpgradeService: ApplicationUpgradeService,
    private val applicationDeploymentService: ApplicationDeploymentService
) : Mutation {

    suspend fun redeployWithVersion(input: ApplicationDeploymentVersionInput, dfe: DataFetchingEnvironment): Boolean {
        applicationUpgradeService.upgrade(dfe.token(), input.applicationDeploymentId, input.version)
        return true
    }

    fun redeployWithCurrentVersion(input: ApplicationDeploymentIdInput, dfe: DataFetchingEnvironment): Boolean {
        applicationUpgradeService.deployCurrentVersion(dfe.token(), input.applicationDeploymentId)
        return true
    }

    fun refreshApplicationDeployment(input: RefreshByApplicationDeploymentIdInput, dfe: DataFetchingEnvironment) =
        applicationUpgradeService.refreshApplicationDeployment(dfe.token(), input.applicationDeploymentId)

    fun refreshApplicationDeployments(input: RefreshByAffiliationsInput, dfe: DataFetchingEnvironment) =
        applicationUpgradeService.refreshApplicationDeployments(dfe.token(), input.affiliations)

    fun deleteApplicationDeployment(input: DeleteApplicationDeploymentInput, dfe: DataFetchingEnvironment) =
        applicationDeploymentService.deleteApplicationDeployment(dfe.token(), input)
}

@Component
class ApplicationDeploymentQuery(
    private val applicationService: ApplicationService
) : Query {

    suspend fun applicationDeployment(id: String): ApplicationDeployment? =
        applicationService.getApplicationDeployment(id).let { resource ->
            val imageRepo = resource.dockerImageRepo
                .takeIf { it != null && !DockerRegistryUtil.isInternal(it) }
                ?.let { ImageRepository.fromRepoString(it) }
            ApplicationDeployment.create(resource, imageRepo)
        }

    suspend fun application(applicationDeployment: ApplicationDeployment): Application? {
        val application = applicationService.getApplication(applicationDeployment.applicationId)
        return createApplicationEdge(application).node
    }
}

/*

@Component
class ApplicationDeploymentMutationResolver(
    private val applicationUpgradeService: ApplicationUpgradeService,
    private val applicationDeploymentService: ApplicationDeploymentService
) : GraphQLMutationResolver {

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
) : GraphQLResolver<ApplicationDeployment> {

    fun affiliation(applicationDeployment: ApplicationDeployment): Affiliation =
        Affiliation(applicationDeployment.affiliationId)

    fun namespace(applicationDeployment: ApplicationDeployment): Namespace =
        Namespace(applicationDeployment.namespaceId, applicationDeployment.affiliationId)

    fun details(applicationDeployment: ApplicationDeployment, dfe: DataFetchingEnvironment) =
        dfe.loader(ApplicationDeploymentDetailsDataLoader::class).load(
            applicationDeployment.id
        )

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
*/
