plugins {
    id("java")
    id("no.skatteetaten.gradle.aurora") version "4.0.5"
}

aurora {
    useAuroraDefaults
    useAsciiDoctor
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

    implementation("uk.q3c.rest:hal-kotlin:0.5.4.0.db32476")
    implementation("io.fabric8:openshift-client:4.12.0")
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
}
