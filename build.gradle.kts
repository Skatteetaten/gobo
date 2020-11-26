plugins {
    id("java")
    id("no.skatteetaten.gradle.aurora") version "4.0.5"
    id("org.flywaydb.flyway") version "7.1.1"
}

aurora {
    useAuroraDefaults
    useKotlin {
        useKtLint
    }
    useSpringBoot {
        useWebFlux
        useCloudContract
    }

    features {
        checkstylePlugin = false
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")

    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.0.2.RELEASE")
    implementation("org.apache.commons:commons-text:1.9")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("com.expediagroup:graphql-kotlin-spring-server:3.6.6")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.expediagroup:graphql-kotlin-spring-server:3.6.4")

    // Postgres
    implementation("org.postgresql:postgresql")

    implementation("uk.q3c.rest:hal-kotlin:0.5.4.0.db32476") {
        exclude(group = "com.google.guava", module = "guava")
    }
    implementation("io.fabric8:openshift-client:4.12.0")
    // h2
    implementation("com.h2database:h2")

    implementation("org.flywaydb:flyway-core")

    implementation("uk.q3c.rest:hal-kotlin:0.5.4.0.db32476")
    implementation("io.fabric8:openshift-client:4.11.1")
    implementation("com.github.fge:json-patch:1.13")
    implementation("com.jayway.jsonpath:json-path:2.4.0")
    implementation("io.projectreactor.addons:reactor-extra:3.3.3.RELEASE")
    implementation("no.skatteetaten.aurora.kubernetes:kubernetes-reactor-coroutines-client:1.3.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:1.10.2")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.22")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("no.skatteetaten.aurora:mockmvc-extensions-kotlin:1.1.5")
    testImplementation("com.ninja-squad:springmockk:2.0.3")
    testImplementation("org.junit-pioneer:junit-pioneer:1.0.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.4.1")
    testImplementation("no.skatteetaten.aurora:mockmvc-extensions-kotlin:1.1.5")
    testImplementation("com.ninja-squad:springmockk:2.0.3")
    testImplementation("org.junit-pioneer:junit-pioneer:0.9.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.3.9")
}

task<de.undercouch.gradle.tasks.download.Download>("download-playground") {
    val baseUrl = "https://cdn.jsdelivr.net/npm/graphql-playground-react/build"
    src(
        listOf(
            "$baseUrl/static/js/middleware.js",
            "$baseUrl/static/css/index.css",
            "$baseUrl/favicon.png",
            "$baseUrl/logo.png"
        )
    )
    dest("src/main/resources/static/playground/")
    onlyIfModified(true)
}
