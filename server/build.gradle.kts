plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
}

group = "ch.lauzhand.helpinghand"
version = "1.0.0"
application {
    mainClass = "ch.lauzhand.helpinghand.ApplicationKt"
}

dependencies {
    api(project(":core"))
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serializationKotlinxJson)
    implementation(libs.ktor.serverCors)
    implementation(libs.ktor.serverStatusPages)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.ktor.clientContentNegotiation)
    testImplementation(libs.kotlin.testJunit)
}