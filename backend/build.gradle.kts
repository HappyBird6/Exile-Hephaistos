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

val bootBom = "org.springframework.boot:spring-boot-dependencies:3.5.16"
dependencies {
    implementation(platform(bootBom))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    add(codegen.implementationConfigurationName, platform(bootBom))
    add(codegen.implementationConfigurationName, "org.jooq:jooq-codegen")
    add(codegen.implementationConfigurationName, "org.flywaydb:flyway-core")
    add(codegen.implementationConfigurationName, "org.flywaydb:flyway-database-postgresql")
    add(codegen.implementationConfigurationName, "org.postgresql:postgresql")
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
