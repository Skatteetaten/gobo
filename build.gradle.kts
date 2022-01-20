plugins {
    kotlin("jvm") version "1.6.10"
    id("no.skatteetaten.gradle.aurora") version "4.4.9"
    id("io.gatling.gradle") version "3.7.4"
    id("com.github.psxpaul.execfork") version "0.1.15"
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")

    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.1.5")
    implementation("org.apache.commons:commons-text:1.9")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("com.expediagroup:graphql-kotlin-spring-server:5.3.2")
    implementation("com.graphql-java:graphql-java-extended-scalars:17.0")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.apache.commons:commons-collections4:4.4")

    implementation("org.postgresql:postgresql")
    implementation("com.h2database:h2")
    implementation("org.flywaydb:flyway-core")

    implementation("uk.q3c.rest:hal-kotlin:0.5.4.0.db32476") {
        exclude(group = "com.google.guava", module = "guava")
    }

    implementation("com.jayway.jsonpath:json-path:2.6.0")
    implementation("io.projectreactor.addons:reactor-extra:3.4.6")
    implementation("no.skatteetaten.aurora.kubernetes:kubernetes-reactor-coroutines-client:1.3.21")
    implementation("no.skatteetaten.aurora.springboot:aurora-spring-security-starter:1.7.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:1.12.2")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.25")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("no.skatteetaten.aurora:mockwebserver-extensions-kotlin:1.2.0") {
        exclude(group = "no.skatteetaten.aurora.springboot", module = "aurora-spring-boot-mvc-starter")
    }
    testImplementation("com.ninja-squad:springmockk:3.1.0")
    testImplementation("org.junit-pioneer:junit-pioneer:1.5.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.0")
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

task<com.github.psxpaul.task.ExecFork>("port-forward-mokey") {
    executable = "./mokey-port-forward.sh"
}

tasks {
    named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
        args("--spring.profiles.active=local-ocp04")
        dependsOn("port-forward-mokey")
    }
}
