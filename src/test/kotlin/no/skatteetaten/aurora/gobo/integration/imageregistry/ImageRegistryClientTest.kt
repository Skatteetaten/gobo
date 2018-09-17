package no.skatteetaten.aurora.gobo.integration.imageregistry

import assertk.assert
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import ru.lanwen.wiremock.ext.WiremockResolver
import ru.lanwen.wiremock.ext.WiremockResolver.Wiremock
import ru.lanwen.wiremock.ext.WiremockUriResolver
import ru.lanwen.wiremock.ext.WiremockUriResolver.WiremockUri

@ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
class ImageRegistryClientTest {

    val tokenProvider = mockk<TokenProvider>()

    val service = ImageRegistryClient(WebClient.create(), tokenProvider)

    private val tagsListResponse = """{
   "name": "no_skatteetaten_aurora/boober",
   "tags": [
     "master-SNAPSHOT",
     "1.0.0-rc.1-b2.2.3-oracle8-1.4.0",
     "1.0.0-rc.2-b2.2.3-oracle8-1.4.0",
     "develop-SNAPSHOT",
     "1"
   ]
 }"""

    @Test
    fun `fetch taglist`(@Wiremock server: WireMockServer, @WiremockUri uri: String) {

        every { tokenProvider.token } returns "token"

        server.stubFor(
            get(urlEqualTo("/taglist"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)

                        .withBody(tagsListResponse)
                )
        )
        val manifestUri = "$uri/taglist"
        val tags = service.getTags(manifestUri, AuthenticationMethod.KUBERNETES_TOKEN)
        assert(tags?.name).isEqualTo("no_skatteetaten_aurora/boober")
        assert(tags?.tags?.size).isEqualTo(5)


    }

    @Test
    fun `fetch manifest`(@Wiremock server: WireMockServer, @WiremockUri uri: String) {

        val expectedBody = "Hello world!"
        server.stubFor(
            get(urlEqualTo("/manifest"))
                .willReturn(
                    aResponse()
                        .withBody(expectedBody)
                )
        )
        val manifestUri = "$uri/manifest"
        val manifest = service.getManifest(manifestUri, AuthenticationMethod.NONE)

        assert(manifest).isEqualTo(expectedBody)
    }

    @Test
    fun `assert auth token if kubernetes registy`(@Wiremock server: WireMockServer, @WiremockUri uri: String) {

        every { tokenProvider.token } returns "token"

        val expectedBody = "Hello world!"
        server.stubFor(
            get(urlEqualTo("/manifest"))
                .withHeader("Authorization", matching("Bearer token"))
                .willReturn(
                    aResponse()
                        .withBody(expectedBody)
                )
        )
        val manifestUri = "$uri/manifest"
        val manifest = service.getManifest(manifestUri, AuthenticationMethod.KUBERNETES_TOKEN)

        assert(manifest).isEqualTo(expectedBody)
    }

    @Test
    fun `empty on 404`(@Wiremock server: WireMockServer, @WiremockUri uri: String) {

        server.stubFor(
            get(urlEqualTo("/manifest"))
                .willReturn(
                    aResponse().withStatus(404)
                )
        )
        val manifestUri = "$uri/manifest"
        val manifest = service.getManifest(manifestUri, AuthenticationMethod.NONE)

        assert(manifest).isNull()
    }
}