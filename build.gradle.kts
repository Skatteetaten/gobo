plugins {
    kotlin("jvm") version "1.5.21"
    id("no.skatteetaten.gradle.aurora") version "4.3.13"
    id("org.flywaydb.flyway") version "7.14.0"
}

aurora {
    useKotlinDefaults
    useSpringBootDefaults

    useSpringBoot {
        useWebFlux
        useCloudContract
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")

    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.1.4")
    implementation("org.apache.commons:commons-text:1.9")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("com.expediagroup:graphql-kotlin-spring-server:4.2.0")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")

    implementation("org.postgresql:postgresql")
    implementation("com.h2database:h2")
    implementation("org.flywaydb:flyway-core")

    implementation("uk.q3c.rest:hal-kotlin:0.5.4.0.db32476") {
        exclude(group = "com.google.guava", module = "guava")
    }

    implementation("com.github.fge:json-patch:1.13")
    implementation("com.jayway.jsonpath:json-path:2.6.0")
    implementation("io.projectreactor.addons:reactor-extra:3.4.4")
    implementation("no.skatteetaten.aurora.kubernetes:kubernetes-reactor-coroutines-client:1.3.12")
    implementation("no.skatteetaten.aurora.springboot:aurora-spring-security-starter:1.4.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:1.12.0")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.24")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("no.skatteetaten.aurora:mockwebserver-extensions-kotlin:1.1.7")
    testImplementation("com.ninja-squad:springmockk:3.0.1")
    testImplementation("org.junit-pioneer:junit-pioneer:1.4.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.1")
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
