package no.skatteetaten.aurora.gobo.service.imageregistry

import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class ImageRegistryServiceTest {

    @Test
    fun `a`() {

        val restTemplate = RestTemplate(createRequestFactory())
        val dockerRegistry = ImageRegistryService(
            restTemplate,
            ImageRegistryUrlBuilder(),
            DefaultRegistryMetadataResolver()
        )
/*
        val imageRepoMetadata =
            ImageRepository.fromRepoString("uil0paas-utv-registry01.skead.no:5000/no_skatteetaten_aurora/boober")
                .let { ImageRepoMetadata(it.registryUrl, it.namespace, it.name) }
        val tagsFor = dockerRegistry.findTagNamesInRepoOrderedByCreatedDateDesc(imageRepoMetadata)

        tagsFor.forEach {
            println(it)
        }
*/
    }
}

private fun createRequestFactory() = OkHttp3ClientHttpRequestFactory(
    OkHttpClient().newBuilder().sslSocketFactory(
        sslContext.socketFactory,
        TrustAllX509TrustManager
    ).build()
)

private val sslContext: SSLContext = SSLContext.getInstance("TLS")
    .apply { init(null, arrayOf(TrustAllX509TrustManager), SecureRandom()) }

private object TrustAllX509TrustManager : X509TrustManager {
    override fun getAcceptedIssuers(): Array<X509Certificate?> = arrayOfNulls(0)
    override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) = Unit
    override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) = Unit
}
