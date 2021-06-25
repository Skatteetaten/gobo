package no.skatteetaten.aurora.gobo.graphql.auroraconfig

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test

class ApplicationDeploymentSpecTest {

    @Test
    fun `Get permissions from deployment spec`() {
        val json = """{
                        "permissions": {
                            "admin": {
                              "source": "about.json",
                              "value": "test_1 test_2"
                            },
                            "view": {
                                "source": "test.json",
                                "value": "test_3"
                            }
                        }
                    }"""

        val jsonNode = jacksonObjectMapper().readTree(json)

        val permissions = ApplicationDeploymentSpec(jsonNode).permissions!!

        val firstPermission = permissions.first()
        assertThat(firstPermission.role).isEqualTo("admin")
        assertThat(firstPermission.subjects).isEqualTo(listOf("test_1", "test_2"))

        val secondPermission = permissions[1]
        assertThat(secondPermission.role).isEqualTo("view")
        assertThat(secondPermission.subjects).isEqualTo(listOf("test_3"))
    }

    @Test
    fun `Return null permissions when no permissions are available on deployment spec`() {
        val jsonNode = jacksonObjectMapper().readTree("{}")
        val permissions = ApplicationDeploymentSpec(jsonNode).permissions

        assertThat(permissions).isNull()
    }
}
