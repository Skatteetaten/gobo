package no.skatteetaten.aurora.gobo.integration.imageregistry

import assertk.assert
import assertk.assertions.isEqualTo
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.web.reactive.function.client.WebClient
import ru.lanwen.wiremock.ext.WiremockResolver
import ru.lanwen.wiremock.ext.WiremockResolver.Wiremock
import ru.lanwen.wiremock.ext.WiremockUriResolver
import ru.lanwen.wiremock.ext.WiremockUriResolver.WiremockUri

@ExtendWith(WiremockResolver::class, WiremockUriResolver::class)
internal class ImageRegistryClientTest {

    val service = ImageRegistryClient(WebClient.create(), TokenProvider(""))

    @Test
    fun shouldInjectWiremock(@Wiremock server: WireMockServer, @WiremockUri uri: String) {

        val expectedBody = "Hello world!"
        server.stubFor(
            get(urlEqualTo("/manifest"))
                .willReturn(
                    aResponse()
                        .withBody(expectedBody)
                )
        );
        val manifestUri = "$uri/manifest"
        val manifest = service.getManifest(manifestUri, AuthenticationMethod.NONE)

        assert(manifest).isEqualTo(expectedBody)
    }
}