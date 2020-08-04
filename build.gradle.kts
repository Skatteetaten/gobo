plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.72"
    id("org.jetbrains.kotlin.plugin.spring") version "1.3.72"
    id("org.jlleitschuh.gradle.ktlint") version "9.3.0"
    id("org.sonarqube") version "3.0"

    id("org.springframework.boot") version "2.3.2.RELEASE"
    id("org.asciidoctor.convert") version "2.4.0"

    id("com.gorylenko.gradle-git-properties") version "2.2.3"
    id("com.github.ben-manes.versions") version "0.29.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.14"
    id("no.skatteetaten.gradle.aurora") version "3.6.2"
}

val springCloudContractVersion: String = project.property("aurora.springCloudContractVersion") as String

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.8")

    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.0.2.RELEASE")
    implementation("org.apache.commons:commons-text:1.9")
    implementation("org.springframework.retry:spring-retry")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("no.skatteetaten.aurora.springboot:aurora-spring-boot-webflux-starter:1.0.7")

    implementation("com.graphql-java-kickstart:graphql-spring-boot-starter:5.10.0")
    implementation("com.graphql-java-kickstart:altair-spring-boot-starter:5.10.0")
    implementation("com.graphql-java-kickstart:voyager-spring-boot-starter:5.10.0")
    implementation("com.graphql-java-kickstart:graphql-java-tools:5.6.1")

    implementation("uk.q3c.rest:hal-kotlin:0.5.4.0.db32476")
    implementation("io.fabric8:openshift-client:4.10.3")
    implementation("com.fkorotkov:kubernetes-dsl:3.0")
    implementation("com.github.fge:json-patch:1.13")
    implementation("com.jayway.jsonpath:json-path:2.4.0")
    implementation("io.projectreactor.addons:reactor-extra:3.3.3.RELEASE")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner:$springCloudContractVersion")
    testImplementation("io.mockk:mockk:1.10.0")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.22")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("no.skatteetaten.aurora:mockmvc-extensions-kotlin:1.1.0")
    testImplementation("com.ninja-squad:springmockk:2.0.2")
    testImplementation("org.junit-pioneer:junit-pioneer:0.6.0")
}
