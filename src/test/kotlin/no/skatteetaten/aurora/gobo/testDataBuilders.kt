package no.skatteetaten.aurora.gobo

import io.fabric8.kubernetes.api.model.ObjectMeta
import io.fabric8.openshift.api.model.User
import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentFilterResource
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileResource
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileType
import no.skatteetaten.aurora.gobo.integration.cantus.AuroraResponse
import no.skatteetaten.aurora.gobo.integration.cantus.CantusFailure
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseInstanceResource
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseMetadataResource
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseSchemaResource
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseUserResource
import no.skatteetaten.aurora.gobo.integration.dbh.JdbcUser
import no.skatteetaten.aurora.gobo.integration.dbh.SchemaCreationRequest
import no.skatteetaten.aurora.gobo.integration.dbh.SchemaDeletionRequest
import no.skatteetaten.aurora.gobo.integration.dbh.SchemaUpdateRequest
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentCommandResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentDetailsResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentRefResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentWithDbResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationResource
import no.skatteetaten.aurora.gobo.integration.mokey.AuroraConfigRefResource
import no.skatteetaten.aurora.gobo.integration.mokey.ContainerResource
import no.skatteetaten.aurora.gobo.integration.mokey.DeployDetailsResource
import no.skatteetaten.aurora.gobo.integration.mokey.GitInfoResource
import no.skatteetaten.aurora.gobo.integration.mokey.ImageDetailsResource
import no.skatteetaten.aurora.gobo.integration.mokey.ManagementEndpointResponseResource
import no.skatteetaten.aurora.gobo.integration.mokey.ManagementResponsesResource
import no.skatteetaten.aurora.gobo.integration.mokey.PodResourceResource
import no.skatteetaten.aurora.gobo.integration.mokey.StatusResource
import no.skatteetaten.aurora.gobo.integration.mokey.VersionResource
import no.skatteetaten.aurora.gobo.integration.skap.Acl
import no.skatteetaten.aurora.gobo.integration.skap.Certificate
import no.skatteetaten.aurora.gobo.integration.skap.Junction
import no.skatteetaten.aurora.gobo.integration.skap.WebsealState
import no.skatteetaten.aurora.gobo.integration.unclematt.ProbeResult
import no.skatteetaten.aurora.gobo.integration.unclematt.ProbeStatus
import no.skatteetaten.aurora.gobo.integration.unclematt.Result
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeployment
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.Status
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.Version
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageTag
import org.intellij.lang.annotations.Language
import org.springframework.hateoas.Link
import org.springframework.http.HttpStatus
import uk.q3c.rest.hal.HalResource
import java.time.Instant

val defaultInstant = Instant.parse("2018-01-01T00:00:01Z")

@Language("JSON")
val linksResponseJson: String = """{
  "_links": {
      "self": {
          "href": "http://localhost:8081/actuator"
        },
      "health": {
          "href": "http://localhost:8081/health"
        },
      "info": {
          "href": "http://localhost:8081/info"
        },
      "env": {
          "href": "http://localhost:8081/env"
      }
  }
}"""

@Language("JSON")
val infoResponseJson: String = """{
  "git": {
    "build.time": "$defaultInstant",
    "commit.time": "$defaultInstant",
    "commit.id.abbrev": ""
  },
 "podLinks": {
    "metrics": "http://localhost"
  }
}"""

@Language("JSON")
val envResponseJson = """{
  "activeProfiles": [
    "myprofile"
  ]
}"""

@Language("JSON")
val healthResponseJson: String = """{"status": "UP"}"""

data class ApplicationDeploymentWithDbResourceBuilder(
    val databaseId: String,
    val applicationDeployments: List<ApplicationDeploymentResource>? = null
) {
    fun build(): ApplicationDeploymentWithDbResource =
        ApplicationDeploymentWithDbResource(
            identifier = databaseId,
            applicationDeployments = applicationDeployments ?: listOf(
                ApplicationDeploymentResourceBuilder().build()
            )
        )
}

data class ApplicationDeploymentResourceBuilder(
    val affiliation: String = "paas",
    val id: String = "id",
    val msg: String = "foo"
) {
    fun build(): ApplicationDeploymentResource =
        ApplicationDeploymentResource(
            identifier = id,
            name = "name",
            affiliation = affiliation,
            environment = "environment",
            namespace = "namespace",
            status = StatusResource("code", "", listOf(), listOf()),
            version = VersionResource("deployTag", "auroraVersion", "releaseTo"),
            dockerImageRepo = "127.0.0.1:5000/aurora/whoami",
            time = Instant.EPOCH,
            message = msg
        ).apply {
            add(Link("http://ApplicationDeploymentDetails/1", "ApplicationDeploymentDetails"))
            add(Link("http://Application/1", "Application"))
        }
}

data class ApplicationResourceBuilder(val name: String = "name") {

    fun build(): ApplicationResource =
        ApplicationResource(
            identifier = "id",
            name = name,
            applicationDeployments = listOf(ApplicationDeploymentResourceBuilder().build())
        )
}

data class ApplicationDeploymentBuilder(
    val affiliation: String = "paas",
    val message: String? = null
) {

    fun build(): ApplicationDeployment =
        ApplicationDeployment(
            id = "id",
            name = "name",
            affiliationId = affiliation,
            environment = "environment",
            namespaceId = "namespaceId",
            status = Status("code", "comment", listOf(), listOf()),
            version = Version(ImageTag(ImageRepository("", "", ""), "deployTag"), "auroraVersion", "releaseTo"),
            dockerImageRepo = "dockerImageRepo",
            time = defaultInstant,
            applicationId = "appId",
            message = message
        )
}

data class ApplicationDeploymentDetailsBuilder(
    val resourceLinks: List<Link> = emptyList(),
    val pause: Boolean = false
) {

    fun build() =
        ApplicationDeploymentDetailsResource(
            buildTime = Instant.now(),
            gitInfo = GitInfoResource("123abc", Instant.now()),
            imageDetails = ImageDetailsResource(
                imageBuildTime = defaultInstant,
                dockerImageReference = "docker.registry/group/name@sha256:123",
                dockerImageTagReference = "docker.registry/group/name:2"
            ),
            deployDetails = DeployDetailsResource(
                targetReplicas = 1,
                availableReplicas = 1,
                deployment = "deployment-1",
                phase = "Complete",
                deployTag = "foobar",
                paused = pause
            ),
            databases = listOf("123"),
            podResources = listOf(
                PodResourceResource(
                    name = "name",
                    phase = "status",
                    deployTag = "tag",
                    latestDeployTag = true,
                    replicaName = "deployment-1",
                    latestReplicaName = true,
                    containers = listOf(
                        ContainerResource(
                            name = "name-java",
                            image = "docker-registry/group/name@sha256:hash",
                            state = "running",
                            restartCount = 1,
                            ready = true
                        ),
                        ContainerResource(
                            name = "name-foo",
                            image = "docker-registry/group/name@sha256:hash",
                            state = "running",
                            restartCount = 2,
                            ready = false
                        )
                    ),
                    startTime = Instant.now(),

                    managementResponses = ManagementResponsesResource(
                        links = ManagementEndpointResponseResource(
                            hasResponse = true,
                            textResponse = linksResponseJson,
                            httpCode = 200,
                            createdAt = defaultInstant,
                            url = "http://localhost/discovery"
                        ),
                        health = ManagementEndpointResponseResource(
                            hasResponse = true,
                            textResponse = healthResponseJson,
                            httpCode = 200,
                            createdAt = defaultInstant,
                            url = "http://localhost/health"
                        ),
                        info = ManagementEndpointResponseResource(
                            hasResponse = true,
                            textResponse = infoResponseJson,
                            httpCode = 200,
                            createdAt = defaultInstant,
                            url = "http://localhost/info"
                        ),
                        env = ManagementEndpointResponseResource(
                            hasResponse = true,
                            textResponse = envResponseJson,
                            httpCode = 200,
                            createdAt = defaultInstant,
                            url = "http://localhost/env"
                        )
                    )

                )
            ),
            applicationDeploymentCommand = ApplicationDeploymentCommandResource(
                emptyMap(),
                ApplicationDeploymentRefResource("environment", "application"),
                AuroraConfigRefResource("name", "refName")
            )
        ).apply {
            add(Link("http://ApplicationDeploymentDetails/1", "self"))
            add(resourceLinks)
        }
}

class ProbeResultListBuilder {
    fun build() = listOf(
        ProbeResult(
            result = Result(
                status = ProbeStatus.OPEN,
                message = "Firewall open",
                dnsname = "test.server",
                resolvedIp = "192.168.1.1",
                port = "80"
            ),
            podIp = "192.168.10.1",
            hostIp = "192.168.100.1"
        ),
        ProbeResult(
            result = Result(
                status = ProbeStatus.CLOSED,
                message = "Firewall closed",
                dnsname = "test.server",
                resolvedIp = "192.168.1.2",
                port = "80"
            ),
            podIp = "192.168.10.2",
            hostIp = "192.168.100.2"
        ),
        ProbeResult(
            result = Result(
                status = ProbeStatus.UNKNOWN,
                message = "Unknown status",
                dnsname = "test.server",
                resolvedIp = "192.168.1.3",
                port = "80"
            ),
            podIp = "192.168.10.3",
            hostIp = "192.168.100.3"
        )
    )
}

class AuroraConfigFileBuilder {

    fun build() =
        AuroraConfigFileResource(
            name = "name",
            contents = "contents",
            type = AuroraConfigFileType.APP
        )
}

data class OpenShiftUserBuilder(val userName: String = "123456", val fullName: String = "Test Testesen") {

    fun build(): User {
        val objectMeta = ObjectMeta()
        objectMeta.name = userName

        val user = io.fabric8.openshift.api.model.User()
        user.fullName = fullName
        user.metadata = objectMeta
        return user
    }
}

data class ApplicationDeploymentFilterResourceBuilder(val affiliation: String = "aurora") {

    fun build() =
        ApplicationDeploymentFilterResource(
            name = "name",
            default = false,
            affiliation = affiliation,
            applications = listOf("app1", "app2"),
            environments = listOf("env1", "env2")
        )
}

data class DatabaseSchemaResourceBuilder(
    val createdDate: Long = Instant.now().toEpochMilli(),
    val lastUsedDate: Long? = Instant.now().toEpochMilli(),
    val labels: Map<String, String> = mapOf(
        "affiliation" to "aurora",
        "userId" to "abc123",
        "name" to "referanse",
        "description" to "my database schema",
        "environment" to "test",
        "application" to "referanse"
    )
) {

    fun build() =
        DatabaseSchemaResource(
            id = "123",
            type = "MANAGED",
            jdbcUrl = "jdbc:oracle:thin:@localhost:1521/db",
            name = "name",
            createdDate = createdDate,
            lastUsedDate = lastUsedDate,
            databaseInstance = DatabaseInstanceResource(engine = "ORACLE"),
            users = listOf(DatabaseUserResource("username", "password", "SCHEMA")),
            metadata = DatabaseMetadataResource(sizeInMb = 0.25),
            labels = labels
        )
}

data class SchemaUpdateRequestBuilder(val id: String = "123", val jdbcUser: JdbcUser? = null) {

    fun build() =
        SchemaUpdateRequest(id, emptyMap(), null)
}

data class SchemaCreationRequestBuilder(
    val id: String = "123",
    val labels: Map<String, String> = mapOf(
        "affiliation" to "paas",
        "name" to "ref-db",
        "environment" to "test",
        "application" to "referanse"
    )
) {

    fun build() =
        SchemaCreationRequest(
            labels, JdbcUser(username = "username", password = "pass", jdbcUrl = "url")
        )
}

data class SchemaDeletionRequestBuilder(val id: String = "123", val cooldownDurationHours: Long? = null) {

    fun build() =
        SchemaDeletionRequest(id, cooldownDurationHours)
}

class JdbcUserBuilder {

    fun build() = JdbcUser(username = "abc123", password = "pass", jdbcUrl = "url")
}

data class AuroraResponseBuilder(val status: Int, val url: String) {
    val statusCode: HttpStatus
        get() = HttpStatus.valueOf(status)

    fun build(): AuroraResponse<HalResource> {
        val statusMessage = when {
            statusCode.is4xxClientError -> {
                when (statusCode.value()) {
                    404 -> "Resource could not be found"
                    400 -> "Invalid request"
                    403 -> "Forbidden"
                    else -> "There has occurred a client error"
                }
            }
            statusCode.is5xxServerError -> {
                when (statusCode.value()) {
                    500 -> "An internal server error has occurred in the docker registry"
                    else -> "A server error has occurred"
                }
            }

            else ->
                "Unknown error occurred"
        }

        val errorMessage = "$statusMessage status=${statusCode.value()} message=${statusCode.reasonPhrase}"

        val cantusFailure = CantusFailure(
            url = url,
            errorMessage = errorMessage
        )

        return AuroraResponse(
            failure = listOf(cantusFailure),
            message = errorMessage
        )
    }
}

data class CertificateResourceBuilder(val id: String = "1", val dn: String = ".activemq") {

    fun build() = Certificate(
        id = id,
        dn = dn,
        issuedDate = Instant.now(),
        revokedDate = Instant.now(),
        expiresDate = Instant.now()
    )
}

data class WebsealStateBuilder(val namespace: String = "test") {

    fun build() = WebsealState(
        acl = Acl("acl-name", true, true, emptyList()),
        name = "test.no",
        namespace = namespace,
        routeName = "test-route",
        junctions = listOf(
            Junction(
                activeWorkerThreads = "activeWorkerThreads",
                allowWindowsStyleURLs = "allowWindowsStyleURLs",
                authenticationHTTPheader = "authenticationHTTPheader",
                basicAuthenticationMode = "basicAuthenticationMode",
                booleanRuleHeader = "booleanRuleHeader",
                caseInsensitiveURLs = "caseInsensitiveURLs",
                currentRequests = "currentRequests",
                delegationSupport = "delegationSupport",
                formsBasedSSO = "formsBasedSSO",
                hostname = "hostname",
                id = "junction-id",
                insertWebSEALSessionCookies = "insertWebSEALSessionCookies",
                insertWebSphereLTPACookies = "insertWebSphereLTPACookies",
                junctionHardLimit = "junctionHardLimit",
                junctionSoftLimit = "junctionSoftLimit",
                mutuallyAuthenticated = "mutuallyAuthenticated",
                operationalState = "operationalState",
                port = "port",
                queryContents = "queryContents",
                queryContentsURL = "queryContentsURL",
                remoteAddressHTTPHeader = "remoteAddressHTTPHeader",
                requestEncoding = "requestEncoding",
                server1 = "server1",
                serverDN = "serverDN",
                serverState = "serverState",
                statefulJunction = "statefulJunction",
                tfimjunctionSSO = "TFIMJunctionSSO",
                totalRequests = "totalRequests",
                type = "type",
                virtualHostJunctionLabel = "virtualHostJunctionLabel",
                virtualHostname = "virtualHostname",
                localIPAddress = "localIPAddress"
            )
        )
    )
}
