package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

/*
@Component
class ApplicationDeploymentDetailsDataLoader(
    private val applicationService: ApplicationServiceBlocking
) : KeyDataLoader<String, ApplicationDeploymentDetails> {
    override fun getByKey(user: User, key: String): Try<ApplicationDeploymentDetails> {
        return if (user.name == "ANONYMOUS_USER.username") { // FIXME compare anonymous user
            Try.failed<ApplicationDeploymentDetails>(AccessDeniedException("Anonymous user cannot get details"))
        } else {
            Try.tryCall {
                applicationService.getApplicationDeploymentDetails(user.token, key).let {
                    ApplicationDeploymentDetails.create(it)
                }
            }
        }
    }
}*/
