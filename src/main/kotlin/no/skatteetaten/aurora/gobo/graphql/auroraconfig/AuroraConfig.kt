package no.skatteetaten.aurora.gobo.graphql.auroraconfig

import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.fasterxml.jackson.databind.JsonNode
import com.jayway.jsonpath.JsonPath
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentRef
import no.skatteetaten.aurora.gobo.graphql.loadMany
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileType
import no.skatteetaten.aurora.gobo.security.checkValidUserToken

data class AuroraConfig(
    val name: String,
    val ref: String,
    val resolvedRef: String,
    val files: List<AuroraConfigFileResource>
) {

    suspend fun files(
        types: List<AuroraConfigFileType>?,
        fileNames: List<String>?,
        dfe: DataFetchingEnvironment
    ): List<AuroraConfigFileResource> {
        dfe.checkValidUserToken()
        return files.filter {
            types == null || types.contains(it.type)
        }.filter {
            fileNames == null || fileNames.contains(it.name)
        }
    }

    suspend fun applicationDeploymentSpec(
        applicationDeploymentRefs: List<ApplicationDeploymentRef>,
        dfe: DataFetchingEnvironment
    ): List<ApplicationDeploymentSpec> {
        dfe.checkValidUserToken()
        return dfe.loadMany(
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
    val cluster = rawJsonValueWithDefaults.text("/cluster/value")
    val envName = rawJsonValueWithDefaults.text("/envName/value")
    val environment = rawJsonValueWithDefaults.expression("\$.envName.sources[?(@.name=='folderName')].value")
    val name = rawJsonValueWithDefaults.text("/name/value")
    val version = rawJsonValueWithDefaults.text("/version/value")
    val releaseTo = rawJsonValueWithDefaults.optionalText("/releaseTo/value")
    val application = rawJsonValueWithDefaults.text("/name/value")
    val type = rawJsonValueWithDefaults.text("/type/value")
    val deployStrategy = rawJsonValueWithDefaults.text("/deployStrategy/type/value")
    val replicas = rawJsonValueWithDefaults.int("/replicas/value")
    val paused = rawJsonValueWithDefaults.boolean("/pause/value")
    val affiliation = rawJsonValueWithDefaults.text("/affiliation/value")

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
