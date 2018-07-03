package no.skatteetaten.aurora.gobo.application

import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class DockerRegistryServiceTest {

    @Test
    fun `a`() {

        val restTemplate = RestTemplate(createRequestFactory(0, 0))
//        val dockerRegistry = DockerRegistryService(restTemplate, "https://docker-registry.aurora.sits.no:5000")
        val dockerRegistry = DockerRegistryService(restTemplate, "https://uil0paas-utv-registry01.skead.no:5000")
        val tagsFor = dockerRegistry.findAllTagsFor("no_skatteetaten_aurora/boober")

        tagsFor.forEach {
            println(it)
        }
    }

    @Throws(NoSuchAlgorithmException::class, KeyManagementException::class)
    private fun createRequestFactory(readTimeout: Long, connectionTimeout: Long): OkHttp3ClientHttpRequestFactory {

        val okHttpClientBuilder = OkHttpClient().newBuilder()
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .connectTimeout(connectionTimeout, TimeUnit.SECONDS)

        okHttpClientBuilder
                .sslSocketFactory(SSLKiller.sslContext.getSocketFactory(), SSLKiller.TrustAllX509TrustManager())

        return OkHttp3ClientHttpRequestFactory(okHttpClientBuilder.build())
    }

}

object SSLKiller {

    val sslContext: SSLContext
        @Throws(NoSuchAlgorithmException::class, KeyManagementException::class)
        get() {
            val sc1 = SSLContext.getInstance("TLS")
            sc1.init(null, arrayOf<TrustManager>(TrustAllX509TrustManager()), java.security.SecureRandom())
            return sc1
        }


    class TrustAllX509TrustManager : X509TrustManager {

        override fun getAcceptedIssuers(): Array<X509Certificate?> {

            return arrayOfNulls(0)
        }

        override fun checkClientTrusted(certs: Array<java.security.cert.X509Certificate>,
                                        authType: String) {

        }

        override fun checkServerTrusted(certs: Array<java.security.cert.X509Certificate>,
                                        authType: String) {

        }
    }
}
