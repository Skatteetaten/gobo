package no.skatteetaten.aurora.gobo.graphql.auroraconfig

import com.fasterxml.jackson.databind.JsonNode
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentRef
import no.skatteetaten.aurora.gobo.graphql.loadMany
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileType

data class AuroraConfig(
    val name: String,
    val ref: String,
    val resolvedRef: String,
    val files: List<AuroraConfigFileResource>
) {

    // FIXME no anonymous access
    fun files(
        types: List<AuroraConfigFileType>?,
        fileNames: List<String>?
    ): List<AuroraConfigFileResource> {
        return files.filter {
            types == null || types.contains(it.type)
        }.filter {
            fileNames == null || fileNames.contains(it.name)
        }
    }

    // FIXME no anonymous access
    suspend fun applicationDeploymentSpec(
        applicationDeploymentRefs: List<ApplicationDeploymentRef>,
        dfe: DataFetchingEnvironment
    ): List<ApplicationDeploymentSpec> {
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
    val rawJsonValueWithDefaults: JsonNode
) {
    val cluster = rawJsonValueWithDefaults.at("/cluster/value").textValue()
    val environment = rawJsonValueWithDefaults.at("/envName/value").textValue()
    val name = rawJsonValueWithDefaults.at("/name/value").textValue()
    val version = rawJsonValueWithDefaults.at("/version/value").textValue()
    val releaseTo: String? = rawJsonValueWithDefaults.at("/releaseTo/value").textValue()
    val application = rawJsonValueWithDefaults.at("/name/value").textValue()
    val type = rawJsonValueWithDefaults.at("/type/value").textValue()
    val deployStrategy = rawJsonValueWithDefaults.at("/deployStrategy/type/value").textValue()
    val replicas = rawJsonValueWithDefaults.at("/replicas/value").intValue().toString()
    val paused = rawJsonValueWithDefaults.at("/pause/value").booleanValue()
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
