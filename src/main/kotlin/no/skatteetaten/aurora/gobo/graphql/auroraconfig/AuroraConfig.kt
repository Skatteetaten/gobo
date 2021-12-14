package no.skatteetaten.aurora.gobo.graphql.auroraconfig

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.fasterxml.jackson.databind.JsonNode
import com.jayway.jsonpath.JsonPath
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.runBlocking
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentRef
import no.skatteetaten.aurora.gobo.graphql.loadValue
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileType
import no.skatteetaten.aurora.gobo.security.checkValidUserToken
import java.util.concurrent.CompletableFuture

data class AuroraConfig(
    val name: String,
    val ref: String,
    val resolvedRef: String,
    val files: List<AuroraConfigFileResource>
) {

    suspend fun files(
        types: List<AuroraConfigFileType>? = null,
        fileNames: List<String>? = null,
        dfe: DataFetchingEnvironment
    ): List<AuroraConfigFileResource> {
        dfe.checkValidUserToken()
        return files.filter {
            types == null || types.contains(it.type)
        }.filter {
            fileNames == null || fileNames.contains(it.name)
        }
    }

    fun applicationDeploymentSpec(
        applicationDeploymentRefs: List<ApplicationDeploymentRef>,
        dfe: DataFetchingEnvironment
    ): CompletableFuture<List<ApplicationDeploymentSpec>> {
        runBlocking { dfe.checkValidUserToken() } // TODO bør fikses med @PreAuthorize?
        return dfe.loadValue(
            AdSpecKey(
                name,
                ref,
                applicationDeploymentRefs
            )
        )
    }
}

data class ApplicationDeploymentSpec(
    @GraphQLIgnore
    val rawJsonValueWithDefaults: JsonNode
) {
    val cluster: String? = rawJsonValueWithDefaults.text("/cluster/value")
    val envName: String? = rawJsonValueWithDefaults.text("/envName/value")
    val environment = rawJsonValueWithDefaults.expression("\$.envName.sources[?(@.name=='folderName')].value")
    val name: String? = rawJsonValueWithDefaults.text("/name/value")
    val version: String? = rawJsonValueWithDefaults.text("/version/value")
    val releaseTo = rawJsonValueWithDefaults.optionalText("/releaseTo/value")
    val application: String? = rawJsonValueWithDefaults.text("/name/value")
    val type: String? = rawJsonValueWithDefaults.text("/type/value")
    val deployStrategy: String? = rawJsonValueWithDefaults.text("/deployStrategy/type/value")
    val replicas = rawJsonValueWithDefaults.int("/replicas/value")
    val paused = rawJsonValueWithDefaults.boolean("/pause/value")
    val affiliation: String? = rawJsonValueWithDefaults.text("/affiliation/value")

    private fun JsonNode.expression(path: String) =
        runCatching { JsonPath.read<List<String>>(this.toString(), path).first() }.getOrNull()

    private fun JsonNode.optionalText(path: String) = this.at(path)?.textValue()
    private fun JsonNode.text(path: String) = this.at(path).textValue()
    private fun JsonNode.boolean(path: String) = this.at(path).booleanValue()
    private fun JsonNode.int(path: String) = this.at(path).intValue()
}

data class AuroraConfigFileResource(
    val name: String,
    val contents: String,
    val type: AuroraConfigFileType,
    val contentHash: String
)

data class NewAuroraConfigFileInput(
    val auroraConfigName: String,
    val auroraConfigReference: String?,
    val fileName: String,
    val contents: String
)

data class UpdateAuroraConfigFileInput(
    val auroraConfigName: String,
    val auroraConfigReference: String?,
    val fileName: String,
    val contents: String,
    val existingHash: String
)

data class AuroraConfigFileValidationResponse(
    val message: String?,
    val success: Boolean,
    val file: AuroraConfigFileResource? = null
)
