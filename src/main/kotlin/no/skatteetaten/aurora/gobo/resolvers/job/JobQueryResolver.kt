package no.skatteetaten.aurora.gobo.resolvers.job

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.skap.Job
import no.skatteetaten.aurora.gobo.integration.skap.JobService
import no.skatteetaten.aurora.gobo.resolvers.AccessDeniedException
import no.skatteetaten.aurora.gobo.security.isAnonymousUser
import org.springframework.stereotype.Component

@Component
class JobQueryResolver(
    val jobService: JobService
) : GraphQLQueryResolver {

    fun jobs(
        namespace: String,
        name: String,
        dfe: DataFetchingEnvironment
    ): List<Job> {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get WebSEAL/BipIp jobs")
        return jobService.getJobs(namespace, name)
    }
}
