plugins {
    kotlin("jvm") version "1.4.32"
    id("no.skatteetaten.gradle.aurora") version "4.2.3"
    id("org.flywaydb.flyway") version "7.8.1"
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")

    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.1.3")
    implementation("org.apache.commons:commons-text:1.9")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("com.expediagroup:graphql-kotlin-spring-server:4.0.1")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")

    // Postgres
    implementation("org.postgresql:postgresql")

    implementation("com.h2database:h2")

    implementation("org.flywaydb:flyway-core")

    implementation("uk.q3c.rest:hal-kotlin:0.5.4.0.db32476") {
        exclude(group = "com.google.guava", module = "guava")
    }

    implementation("io.fabric8:openshift-client:5.3.0")
    implementation("com.github.fge:json-patch:1.13")
    implementation("com.jayway.jsonpath:json-path:2.5.0")
    implementation("io.projectreactor.addons:reactor-extra:3.4.3")
    implementation("no.skatteetaten.aurora.kubernetes:kubernetes-reactor-coroutines-client:1.0-SNAPSHOT")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:1.11.0")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.23.1")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("no.skatteetaten.aurora:mockmvc-extensions-kotlin:1.1.6")
    testImplementation("com.ninja-squad:springmockk:3.0.1")
    testImplementation("org.junit-pioneer:junit-pioneer:1.3.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.4.3")
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
