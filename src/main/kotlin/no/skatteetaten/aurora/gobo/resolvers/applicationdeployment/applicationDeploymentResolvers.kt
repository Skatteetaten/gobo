package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

/*
@Component
class ApplicationDeploymentQueryResolver(
    private val applicationService: ApplicationServiceBlocking,
    private val dockerRegistry: DockerRegistry
) : GraphQLQueryResolver {

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
