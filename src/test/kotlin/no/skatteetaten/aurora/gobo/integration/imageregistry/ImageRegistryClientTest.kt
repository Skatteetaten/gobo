package no.skatteetaten.aurora.gobo.integration.imageregistry

import assertk.assert
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.web.reactive.function.client.WebClient
import ru.lanwen.wiremock.ext.WiremockResolver
import ru.lanwen.wiremock.ext.WiremockResolver.Wiremock
import ru.lanwen.wiremock.ext.WiremockUriResolver
import ru.lanwen.wiremock.ext.WiremockUriResolver.WiremockUri

@ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
class ImageRegistryClientTest {

    val tokenProvider = mockk<TokenProvider>()

    val service = ImageRegistryClient(WebClient.create(), tokenProvider)

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