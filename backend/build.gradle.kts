// 백엔드 의존성·컴파일·포맷·단위/통합 검사·격리 DB 기반 jOOQ 생성·배포 JAR 작업을 정의한다.
// 게임 JSON은 원본 생성 절차를 따르며 아래 자원 작업은 표시 자료와 검증 원자료의 실행/테스트 범위를 구분한다.
plugins {
    java
    id("org.springframework.boot") version "3.5.16"
    id("com.diffplug.spotless") version "7.2.1"
}

group = "com.poe2craft"
version = "0.1.0-SNAPSHOT"
java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }
repositories { mavenCentral() }

val integrationTest by sourceSets.creating
val codegen by sourceSets.creating
configurations[integrationTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
configurations[integrationTest.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())
integrationTest.compileClasspath += sourceSets.main.get().output
integrationTest.runtimeClasspath += sourceSets.main.get().output

// JDBC patch shared by runtime and the isolated jOOQ generation database.
val postgresJdbcVersion = "42.7.12"
val bootBom = "org.springframework.boot:spring-boot-dependencies:3.5.16"
dependencies {
    implementation(platform(bootBom))
    // Security patch floors beyond the Boot 3.5 BOM; keep related modules aligned.
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.21.7"))
    implementation(platform("io.netty:netty-bom:4.1.137.Final"))
    implementation(platform("org.apache.logging.log4j:log4j-bom:2.25.5"))
    constraints {
        for (module in listOf("core", "el", "websocket")) {
            implementation("org.apache.tomcat.embed:tomcat-embed-$module:10.1.59")
        }
        runtimeOnly("org.postgresql:postgresql:$postgresJdbcVersion")
    }
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.security:spring-security-crypto")
    implementation("org.springframework.session:spring-session-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.flywaydb:flyway-core")
    implementation("com.google.re2j:re2j:1.8")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.xerial:sqlite-jdbc:3.53.4.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    add(codegen.implementationConfigurationName, platform(bootBom))
    add(codegen.implementationConfigurationName, "org.jooq:jooq-codegen")
    add(codegen.implementationConfigurationName, "org.flywaydb:flyway-core")
    add(codegen.implementationConfigurationName, "org.flywaydb:flyway-database-postgresql")
    add(codegen.implementationConfigurationName, "org.postgresql:postgresql:$postgresJdbcVersion")
    add(codegen.implementationConfigurationName, "org.testcontainers:postgresql")
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }
val integrationTestTask = tasks.register<Test>("integrationTest") {
    description = "Runs required PostgreSQL/Redis and application integration checks (Docker required)."
    group = "verification"
    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
    shouldRunAfter(tasks.test)
}
tasks.check { dependsOn(integrationTestTask) }

tasks.register<JavaExec>("generateJooq") {
    group = "code generation"
    description = "Generate from Flyway migrations in a disposable PostgreSQL container only."
    classpath = codegen.runtimeClasspath
    mainClass = "com.poe2craft.codegen.GenerateJooq"
    args(layout.projectDirectory.dir("src/main/resources/db/migration").asFile.absolutePath,
         layout.buildDirectory.dir("generated-sources/jooq").get().asFile.absolutePath)
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(21) }
}
spotless { java { target("src/**/*.java"); googleJavaFormat("1.28.0") } }
tasks.bootJar { archiveFileName = "poe2craft.jar" }

// Copy the exact shared frontend asset; no reserialization or second maintained template copy.
val chaosDisplayResources by tasks.registering(Copy::class) {
    from(layout.projectDirectory.file("../frontend/src/shared/i18n/modifierTemplates.json"))
    into(layout.buildDirectory.dir("generated-resources/chaosrepeat/chaosrepeat"))
}
sourceSets.main { resources.srcDir(layout.buildDirectory.dir("generated-resources/chaosrepeat")) }
tasks.processResources { dependsOn(chaosDisplayResources) }

// Captured source checksums remain testable without shipping raw evidence in the server.
sourceSets.test { resources.srcDir(layout.projectDirectory.dir("../docs/legacy/raw-data")) }

// 실험 기록에서 전송한 보강 입력만 묶는다. 기대 실행열과 과거 응답은 배포하지 않는다.
val experimentSource = layout.projectDirectory.file("../docs/llm/context-ab-responses.md")
val experimentFixtures = layout.projectDirectory.file("../docs/llm/context-ab-cards.json")
val modifierDisplay = layout.projectDirectory.file("../frontend/src/shared/i18n/modifierTemplates.json")
val experimentResource = layout.buildDirectory.file("generated-resources/llmtest/llmtest/cards.json")
val extractExperimentCards by tasks.registering {
    inputs.files(experimentSource, experimentFixtures, modifierDisplay)
    outputs.file(experimentResource)
    doLast {
        val source = experimentSource.asFile.readText().replace("\r\n", "\n")
        val fence = "```"
        val prompt = source.substringAfter("${fence}text\n", "").substringBefore("\n$fence")
        require(prompt.isNotBlank()) { "Missing experiment prompt" }
        val cards = linkedMapOf<String, Any>()
        val json = groovy.json.JsonSlurper()
        val fixtures = json.parse(experimentFixtures.asFile) as Map<*, *>
        val display = json.parse(modifierDisplay.asFile) as Map<*, *>
        val definitions = display["definitions"] as Map<*, *>
        for (section in source.split("\n## 호출 ").drop(1)) {
            val heading = section.substringBefore('\n').split(" · ")
            if (heading.last() != "보강") continue
            val id = heading[1]
            val block = section.substringAfter("전송한 사용자 메시지와 응답 스키마.\n\n${fence}json\n", "")
                .substringBefore("\n$fence")
            val payload = json.parseText(block) as Map<*, *>
            require(payload["input"] is Map<*, *> && payload["schema"] is Map<*, *>)
            val fixture = (fixtures[id] as Map<*, *>)["fixture"] as Map<*, *>
            val labels = (fixture["mods"] as Map<*, *>).mapValues { (_, value) ->
                val modifier = value as Map<*, *>
                val binding = definitions[modifier["id"]] as Map<*, *>
                require(binding["englishText"] == modifier["text"] && binding["stats"] == modifier["stats"])
                binding["template"] as String
            }
            val card = mapOf("input" to payload["input"], "schema" to payload["schema"],
                "baseId" to fixture["baseId"], "templates" to labels)
            require(cards.put(id, card) == null) { "Duplicate experiment card $id" }
        }
        require(cards.isNotEmpty() && cards.keys == fixtures.keys) { "Enhanced experiment card set differs from fixtures" }
        val target = experimentResource.get().asFile
        target.parentFile.mkdirs()
        target.writeText(groovy.json.JsonOutput.toJson(mapOf("prompt" to prompt, "cards" to cards)))
    }
}
tasks.processResources {
    dependsOn(extractExperimentCards)
    from(experimentResource) { into("llmtest") }
}
