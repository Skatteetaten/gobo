package no.skatteetaten.aurora.gobo

import no.skatteetaten.aurora.gobo.graphql.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.graphql.application.Certificate
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeployment
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.Status
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.Version
import no.skatteetaten.aurora.gobo.graphql.auroraconfig.AuroraConfigFileResource
import no.skatteetaten.aurora.gobo.graphql.cname.CnameAzure
import no.skatteetaten.aurora.gobo.graphql.cname.CnameEntry
import no.skatteetaten.aurora.gobo.graphql.cname.CnameInfo
import no.skatteetaten.aurora.gobo.graphql.database.JdbcUser
import no.skatteetaten.aurora.gobo.graphql.imagerepository.ImageRepository
import no.skatteetaten.aurora.gobo.graphql.imagerepository.ImageTag
import no.skatteetaten.aurora.gobo.graphql.namespace.Namespace
import no.skatteetaten.aurora.gobo.graphql.webseal.Acl
import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentFilterResource
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileType
import no.skatteetaten.aurora.gobo.integration.boober.BooberEnvironmentDeploymentRef
import no.skatteetaten.aurora.gobo.integration.boober.BooberEnvironmentResource
import no.skatteetaten.aurora.gobo.integration.boober.BooberVault
import no.skatteetaten.aurora.gobo.integration.cantus.AuroraResponse
import no.skatteetaten.aurora.gobo.integration.cantus.CantusFailure
import no.skatteetaten.aurora.gobo.integration.cantus.ImageBuildTimeline
import no.skatteetaten.aurora.gobo.integration.cantus.ImageTagResource
import no.skatteetaten.aurora.gobo.integration.cantus.JavaImage
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseInstanceResource
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseMetadataResource
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseSchemaResource
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseUserResource
import no.skatteetaten.aurora.gobo.integration.dbh.RestorableDatabaseSchemaResource
import no.skatteetaten.aurora.gobo.integration.dbh.SchemaCreationRequest
import no.skatteetaten.aurora.gobo.integration.dbh.SchemaDeletionRequest
import no.skatteetaten.aurora.gobo.integration.dbh.SchemaRestorationRequest
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
import no.skatteetaten.aurora.gobo.integration.mokey.addAll
import no.skatteetaten.aurora.gobo.integration.phil.DeletionResource
import no.skatteetaten.aurora.gobo.integration.phil.DeploymentRefResource
import no.skatteetaten.aurora.gobo.integration.phil.DeploymentResource
import no.skatteetaten.aurora.gobo.integration.phil.DeploymentStatus
import no.skatteetaten.aurora.gobo.integration.skap.SkapJob
import no.skatteetaten.aurora.gobo.integration.skap.WebsealStateResource
import no.skatteetaten.aurora.gobo.integration.unclematt.ProbeResult
import no.skatteetaten.aurora.gobo.integration.unclematt.ProbeStatus
import no.skatteetaten.aurora.gobo.integration.unclematt.Result
import org.intellij.lang.annotations.Language
import org.springframework.http.HttpStatus
import uk.q3c.rest.hal.HalLink
import uk.q3c.rest.hal.HalResource
import uk.q3c.rest.hal.Links
import java.time.Instant
import java.time.LocalDateTime
import java.util.Date
import no.skatteetaten.aurora.gobo.integration.herkimer.ResourceHerkimer
import no.skatteetaten.aurora.gobo.integration.herkimer.ResourceKind
import no.skatteetaten.aurora.gobo.integration.mokey.StoragegridObjectAreaResource

val defaultInstant: Instant = Instant.parse("2018-01-01T00:00:01Z")

@Language("JSON")
val linksResponseJson: String =
    """{
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
val infoResponseJson: String =
    """{
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
val envResponseJson =
    """{
  "activeProfiles": [
    "myprofile"
  ]
}"""

@Language("JSON")
val healthResponseJson: String =
    """{"status": "UP"}"""

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
    val environment: String = "environment",
    val namespace: String = "namespace",
    val name: String = "name",
    val id: String = "id",
    val status: String = "HEALTHY",
    val msg: String = "foo"
) {
    fun build(): ApplicationDeploymentResource =
        ApplicationDeploymentResource(
            identifier = id,
            name = name,
            affiliation = affiliation,
            environment = environment,
            namespace = namespace,
            status = StatusResource(status, "", listOf(), listOf()),
            version = VersionResource("deployTag", "auroraVersion", "releaseTo"),
            dockerImageRepo = "127.0.0.1:5000/aurora/whoami",
            time = Instant.EPOCH,
            message = msg
        ).apply {
            link("ApplicationDeploymentDetails", HalLink("http://ApplicationDeploymentDetails/1"))
            link("Application", HalLink("http://Application/1"))
        }
}

data class MultiAffiliationResponseBuilder(
    private val environment: String = "utv",
    private val application: String = "gobo",
    private val errorMessage: String? = null
) {
    fun build() = BooberEnvironmentResource(
        affiliation = "aurora",
        applicationDeploymentRef = BooberEnvironmentDeploymentRef(environment, application),
        errorMessage = errorMessage,
        warningMessage = null,
        autoDeploy = true
    )
}

data class ApplicationResourceBuilder(
    val name: String = "name",
    val affiliation: String = "paas",
    val namespace: String = "namespace",
    val applicationDeployments: List<ApplicationDeploymentResource> = listOf(
        ApplicationDeploymentResourceBuilder(
            affiliation = affiliation,
            namespace = namespace
        ).build()
    )
) {
    fun build(): ApplicationResource =
        ApplicationResource(
            identifier = "id",
            name = name,
            applicationDeployments = applicationDeployments
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
            affiliation = Affiliation(affiliation),
            environment = "environment",
            namespace = Namespace("namespaceId", Affiliation(affiliation)),
            status = Status("code", "comment", listOf(), listOf()),
            version = Version(ImageTag(ImageRepository("", "", ""), "deployTag"), "auroraVersion", "releaseTo"),
            dockerImageRepo = "dockerImageRepo",
            time = defaultInstant,
            applicationId = "appId",
            message = message,
            imageRepository = ImageRepository.fromRepoString("docker.registry/group/name")
        )
}

data class ApplicationDeploymentDetailsBuilder(
    val resourceLinks: Links = Links(),
    val pause: Boolean = false
) {

    fun build() =
        ApplicationDeploymentDetailsResource(
            updatedBy = "linus",
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
                    containers = ContainerResourceListBuilder().build(),
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
            ),
            serviceLinks = Links().apply { add("metrics", "http://metrics") }
        ).apply {
            self("http://ApplicationDeploymentDetails/1")
            addAll(resourceLinks)
        }
}

data class ApplicationDeploymentDetailsResourceBuilder(
    val resourceLinks: Links = Links(),
    val pause: Boolean = false
) {

    fun build() =
        ApplicationDeploymentDetailsResource(
            updatedBy = "linus",
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
                    containers = ContainerResourceListBuilder().build(),
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
            ),
            serviceLinks = Links().apply { add("metrics", "http://metrics") }
        ).apply {
            self("http://ApplicationDeploymentDetails/1")
            addAll(resourceLinks)
        }
}

class ContainerResourceListBuilder {
    fun build() = listOf(
        ContainerResource(
            name = "name-java",
            image = "docker-registry/group/name@sha256:hash",
            state = "running",
            restartCount = 1,
            ready = true
        ),
        ContainerResource(
            name = "name-foo-toxiproxy-sidecar",
            image = "docker-registry/group/name@sha256:hash",
            state = "running",
            restartCount = 2,
            ready = false
        ),
        ContainerResource(
            name = "name-foo",
            image = "docker-registry/group/name@sha256:hash",
            state = "running",
            restartCount = 2,
            ready = false
        )
    )
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
            name = "test/gobo.yaml",
            contents = "contents",
            type = AuroraConfigFileType.APP,
            contentHash = "12345"
        )
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

data class DatabaseInstanceResourceBuilder(val affiliation: String = "paas") {
    fun build() =
        DatabaseInstanceResource(
            engine = "POSTGRES",
            instanceName = "name",
            host = "host",
            port = 8080,
            createSchemaAllowed = true,
            labels = mapOf("affiliation" to affiliation)
        )
}

data class DatabaseSchemaResourceBuilder(
    val createdDate: Long = Instant.now().toEpochMilli(),
    val lastUsedDate: Long? = Instant.now().toEpochMilli(),
    val name: String = "name",
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
            name = name,
            createdDate = createdDate,
            lastUsedDate = lastUsedDate,
            databaseInstance = DatabaseInstanceResourceBuilder().build(),
            users = listOf(DatabaseUserResource("username", "password", "SCHEMA")),
            metadata = DatabaseMetadataResource(sizeInMb = 0.25),
            labels = labels
        )
}

data class RestorableDatabaseSchemaBuilder(
    val setToCooldownAt: Long = Instant.now().toEpochMilli(),
    val deleteAfter: Long = Instant.now().toEpochMilli(),
    val databaseSchema: DatabaseSchemaResource = DatabaseSchemaResourceBuilder().build()
) {
    fun build() = RestorableDatabaseSchemaResource(databaseSchema, setToCooldownAt, deleteAfter)
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
            labels,
            JdbcUser(username = "username", password = "pass", jdbcUrl = "url"),
            "ORACLE",
            null
        )
}

data class SchemaDeletionRequestBuilder(val id: String = "123", val cooldownDurationHours: Long? = null) {

    fun build() =
        SchemaDeletionRequest(id, cooldownDurationHours)
}

data class SchemaRestorationRequestBuilder(val id: String = "123", val active: Boolean = true) {

    fun build() =
        SchemaRestorationRequest(id, active)
}

class JdbcUserBuilder {

    fun build() = JdbcUser(username = "abc123", password = "pass", jdbcUrl = "url")
}

data class AuroraResponseBuilder(val status: Int, val url: String) {
    val statusCode: HttpStatus
        get() = HttpStatus.valueOf(status)

    fun <T : HalResource> build(): AuroraResponse<T> {
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

data class HerkimerResourceBuilder(val id: String) {
    fun build() = ResourceHerkimer(
        id = id,
        name = "resource",
        kind = ResourceKind.StorageGridTenant,
        ownerId = "owner",
        claims = emptyList(),
        active = true,
        setToCooldownAt = null,
        createdBy = "aurora",
        modifiedBy = "aurora",
        createdDate = LocalDateTime.now(),
        modifiedDate = LocalDateTime.now()
    )
}

data class StoragegridObjectAreaResourceBuilder(val namespace: String) {
    fun build() = StoragegridObjectAreaResource(
        name = "some-area",
        namespace = namespace,
        creationTimestamp = "today",
        objectArea = "area",
        bucketName = "$namespace-utv04-default",
        message = "msg",
        reason = "reason",
        success = true
    )
}

data class WebsealStateResourceBuilder(val namespace: String = "namespace") {

    fun build() = WebsealStateResource(
        acl = Acl("acl-name", true, true, emptyList()),
        name = "test.no",
        namespace = namespace,
        routeName = "test-route",
        junctions = listOf(
            mapOf(
                "Active Worker Threads" to "activeWorkerThreads1",
                "Allow Windows Style URLs" to "allowWindowsStyleURLs"
            ),
            mapOf(
                "Active Worker Threads" to "activeWorkerThreads2"
            )
        )
    )
}

data class SkapJobForWebsealBuilder(val namespace: String = "namespace", val name: String = "name") {

    fun build() =
        SkapJob(
            id = "75745",
            payload = "{\"cluster\":\"target.utv.paas.skead.no\",\"roles\":[],\"host\":\"testing.test.no\",\"namespace\":\"testing-utv\",\"routeName\":\"test-webseal\"}",
            type = "websealv2",
            operation = "CREATEORUPDATE",
            status = "DONE",
            updated = "2019-10-17T09:17:15.547788+02:00",
            errorMessage = null
        )
}

data class SkapJobForBigipBuilder(val namespace: String = "namespace", val name: String = "name") {

    fun build() =
        SkapJob(
            id = "465774",
            payload = "{\"asmPolicy\":\"testing-get\",\"externalHost\":\"testingtesting.no\",\"apiPaths\":[\"/testing/api/\"],\"oauthScopes\":[\"test1\",\"test2\",\"test3\"],\"hostname\":\"testing.no\",\"namespace\":\"paas-dev\",\"name\":\"testing\",\"clusterEnvironment\":\"utvikling\",\"clusterType\":\"MAIN\",\"affiliation\":\"folk\",\"serviceName\":\"testing\"}",
            type = "bigipexternal",
            operation = "CREATEORUPDATE",
            status = "DONE",
            updated = "2019-10-17T09:17:15.547788+02:00",
            errorMessage = null
        )
}

class ImageTagResourceBuilder {
    fun build() = ImageTagResource(
        auroraVersion = "1",
        appVersion = "1",
        timeline = ImageBuildTimeline(Instant.now(), Instant.now()),
        dockerVersion = "1",
        dockerDigest = "abc123",
        java = JavaImage("1", "1", "1", ""),
        requestUrl = "http://localhost"
    )
}

data class BooberVaultBuilder(
    val permissions: List<String> = listOf("APP_PaaS_utv"),
    val secrets: Map<String, String> = mapOf("latest.properties" to "QVRTX1VTRVJOQU1FPWJtYwp")
) {
    fun build() = BooberVault(
        name = "test-vault",
        hasAccess = true,
        permissions = permissions,
        secrets = secrets
    )
}

data class CnameInfoBuilder(val namespace: String = "aurora-demo") {
    fun build() = CnameInfo(
        status = "SUCCESS",
        clusterId = "utv",
        appName = "demo",
        namespace = namespace,
        routeName = "demo",
        message = "",
        entry = CnameEntry(cname = "demo.localhost.no", host = "host1", ttl = 300)
    )
}

data class CnameAzureBuilder(val namespace: String = "aurora-demo") {
    fun build() = CnameAzure(
        canonicalName = "demo.localhost.no",
        ttlInSeconds = 3098,
        namespace = namespace,
        clusterId = "utv",
        ownerObjectName = "demo"
    )
}

class DeploymentResourceBuilder {
    fun build(environment: String = "dev-utv") = DeploymentResource(
        deploymentRef = DeploymentRefResource("utv", "aurora", environment, "gobo"),
        timestamp = Date(),
        message = "",
        status = DeploymentStatus.APPLIED
    )
}

class DeletionResourceBuilder {
    fun build() = DeletionResource(
        deploymentRef = DeploymentRefResource("utv", "aurora", "dev-utv", "gobo"),
        timestamp = Date(),
        message = "",
        deleted = true
    )
}
