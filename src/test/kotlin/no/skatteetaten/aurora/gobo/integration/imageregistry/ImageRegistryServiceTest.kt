package no.skatteetaten.aurora.gobo.integration.imageregistry

import assertk.assert
import assertk.assertions.containsAll
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.message
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.integration.enqueueJson
import no.skatteetaten.aurora.gobo.integration.imageregistry.AuthenticationMethod.KUBERNETES_TOKEN
import no.skatteetaten.aurora.gobo.integration.imageregistry.AuthenticationMethod.NONE
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.toImageRepo
import okhttp3.mockwebserver.MockWebServer
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant

class ImageRegistryServiceTest {

    private val imageRepoName = "no_skatteetaten_aurora/boober"
    private val tagName = "1"

    private val server = MockWebServer()
    private val url = server.url("/")
    private val imageRepo = ImageRepository.fromRepoString("${url.host()}:${url.port()}/$imageRepoName").toImageRepo()

    private val defaultRegistryMetadataResolver = mockk<DefaultRegistryMetadataResolver>()
    private val tokenProvider = mockk<TokenProvider>()
    private val imageRegistry = ImageRegistryService(
        ImageRegistryUrlBuilder(), defaultRegistryMetadataResolver, WebClient.create(url.toString()), tokenProvider
    )

    @BeforeEach
    fun setUp() {
        clearMocks(defaultRegistryMetadataResolver, tokenProvider)

        every {
            defaultRegistryMetadataResolver.getMetadataForRegistry(any())
        } returns RegistryMetadata("${url.host()}:${url.port()}", "http", NONE)
    }

    @Test
    fun `verify fetches all tags for specified repo`() {
        server.enqueueJson(body = tagsListResponse)
        val tags = imageRegistry.findTagNamesInRepoOrderedByCreatedDateDesc(imageRepo)
        val request = server.takeRequest()

        assert(tags).containsAll(
            "1",
            "develop-SNAPSHOT",
            "1.0.0-rc.2-b2.2.3-oracle8-1.4.0",
            "1.0.0-rc.1-b2.2.3-oracle8-1.4.0",
            "master-SNAPSHOT"
        )
        assert(request.path).isEqualTo("/v2/$imageRepoName/tags/list")
        assert(request.headers[HttpHeaders.AUTHORIZATION]).isNull()
    }

    @Test
    fun `fetch all tags with authorization header`() {
        every { tokenProvider.token } returns "token"

        every {
            defaultRegistryMetadataResolver.getMetadataForRegistry(any())
        } returns RegistryMetadata("${url.host()}:${url.port()}", "http", KUBERNETES_TOKEN)

        server.enqueueJson(body = tagsListResponse)

        val tags = imageRegistry.findTagNamesInRepoOrderedByCreatedDateDesc(imageRepo)
        val request = server.takeRequest()

        assert(request.path).isEqualTo("/v2/$imageRepoName/tags/list")
        assert(tags).isNotNull()
        assert(request.headers[HttpHeaders.AUTHORIZATION]).isEqualTo("Bearer token")
    }

    @Test
    fun `verify tag can be found by name`() {
        server.enqueueJson(body = manifestResponse)

        val tag = imageRegistry.findTagByName(imageRepo, tagName)
        val request = server.takeRequest()
        assert(tag.created).isEqualTo(Instant.parse("2017-09-25T11:38:20.361177648Z"))
        assert(tag.name).isEqualTo(tagName)
        assert(tag.type).isEqualTo(ImageTagType.MAJOR)
        assert(request.path).isEqualTo("/v2/$imageRepoName/manifests/$tagName")
    }

    @Test
    fun `Throw exception when bad request is returned from registry`() {
        server.enqueueJson(404, "Not found")

        assert {
            imageRegistry.findTagByName(imageRepo, tagName)
        }.thrownError {
            server.takeRequest()
            message().isEqualTo("No metadata for tag=$tagName in repo=${imageRepo.repository}")
        }
    }
}

@Language("JSON")
private const val tagsListResponse = """{
  "name": "no_skatteetaten_aurora/boober",
  "tags": [
    "master-SNAPSHOT",
    "1.0.0-rc.1-b2.2.3-oracle8-1.4.0",
    "1.0.0-rc.2-b2.2.3-oracle8-1.4.0",
    "develop-SNAPSHOT",
    "1"
  ]
}"""

@Language("JSON")
private const val manifestResponse = """{
  "schemaVersion": 1,
  "name": "no_skatteetaten_aurora/boober",
  "tag": "1",
  "architecture": "amd64",
  "fsLayers": [
    {
      "blobSum": "sha256:a3ed95caeb02ffe68cdd9fd84406680ae93d633cb16422d00e8a7c22955b46d4"
    },
    {
      "blobSum": "sha256:a3ed95caeb02ffe68cdd9fd84406680ae93d633cb16422d00e8a7c22955b46d4"
    },
    {
      "blobSum": "sha256:8769886f5d4958a9c9d53f0184babe8d9f9728750d1f634b3e8d29a7a559767d"
    },
    {
      "blobSum": "sha256:02a3015451826a8f5fbd75150f12022d3b22513c7faaae63e98686116342841b"
    },
    {
      "blobSum": "sha256:a3ed95caeb02ffe68cdd9fd84406680ae93d633cb16422d00e8a7c22955b46d4"
    },
    {
      "blobSum": "sha256:a3ed95caeb02ffe68cdd9fd84406680ae93d633cb16422d00e8a7c22955b46d4"
    },
    {
      "blobSum": "sha256:a3ed95caeb02ffe68cdd9fd84406680ae93d633cb16422d00e8a7c22955b46d4"
    },
    {
      "blobSum": "sha256:a3ed95caeb02ffe68cdd9fd84406680ae93d633cb16422d00e8a7c22955b46d4"
    },
    {
      "blobSum": "sha256:a3ed95caeb02ffe68cdd9fd84406680ae93d633cb16422d00e8a7c22955b46d4"
    },
    {
      "blobSum": "sha256:a3ed95caeb02ffe68cdd9fd84406680ae93d633cb16422d00e8a7c22955b46d4"
    },
    {
      "blobSum": "sha256:a3ed95caeb02ffe68cdd9fd84406680ae93d633cb16422d00e8a7c22955b46d4"
    },
    {
      "blobSum": "sha256:a3ed95caeb02ffe68cdd9fd84406680ae93d633cb16422d00e8a7c22955b46d4"
    },
    {
      "blobSum": "sha256:1b08e710e56d5d68ad12291a12d0a8ec6cfff7620d5de26973ccb6011508fb0c"
    },
    {
      "blobSum": "sha256:a3ed95caeb02ffe68cdd9fd84406680ae93d633cb16422d00e8a7c22955b46d4"
    },
    {
      "blobSum": "sha256:7417e681622da825bed3e7805115a191eb225b4ad97a349502c25add9b79ca90"
    },
    {
      "blobSum": "sha256:cc97e04d66637b95500f3ed81df79eeb368260443ac075e4cd62657bc2be1b29"
    },
    {
      "blobSum": "sha256:9540b542f94d6e3fb8ebbea4cd8e77372caa4b9ddcb13d19ec2c99b72534aa44"
    },
    {
      "blobSum": "sha256:3195fd76ce36cf61e727de01c784cb25331cc05614ca453a0437a64f5fad96ce"
    },
    {
      "blobSum": "sha256:938c70b3e850d6e4a8d564dc25618db469fb8e96ecd73ee72668c7ca53244e25"
    },
    {
      "blobSum": "sha256:acebbf7b94edbfa17ef317ac1908d8ae62c70219f6e31eefa0e0de4dfa224b68"
    },
    {
      "blobSum": "sha256:fbe1a15141b58f55771623d744d89bf032997288e7b1bf55c01ae15c59c318a0"
    },
    {
      "blobSum": "sha256:a3ed95caeb02ffe68cdd9fd84406680ae93d633cb16422d00e8a7c22955b46d4"
    },
    {
      "blobSum": "sha256:a3ed95caeb02ffe68cdd9fd84406680ae93d633cb16422d00e8a7c22955b46d4"
    },
    {
      "blobSum": "sha256:00d19003217b69eca457158912c38a2ab5f6ae78a99c2512d2e56696248c3cf3"
    }
  ],
  "history": [
    {
      "v1Compatibility": "{\"architecture\":\"amd64\",\"author\":\"Aurora OpenShift Utvikling \\u003cutvpaas@skatteetaten.no\\u003e\",\"config\":{\"Hostname\":\"11fbdc1f630f\",\"Domainname\":\"\",\"User\":\"\",\"AttachStdin\":false,\"AttachStdout\":false,\"AttachStderr\":false,\"Tty\":false,\"OpenStdin\":false,\"StdinOnce\":false,\"Env\":[\"PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/jre/bin\",\"JAVA_VERSION_MAJOR=8\",\"JAVA_VERSION_MINOR=121\",\"JAVA_VERSION_BUILD=13\",\"JAVA_PACKAGE=server-jre\",\"JAVA_HOME=/jre\",\"LANG=C.UTF-8\",\"HOME=/u01\",\"JOLOKIA_VERSION=1.3.5\",\"JOLOKIA_PATH=/opt/jolokia/jolokia-jvm-1.3.5-agent.jar\",\"KEY_STORE=/jre/lib/security/cacerts\",\"BASE_IMAGE_VERSION=1.4.0\",\"ALPINE_VERSION=3.5\",\"LOGBACK_FILE=/u01/logback.xml\",\"AURORA_VERSION=1.0.0-b2.2.5-oracle8-1.4.0\",\"APP_VERSION=1.0.0\",\"PUSH_EXTRA_TAGS=latest,major,minor,patch\"],\"Cmd\":[\"bin/run\"],\"ArgsEscaped\":true,\"Image\":\"sha256:52a41ac45f29d84fe8ca74b72fa7644622c2a08a0cb6769fc9c41065ad927529\",\"Volumes\":null,\"WorkingDir\":\"/u01\",\"Entrypoint\":[\"/usr/bin/dumb-init\",\"--\"],\"OnBuild\":[],\"Labels\":{\"alpine\":\"3.5\",\"io.k8s.description\":\"Controler for creating/updating application objects\",\"io.openshift.tags\":\"openshift,springboot,kotlin\",\"java\":\"oracle-8u121-b13\",\"jolokia\":\"1.3.5\",\"version\":\"1.4.0\"}},\"container\":\"9a3bc5feb2058c0d9c19f2356d6372091b5dd6340f215172b3165eeff6fd521d\",\"container_config\":{\"Hostname\":\"11fbdc1f630f\",\"Domainname\":\"\",\"User\":\"\",\"AttachStdin\":false,\"AttachStdout\":false,\"AttachStderr\":false,\"Tty\":false,\"OpenStdin\":false,\"StdinOnce\":false,\"Env\":[\"PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/jre/bin\",\"JAVA_VERSION_MAJOR=8\",\"JAVA_VERSION_MINOR=121\",\"JAVA_VERSION_BUILD=13\",\"JAVA_PACKAGE=server-jre\",\"JAVA_HOME=/jre\",\"LANG=C.UTF-8\",\"HOME=/u01\",\"JOLOKIA_VERSION=1.3.5\",\"JOLOKIA_PATH=/opt/jolokia/jolokia-jvm-1.3.5-agent.jar\",\"KEY_STORE=/jre/lib/security/cacerts\",\"BASE_IMAGE_VERSION=1.4.0\",\"ALPINE_VERSION=3.5\",\"LOGBACK_FILE=/u01/logback.xml\",\"AURORA_VERSION=1.0.0-b2.2.5-oracle8-1.4.0\",\"APP_VERSION=1.0.0\",\"PUSH_EXTRA_TAGS=latest,major,minor,patch\"],\"Cmd\":[\"/bin/sh\",\"-c\",\"#(nop) \",\"CMD [\\\"bin/run\\\"]\"],\"ArgsEscaped\":true,\"Image\":\"sha256:52a41ac45f29d84fe8ca74b72fa7644622c2a08a0cb6769fc9c41065ad927529\",\"Volumes\":null,\"WorkingDir\":\"/u01\",\"Entrypoint\":[\"/usr/bin/dumb-init\",\"--\"],\"OnBuild\":[],\"Labels\":{\"alpine\":\"3.5\",\"io.k8s.description\":\"Controler for creating/updating application objects\",\"io.openshift.tags\":\"openshift,springboot,kotlin\",\"java\":\"oracle-8u121-b13\",\"jolokia\":\"1.3.5\",\"version\":\"1.4.0\"}},\"created\":\"2017-09-25T11:38:20.361177648Z\",\"docker_version\":\"1.12.6\",\"id\":\"d49972a13cb1a474f502adad3f1cfda407f9ad3f6f686e794065538b4d4eb013\",\"os\":\"linux\",\"parent\":\"b614876c8990e4106eeb75128cfb03db3a60463801215137350aae30c8c7e0fd\",\"throwaway\":true}"
    },
    {
      "v1Compatibility": "{\"id\":\"b614876c8990e4106eeb75128cfb03db3a60463801215137350aae30c8c7e0fd\",\"parent\":\"99258ba72e52832194cdce994825748dc11274f8e156011d85dd9411e2783fa5\",\"created\":\"2017-09-25T11:38:18.965033961Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop)  ENV AURORA_VERSION=1.0.0-b2.2.5-oracle8-1.4.0 APP_VERSION=1.0.0 PUSH_EXTRA_TAGS=latest,major,minor,patch\"]},\"throwaway\":true}"
    },
    {
      "v1Compatibility": "{\"id\":\"99258ba72e52832194cdce994825748dc11274f8e156011d85dd9411e2783fa5\",\"parent\":\"6e74e8a4963f0d554913744d79a53b41a1890628d2953aef4ce6b280ff8e10f7\",\"created\":\"2017-09-25T11:38:17.59844068Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c chmod -R 777 /u01/\"]}}"
    },
    {
      "v1Compatibility": "{\"id\":\"6e74e8a4963f0d554913744d79a53b41a1890628d2953aef4ce6b280ff8e10f7\",\"parent\":\"c29331df87f5f3ba2241368d486da2835aa67673a25ac2cf25daf77c63e1fd07\",\"created\":\"2017-09-25T11:38:12.955791473Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop) COPY dir:8a65442746a8fe0c91c4d579eeeb851b892612cbd2df5f77daa6035cd988fe69 in /u01 \"]}}"
    },
    {
      "v1Compatibility": "{\"id\":\"c29331df87f5f3ba2241368d486da2835aa67673a25ac2cf25daf77c63e1fd07\",\"parent\":\"dbf2e0340715ff4a957a93dc8314050ef5594496f10d41d1a484cc4ef22f3589\",\"created\":\"2017-09-25T11:38:10.37225516Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop)  LABEL io.k8s.description=Controler for creating/updating application objects io.openshift.tags=openshift,springboot,kotlin\"]},\"throwaway\":true}"
    },
    {
      "v1Compatibility": "{\"id\":\"dbf2e0340715ff4a957a93dc8314050ef5594496f10d41d1a484cc4ef22f3589\",\"parent\":\"6bce1c6d74656d10b648ff1c7d4378be6b2d3d570f8c76ae55effeb8403b9b69\",\"created\":\"2017-09-25T11:38:08.53913627Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop)  MAINTAINER Aurora OpenShift Utvikling \\u003cutvpaas@skatteetaten.no\\u003e\"]},\"throwaway\":true}"
    },
    {
      "v1Compatibility": "{\"id\":\"6bce1c6d74656d10b648ff1c7d4378be6b2d3d570f8c76ae55effeb8403b9b69\",\"parent\":\"e7ddbe4aa7acb422553a9017389a88ff49a6cfab2b61b9623f1ed4288a1aa7a6\",\"created\":\"2017-02-16T09:51:45.607072668Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop)  LABEL version=1.4.0\"]},\"throwaway\":true}"
    },
    {
      "v1Compatibility": "{\"id\":\"e7ddbe4aa7acb422553a9017389a88ff49a6cfab2b61b9623f1ed4288a1aa7a6\",\"parent\":\"288441b9f273991a0df2a145c94f697d663c9daa316dffdf91909fb8501002b2\",\"created\":\"2017-02-16T09:51:43.556228405Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop)  LABEL jolokia=1.3.5\"]},\"throwaway\":true}"
    },
    {
      "v1Compatibility": "{\"id\":\"288441b9f273991a0df2a145c94f697d663c9daa316dffdf91909fb8501002b2\",\"parent\":\"79c635cac7ffd6175a039b79b6528b0daf0e6b6a946f611c2503a1d7454cdae2\",\"created\":\"2017-02-16T09:51:41.241040763Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop)  LABEL alpine=3.5\"]},\"throwaway\":true}"
    },
    {
      "v1Compatibility": "{\"id\":\"79c635cac7ffd6175a039b79b6528b0daf0e6b6a946f611c2503a1d7454cdae2\",\"parent\":\"4ce074f0c471162370f6d77bc47c89b70874916dbbe7d71524b00d1595755d5d\",\"created\":\"2017-02-16T09:51:39.09520681Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop)  LABEL java=oracle-8u121-b13\"]},\"throwaway\":true}"
    },
    {
      "v1Compatibility": "{\"id\":\"4ce074f0c471162370f6d77bc47c89b70874916dbbe7d71524b00d1595755d5d\",\"parent\":\"27cf691330dbb1673eeec13a156c89d41d6370f9759316e3c7ce48b00ac79c26\",\"created\":\"2017-02-16T09:51:36.547996164Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop)  ENTRYPOINT [\\\"/usr/bin/dumb-init\\\" \\\"--\\\"]\"]},\"throwaway\":true}"
    },
    {
      "v1Compatibility": "{\"id\":\"27cf691330dbb1673eeec13a156c89d41d6370f9759316e3c7ce48b00ac79c26\",\"parent\":\"ed4dfc7cb0e59d3301d8c358ef46e7ff69f9010ee6e178fa8a0bab70c4194c65\",\"created\":\"2017-02-16T09:51:34.034339716Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop)  ENV LOGBACK_FILE=/u01/logback.xml\"]},\"throwaway\":true}"
    },
    {
      "v1Compatibility": "{\"id\":\"ed4dfc7cb0e59d3301d8c358ef46e7ff69f9010ee6e178fa8a0bab70c4194c65\",\"parent\":\"e1664dd3bbb7cb94a1894185370601164202169ecacc5eda1da2221c5041aeec\",\"created\":\"2017-02-16T09:51:31.954133048Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop) COPY file:ab0eb00468aab0c926a1034fe0a5a2b25b7d406e057a13177a4ee5518827c090 in /u01 \"]}}"
    },
    {
      "v1Compatibility": "{\"id\":\"e1664dd3bbb7cb94a1894185370601164202169ecacc5eda1da2221c5041aeec\",\"parent\":\"83ac32f40c245bdb5739c755ce00e632672bef415ddf0294e5640e7c67a35f06\",\"created\":\"2017-02-16T09:51:29.203016103Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop)  WORKDIR /u01\"]},\"throwaway\":true}"
    },
    {
      "v1Compatibility": "{\"id\":\"83ac32f40c245bdb5739c755ce00e632672bef415ddf0294e5640e7c67a35f06\",\"parent\":\"c95bb8a2495f3400e29e69c19bc385bec5594d6a5f8cfc295fb9e05f6ab230da\",\"created\":\"2017-02-16T09:51:27.015111718Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop) COPY file:9222647985c8f408dd2c9b5076829ce4da3a43a1024a47242ef6d2dccdfa3e25 in /jre/lib/security/cacerts \"]}}"
    },
    {
      "v1Compatibility": "{\"id\":\"c95bb8a2495f3400e29e69c19bc385bec5594d6a5f8cfc295fb9e05f6ab230da\",\"parent\":\"c12db5eef5240bddba98b1a331eb81ba632c58938e6f6a2edc2922b24c04e420\",\"created\":\"2017-02-16T09:51:20.598754394Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c apk add --update curl ca-certificates \\u0026\\u0026       cd /tmp \\u0026\\u0026       curl -k -L -o glibc-2.23-r1.apk https://github.com/andyshinn/alpine-pkg-glibc/releases/download/2.23-r1/glibc-2.23-r1.apk \\u0026\\u0026       curl -k -L -o glibc-bin-2.23-r1.apk https://github.com/andyshinn/alpine-pkg-glibc/releases/download/2.23-r1/glibc-bin-2.23-r1.apk \\u0026\\u0026       apk add --allow-untrusted           glibc-2.23-r1.apk           glibc-bin-2.23-r1.apk \\u0026\\u0026       echo 'hosts: files mdns4_minimal [NOTFOUND=return] dns mdns4' \\u003e\\u003e /etc/nsswitch.conf \\u0026\\u0026       curl -jksSLH \\\"Cookie: oraclelicense=accept-securebackup-cookie\\\"           \\\"http://download.oracle.com/otn-pub/java/jdk/${'$'}{JAVA_VERSION_MAJOR}u${'$'}{JAVA_VERSION_MINOR}-b${'$'}{JAVA_VERSION_BUILD}/e9e7ea248e2c4826b92b3f075a80e441/${'$'}{JAVA_PACKAGE}-${'$'}{JAVA_VERSION_MAJOR}u${'$'}{JAVA_VERSION_MINOR}-linux-x64.tar.gz\\\"           | gunzip -c - | tar -xf - \\u0026\\u0026       mv jdk1.${'$'}{JAVA_VERSION_MAJOR}.0_${'$'}{JAVA_VERSION_MINOR}/jre /jre \\u0026\\u0026       curl -jkSLH \\\"Cookie: oraclelicense=accept-securebackup-cookie\\\"           \\\"http://download.oracle.com/otn-pub/java/jce/8/jce_policy-8.zip\\\" \\u003e jce_policy.zip  \\u0026\\u0026       unzip jce_policy.zip \\u0026\\u0026       cp UnlimitedJCEPolicyJDK8/local_policy.jar /jre/lib/security/ \\u0026\\u0026       cp UnlimitedJCEPolicyJDK8/US_export_policy.jar /jre/lib/security/ \\u0026\\u0026       rm jce_policy.zip \\u0026\\u0026       rm -r UnlimitedJCEPolicyJDK8 \\u0026\\u0026       apk del curl ca-certificates \\u0026\\u0026       rm /jre/bin/jjs \\u0026\\u0026       rm /jre/bin/keytool \\u0026\\u0026       rm /jre/bin/orbd \\u0026\\u0026       rm /jre/bin/pack200 \\u0026\\u0026       rm /jre/bin/policytool \\u0026\\u0026       rm /jre/bin/rmid \\u0026\\u0026       rm /jre/bin/rmiregistry \\u0026\\u0026       rm /jre/bin/servertool \\u0026\\u0026       rm /jre/bin/tnameserv \\u0026\\u0026       rm /jre/bin/unpack200 \\u0026\\u0026       rm /jre/lib/ext/nashorn.jar \\u0026\\u0026       rm /jre/lib/jfr.jar \\u0026\\u0026       rm -rf /jre/lib/jfr \\u0026\\u0026       rm -rf /jre/lib/oblique-fonts \\u0026\\u0026       rm -rf /tmp/* /var/cache/apk/*\"]}}"
    },
    {
      "v1Compatibility": "{\"id\":\"c12db5eef5240bddba98b1a331eb81ba632c58938e6f6a2edc2922b24c04e420\",\"parent\":\"04b468ea158dccc846387bc58e1c477cc77bcaf4cc475ddd36ebe28e67be16b3\",\"created\":\"2017-02-16T09:50:42.687074876Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c apk add dumb-init --update-cache --repository http://nl.alpinelinux.org/alpine/edge/community\"]}}"
    },
    {
      "v1Compatibility": "{\"id\":\"04b468ea158dccc846387bc58e1c477cc77bcaf4cc475ddd36ebe28e67be16b3\",\"parent\":\"517df0c846b7ff8beadaa1731bc86e0df6606166d686354c81ae682aa7539774\",\"created\":\"2017-02-16T09:50:39.099118339Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c apk add --no-cache tzdata \\u0026\\u0026   cp /usr/share/zoneinfo/Europe/Oslo /etc/localtime \\u0026\\u0026   echo \\\"Europe/Oslo\\\" \\u003e /etc/timezone \\u0026\\u0026   apk del tzdata\"]}}"
    },
    {
      "v1Compatibility": "{\"id\":\"517df0c846b7ff8beadaa1731bc86e0df6606166d686354c81ae682aa7539774\",\"parent\":\"b656f984cfbb281598023827d2b86def701d695b57726700a29d2b810f3c902c\",\"created\":\"2017-02-16T09:50:35.046342288Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c chmod -R 777 /opt/jolokia \\u0026\\u0026     apk add --no-cache drill bash \\u0026\\u0026     mkdir ${'$'}HOME \\u0026\\u0026     chmod -R 777 ${'$'}HOME\"]}}"
    },
    {
      "v1Compatibility": "{\"id\":\"b656f984cfbb281598023827d2b86def701d695b57726700a29d2b810f3c902c\",\"parent\":\"5d6070e8234ff3915df3b7ec6b90db0dae5638f6b60329c0b5a2f96c41aef37c\",\"created\":\"2017-02-16T09:50:25.089076683Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop) COPY file:ac7f41b592edbeef9db8681e160ece122ce1ebb7721dc20ff3142caaf5bb6fea in /usr/local/bin/dig \"]}}"
    },
    {
      "v1Compatibility": "{\"id\":\"5d6070e8234ff3915df3b7ec6b90db0dae5638f6b60329c0b5a2f96c41aef37c\",\"parent\":\"d3acfe03100b3925636447112b230b55962686734bac7524e626fd76328dd2d6\",\"created\":\"2017-02-16T09:50:22.990018537Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop) ADD dir:26aada285163b18382ca57cc3bf2b6ab04f461b6d3c6286578b3f6ad02e3e47a in /opt/jolokia \"]}}"
    },
    {
      "v1Compatibility": "{\"id\":\"d3acfe03100b3925636447112b230b55962686734bac7524e626fd76328dd2d6\",\"parent\":\"57346420982cdcc0f6a546dbc38c4d80c76ca64212cb8c439fe765065dbac0ee\",\"created\":\"2017-02-16T09:50:20.071012736Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop)  ENV JAVA_VERSION_MAJOR=8 JAVA_VERSION_MINOR=121 JAVA_VERSION_BUILD=13 JAVA_PACKAGE=server-jre JAVA_HOME=/jre PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/jre/bin LANG=C.UTF-8 HOME=/u01 JOLOKIA_VERSION=1.3.5 JOLOKIA_PATH=/opt/jolokia/jolokia-jvm-1.3.5-agent.jar KEY_STORE=/jre/lib/security/cacerts BASE_IMAGE_VERSION=1.4.0 ALPINE_VERSION=3.5\"]},\"throwaway\":true}"
    },
    {
      "v1Compatibility": "{\"id\":\"57346420982cdcc0f6a546dbc38c4d80c76ca64212cb8c439fe765065dbac0ee\",\"parent\":\"54f3b975150482086b618b346204671160c57aa663feede4703308c4c2178a8e\",\"created\":\"2017-02-16T09:50:17.159056118Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop)  MAINTAINER Skatteetaten Utviklingsplattform \\u003cutvpass@skatteetaten.no\\u003e\"]},\"throwaway\":true}"
    },
    {
      "v1Compatibility": "{\"id\":\"54f3b975150482086b618b346204671160c57aa663feede4703308c4c2178a8e\",\"created\":\"2016-12-27T18:17:25.702182968Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop) ADD file:92ab746eb22dd3ed2b87469c719adf3c1bed7302653bbd76baafd7cfd95e911e in / \"]}}"
    }
  ],
  "signatures": [
    {
      "header": {
        "jwk": {
          "crv": "P-256",
          "kid": "RO6T:DD4G:QYGC:42XQ:WOCD:NTZE:S5T7:BZ36:62NL:42SV:645O:YYQB",
          "kty": "EC",
          "x": "uBB1hW7c9mJDExJ2u5d8ipSdSam9TQrCZ3vHs4yBE4A",
          "y": "Tba1YhwYJFCoZysGopz-XycUZmOrpZwxutIyRxbxz4E"
        },
        "alg": "ES256"
      },
      "signature": "lrAlGY_kDrpraGO6o6OHefIfHZy8XowRvX1uI-pAzpcOpDIAhk-mD69BaBv3RE5qrlPytQ_rv_I5wqeME_eZ3g",
      "protected": "eyJmb3JtYXRMZW5ndGgiOjE3MTg1LCJmb3JtYXRUYWlsIjoiQ24wIiwidGltZSI6IjIwMTgtMDgtMjBUMTY6MTI6NDFaIn0"
    }
  ]
}"""
