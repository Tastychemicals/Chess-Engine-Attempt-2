plugins {
    kotlin("jvm") version "2.0.10"
    application // This allows you to run your Kotlin app easily
    id("org.openjfx.javafxplugin") version "0.0.13" // JavaFX plugin
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib")) // Kotlin standard library
    implementation("org.openjfx:javafx-controls:23.0.2") // JavaFX controls
    implementation("org.openjfx:javafx-fxml:23.0.2") // JavaFX FXML
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.apache.xmlgraphics:batik-transcoder:1.17") // for rendering svgs
    implementation("org.apache.xmlgraphics:batik-codec:1.17")
    implementation("org.openjfx:javafx-swing:23.0.2")
}


tasks.test {
    useJUnitPlatform()
}

javafx {
    version = "23.0.2" // JavaFX version
    modules = listOf("javafx.controls", "javafx.fxml","javafx.swing" ) // JavaFX modules you need
}
application {
    mainClass.set("GUIKt") // Update with your actual main class
}