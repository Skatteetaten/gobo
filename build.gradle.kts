plugins {
    kotlin("jvm") version "1.7.10"
    id("no.skatteetaten.gradle.aurora") version "4.5.4"
    id("io.gatling.gradle") version "3.8.3.2"
    id("org.asciidoctor.jvm.convert") version "3.3.2"
}

aurora {
    useKotlinDefaults
    useSpringBootDefaults

    useSpringBoot {
        useWebFlux
        useCloudContract
    }

    versions {
        springCloudContract = "3.1.3"
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.1.7")
    implementation("org.apache.commons:commons-text:1.9")
    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("com.expediagroup:graphql-kotlin-spring-server:5.5.0")
    // bump transitive dependency because of high security risk in version 61.1
    implementation("com.ibm.icu:icu4j:71.1")

    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.1")

    implementation("org.postgresql:postgresql:42.4.2")
    implementation("org.flywaydb:flyway-core")

    implementation("uk.q3c.rest:hal-kotlin:0.5.4.0.db32476") {
        exclude(group = "com.google.guava", module = "guava")
    }

    implementation("com.jayway.jsonpath:json-path:2.7.0")
    implementation("io.projectreactor.addons:reactor-extra:3.4.8")
    implementation("no.skatteetaten.aurora.kubernetes:kubernetes-reactor-coroutines-client:1.3.32")
    implementation("no.skatteetaten.aurora.springboot:aurora-spring-security-starter:1.14.0")

    testImplementation("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:1.12.7")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.25")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("no.skatteetaten.aurora:mockwebserver-extensions-kotlin:1.3.1") {
        exclude(group = "no.skatteetaten.aurora.springboot", module = "aurora-spring-boot-mvc-starter")
    }
    testImplementation("com.ninja-squad:springmockk:3.1.1")
    testImplementation("org.junit-pioneer:junit-pioneer:1.7.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("io.kubernetes:client-java:16.0.0")
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
repositories {
    mavenCentral()
}

tasks {
    named<org.asciidoctor.gradle.jvm.AsciidoctorTask>("asciidoctor") {
        sourceDir("src/main/asciidoc")
        setBaseDir("src/test/resources/graphql")
        logDocuments = true
    }
}

task<JavaExec>("runLocal") {
    mainClass.set("no.skatteetaten.aurora.gobo.GoboTestMainKt")
    classpath(sourceSets["test"].runtimeClasspath, sourceSets["main"].runtimeClasspath)
}
