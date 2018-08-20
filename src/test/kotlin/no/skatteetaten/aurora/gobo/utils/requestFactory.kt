package no.skatteetaten.aurora.gobo.utils

import okhttp3.OkHttpClient
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

fun createRequestFactory() = OkHttp3ClientHttpRequestFactory(
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
