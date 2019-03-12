plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.21"
    id("org.jetbrains.kotlin.plugin.spring") version "1.3.21"
    id("org.jlleitschuh.gradle.ktlint") version "7.1.0"

    id("org.springframework.boot") version "2.1.3.RELEASE"
    id("org.asciidoctor.convert") version "1.6.0"

    id("com.gorylenko.gradle-git-properties") version "2.0.0"
    id("com.github.ben-manes.versions") version "0.21.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.9"

    id("no.skatteetaten.gradle.aurora") version "2.0.2"
}

dependencies {
    implementation("no.skatteetaten.aurora.springboot:aurora-spring-boot2-starter:2.0.0")

    implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.21")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.1.1")

    implementation("org.springframework.retry:spring-retry")
    implementation("org.springframework.boot:spring-boot-devtools")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-hateoas")

    implementation("com.graphql-java-kickstart:graphql-spring-boot-starter:5.7.0")
    implementation("com.graphql-java-kickstart:graphiql-spring-boot-starter:5.7.0")
    implementation("com.graphql-java-kickstart:voyager-spring-boot-starter:5.7.0")
    implementation("com.graphql-java-kickstart:graphql-java-tools:5.5.1")

    implementation("uk.q3c.rest:hal-kotlin:0.5.4.0.db32476")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("io.fabric8:openshift-client:4.1.3")
    implementation("com.fkorotkov:kubernetes-dsl:2.0.1")
    implementation("com.github.fge:json-patch:1.9")
    implementation("io.github.microutils:kotlin-logging:1.6.25")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner:2.1.1.RELEASE")
    testImplementation("io.mockk:mockk:1.9.2")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.13")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("com.nhaarman:mockito-kotlin:1.6.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:3.13.1")
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
