plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.50"
    id("org.jetbrains.kotlin.plugin.spring") version "1.3.50"
    id("org.jlleitschuh.gradle.ktlint") version "9.0.0"
    id("org.sonarqube") version "2.8"

    id("org.springframework.boot") version "2.2.0.RELEASE"
    id("org.asciidoctor.convert") version "2.3.0"

    id("com.gorylenko.gradle-git-properties") version "2.2.0"
    id("com.github.ben-manes.versions") version "0.27.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.13"
    id("no.skatteetaten.gradle.aurora") version "3.2.0"
}

val springCloudContractVersion: String = project.property("aurora.springCloudContractVersion") as String

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.2")

    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.0.0.RELEASE")
    implementation("org.apache.commons:commons-text:1.8")
    implementation("org.springframework.retry:spring-retry")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("com.graphql-java-kickstart:graphql-spring-boot-starter:5.10.0")
    implementation("com.graphql-java-kickstart:altair-spring-boot-starter:5.10.0")
    implementation("com.graphql-java-kickstart:voyager-spring-boot-starter:5.10.0")
    implementation("com.graphql-java-kickstart:graphql-java-tools:5.6.1")

    implementation("uk.q3c.rest:hal-kotlin:0.5.4.0.db32476")
    implementation("io.fabric8:openshift-client:4.6.1")
    implementation("com.fkorotkov:kubernetes-dsl:3.0")
    implementation("com.github.fge:json-patch:1.9")
    implementation("com.jayway.jsonpath:json-path:2.4.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner:$springCloudContractVersion")
    testImplementation("io.mockk:mockk:1.9.3")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.19")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("com.nhaarman:mockito-kotlin:1.6.0")
    testImplementation("no.skatteetaten.aurora:mockmvc-extensions-kotlin:1.0.2")
    testImplementation("com.ninja-squad:springmockk:1.1.3")
}

tasks {
    fun createTagsArray(tags: Any?) = (tags as String).split(",").toTypedArray()

    register<Test>("testExclude") {
        if (project.hasProperty("tags")) {
            useJUnitPlatform {
                excludeTags(*createTagsArray(project.property("tags")))
            }
        }
    }

    register<Test>("testOnly") {
        if (project.hasProperty("tags")) {
            useJUnitPlatform {
                includeTags(*createTagsArray(project.property("tags")))
            }
        }
    }
}
